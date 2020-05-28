(ns edi.control.core
  (:gen-class)
  (:require [clojure.tools.logging :as log]
            [edi.common.app :as app]
            [edi.common.db.jdbc :as db]
            [edi.control.deploy :as deploy]))


(defn- run-app! [options config]
  (log/debug "Options:" options)
  (let [context {:config config
                 :db     (db/connect config)}]
    (cond
      (:print-deploy-sql options)
      (deploy/print-deploy-sql context)

      (:deploy options)
      (deploy/deploy! context))))


(defn -main [& args]
  (app/start! args
              [[nil "--deploy" "initialize or migrate database"]
              [nil "--print-deploy-sql" "just print sql for deploy"]]
              run-app!))
