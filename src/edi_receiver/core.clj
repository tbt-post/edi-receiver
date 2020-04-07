(ns edi-receiver.core
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]]
            [clojure.string :as string]
            [clojure.tools.cli :as cli]
            [clojure.tools.logging :as log]
            [edi-receiver.config :as config]
            [edi-receiver.upstream :as upstream]
            [edi-receiver.db.pg :as pg]
            [edi-receiver.api.core :as api]
            [edi-receiver.deploy :as deploy]
            [edi-receiver.saver :as saver]
            [edi-receiver.utils :as utils]))


(def ^:private cli-options
  [;; Options
   ["-h" "--help" "show help"]
   ["-c" "--config CONFIG" "External config file"
    :parse-fn #(string/trim %)
    :validate [#(-> % io/file .exists) "Config file does not exist"]]
   ["-t" "--topics TOPICS" "Comma separated topic list"
    :parse-fn utils/split-comma-separated
    :validate [(fn [topics]
                 (every?
                   #(if (io/resource (format "sql/tables/%s.sql" %))
                      true
                      (do (log/error "Unknown topic:" %)
                          false))
                   topics)) "Unknown topic"]]
   ;; Flags
   [nil "--sync" "update schemas and tests"]
   [nil "--autoinit-tables" "initialize database before start"]
   [nil "--dump-config" "dump system configuration"]])


(defn- run-app! [options]
  (log/debug "Options:" options)
  (let [config  (config/create options)
        pg      (pg/connect (:pg config))
        context {:config   config
                 :upstream (upstream/create (:upstream config))
                 :pg       pg}]
    (if (:autoinit-tables config)
      (deploy/deploy! context))
    (if (saver/run-tests! context)
      (let [server (api/start (:api config) context)]
        (-> (Runtime/getRuntime)
            (.addShutdownHook (Thread. #(do (api/stop server)
                                            (pg/close pg))))))
      (do (log/error "TESTS FAILED")
          (pg/close pg)
          (System/exit 1)))))


(defn -main [& args]
  (let [{:keys [options _ summary errors]} (cli/parse-opts args cli-options)]
    (cond
      errors
      (do
        (println "Errors:" (string/join "\n\t" errors))
        1)

      (:help options)
      (println summary)

      (:dump-config options)
      (pprint (config/create options))

      (:init-db options)
      (deploy/deploy! (config/create options))

      :else
      (run-app! options))))
