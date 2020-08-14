(ns system
  (:require [com.stuartsierra.component :as component]
            [edi.common.config :as config]
            [edi.common.db.jdbc :as db]
            [edi.control.deploy :as deploy]
            [edi.receiver.api.core :as api]
            [edi.receiver.backend.core :as backend]
            [edi.receiver.buffers :as buffers]
            [edi.receiver.saver :as saver]
            [edi.receiver.stats :as stats]
            [edi.receiver.upstream :as upstream]))


(defrecord Config [options config]
  component/Lifecycle

  (start [this]
    (assoc this :config (config/create options)))

  (stop [this]
    (assoc this :config nil)
    this))


(defrecord Db [config db]
  component/Lifecycle

  (start [this]
    (assoc this :db (db/connect (:config config))))

  (stop [this]
    (db/close (-> this :db))
    (assoc this :db nil)))


(defrecord Buffers [config db buffers]
  component/Lifecycle

  (start [this]
    (assoc this :buffers (buffers/create {:config (:config config)
                                          :db     (:db db)})))

  (stop [this]
    (buffers/stop (-> this :buffers))
    (assoc this :buffers nil)))


(defrecord Backend [config buffers backend]
  component/Lifecycle

  (start [this]
    (assoc this :backend (backend/create {:config  (:config config)
                                          :buffers (:buffers buffers)})))

  (stop [this]
    (backend/close (-> this :backend))
    (assoc this :backend nil)))


(defrecord Upstream [config upstream]
  component/Lifecycle

  (start [this]
    (assoc this :upstream (upstream/create (-> config :config :upstream))))

  (stop [this]
    (assoc this :upstream nil)))


(defrecord Stats [config db stats]
  component/Lifecycle

  (start [this]
    (assoc this :stats (stats/create {:config (:config config)
                                      :db     (:db db)})))

  (stop [this]
    (assoc this :stats nil)))


(defrecord Server [config upstream db buffers backend stats server]
  component/Lifecycle

  (start [this]
    (let [context {:config   (:config config)
                   :upstream (:upstream upstream)
                   :db       (:db db)
                   :backend  (:backend backend)
                   :stats    (:stats stats)}]
      (deploy/deploy! context)
      (saver/run-tests context)
      (buffers/start (:buffers buffers))
      (assoc this :server (api/start (-> config :config :api)
                                     context))))

  (stop [this]
    (api/stop (-> this :server))
    (assoc this :server nil)))


(defn edi-system [options]
  (component/system-map
    :config (map->Config {:options options})
    :db (-> (map->Db {})
            (component/using [:config]))
    :buffers (-> (map->Buffers {})
                 (component/using [:config :db]))
    :backend (-> (map->Backend {})
                 (component/using [:config :buffers]))
    :upstream (-> (map->Upstream {})
                  (component/using [:config]))
    :stats (-> (map->Stats {})
               (component/using [:config :db]))
    :server (-> (map->Server {})
                (component/using [:config :db :buffers :backend :upstream :stats]))))
