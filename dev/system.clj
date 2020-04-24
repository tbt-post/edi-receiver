(ns system
  (:require
    [com.stuartsierra.component :as component]
    [edi-receiver.api.core :as api]
    [edi-receiver.config :as config]
    [edi-receiver.db.jdbc :as db]
    [edi-receiver.upstream :as upstream]
    [edi-receiver.saver :as saver]
    [edi-receiver.deploy :as deploy]))


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
    (assoc this :db (db/connect (-> config :config))))

  (stop [this]
    (db/close (-> this :db))
    (assoc this :db nil)))


(defrecord Upstream [config upstream]
  component/Lifecycle

  (start [this]
    (assoc this :upstream (upstream/create (-> config :config :upstream))))

  (stop [this]
    (assoc this :upstream nil)))


(defrecord Server [config upstream db server]
  component/Lifecycle

  (start [this]
    (let [context {:config   (:config config)
                   :upstream (:upstream upstream)
                   :db       (:db db)}]
      (deploy/deploy! context)
      (saver/run-tests! context)
      (assoc this :server (api/start (-> config :config :api)
                                     context))))

  (stop [this]
    (api/stop (-> this :server))
    (assoc this :server nil)))


(defn edi-receiver-system [options]
  (component/system-map
    :config (map->Config {:options options})
    :db (-> (map->Db {})
            (component/using [:config]))
    :upstream (-> (map->Upstream {})
                  (component/using [:config]))
    :server (-> (map->Server {})
                (component/using [:config :db :upstream]))))
