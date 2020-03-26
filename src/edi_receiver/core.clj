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
            [edi-receiver.saver :as saver]))


(def cli-options
  [;; Options
   ["-h" "--help" "show help"]
   ["-c" "--config CONFIG" "External config file"
    :parse-fn #(str %)
    :validate [#(-> % string/trim io/file .exists) "Config file does not exist"]]
   ;; Flags
   [nil "--dump-config" "dump system configuration"]
   [nil "--init-db" "initialize database"]])


(defn- run-app! [options]
  (log/debug "Options:" options)
  (let [config  (config/create options)
        pg      (pg/connect (:pg config))
        context {:config   config
                 :upstream (upstream/create (:upstream config))
                 :pg       pg}]
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
      (deploy/deploy (config/create options))

      :else
      (run-app! options))))
