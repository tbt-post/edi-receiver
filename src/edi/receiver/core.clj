(ns edi.receiver.core
  (:gen-class)
  (:require [clojure.tools.logging :as log]
            [edi.receiver.api.core :as api]
            [edi.receiver.backend.core :as backend]
            [edi.common.db.jdbc :as db]
            [edi.receiver.saver :as saver]
            [edi.receiver.upstream :as upstream]
            [edi.common.app :as app]))


(defn- run-app! [options config]
  (log/debug "Options:" options)
  (let [db      (db/connect config)
        backend (backend/create config)
        context {:config   config
                 :upstream (upstream/create (:upstream config))
                 :db       db
                 :backend  backend}]
    (if (saver/run-tests context)
      (let [server (api/start (:api config) context)]
        (-> (Runtime/getRuntime)
            (.addShutdownHook (Thread. #(do (api/stop server)
                                            (db/close db))))))
      (do (log/error "TESTS FAILED")
          (db/close db)
          (backend/close backend)
          (System/exit 1)))))


(defn -main [& args]
  (app/start! args
              [[nil "--sync" "update schemas and tests"]]
              run-app!))
