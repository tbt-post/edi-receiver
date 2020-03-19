(ns system
  (:require
    [com.stuartsierra.component :as component]
    [edi-receiver.db.pg :as pg]
    [edi-receiver.upstream :as upstream]
    [edi-receiver.api.core :as api]
    [edi-receiver.config :as config]))


(defrecord Config [options config]
  component/Lifecycle

  (start [this]
    (println "\tStarting Config ...")
    (assoc this :config (config/create options)))

  (stop [this]
    (println "\tStopping Config ...")
    (assoc this :config nil)
    this))


(defrecord Pg [config pg]
  component/Lifecycle

  (start [this]
    (assoc this :pg (pg/connect (-> config :config :pg))))

  (stop [this]
    (pg/close (-> this :pg))
    (assoc this :pg nil)))


(defrecord Upstream [config upstream]
  component/Lifecycle

  (start [this]
    (assoc this :upstream (upstream/create (-> config :config :upstream))))

  (stop [this]
    (assoc this :upstream nil)))


(defrecord Server [config upstream pg server]
  component/Lifecycle

  (start [this]
    (assoc this :server (api/start (-> config :config :api)
                                   {:config   (:config config)
                                    :upstream (:upstream upstream)
                                    :pg       (:pg pg)})))

  (stop [this]
    (api/stop (-> this :server))
    (assoc this :server nil)))


(defn edi-receiver-system [options]
  (component/system-map
    :config (map->Config {:options options})
    :pg (-> (map->Pg {})
            (component/using [:config]))
    :upstream (-> (map->Upstream {})
                  (component/using [:config]))
    :server (-> (map->Server {})
                (component/using [:config :pg :upstream]))))
