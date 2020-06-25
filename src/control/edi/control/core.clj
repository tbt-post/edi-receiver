(ns edi.control.core
  (:gen-class)
  (:require [clojure.tools.logging :as log]
            [edi.common.app :as app]
            [edi.common.db.jdbc :as db]
            [edi.control.deploy :as deploy]
            [edi.control.fire :as fire]
            [clojure.java.io :as io]
            [clojure.string :as string]))


(defn- context [config]
  {:config config
   :db     (db/connect config)})


(defn- run-app! [options config]
  (log/debug "Options:" options)
  (cond
    (:print-deploy-sql options)
    (deploy/print-deploy-sql (context config))

    (:deploy options)
    (deploy/deploy! (context config))

    (:fire options)
    (fire/fire! options)))


(defn -main [& args]
  (app/start! args
              [[nil "--deploy" "initialize or migrate database"]
               [nil "--fire FIRE-CONFIG" "generate lot of http requests"
                :parse-fn string/trim
                :validate [#(-> % io/file .exists) "Fire config file does not exist"]]
               [nil "--print-deploy-sql" "just print sql for deploy"]]
              run-app!))
