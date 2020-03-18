(ns edi-receiver.core
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]]
            [clojure.string :as string]
            [clojure.tools.cli :as cli]
            [clojure.tools.logging :as log]
            [edi-receiver.config :refer [create-config]]
            [edi-receiver.upstream :refer [create-upstream]]
            [edi-receiver.api.core :as api]))


(def cli-options
  [;; Options
   ["-h" "--help" "show help"]
   ["-c" "--config CONFIG" "External config file"
    :parse-fn #(str %)
    :validate [#(-> % string/trim io/file .exists) "Config file does not exist"]]
   ;; Flags
   [nil "--dump-config" "dump system configuration"]])


(defn- run-app! [options]
  (log/debug "Options:" options)
  (let [{:keys [api-host api-port upstream-list-url] :as config} (create-config options)
        server (api/start-server api-host api-port
                                 {:config   config
                                  :upstream (create-upstream upstream-list-url)
                                  :db       nil})]
    (-> (Runtime/getRuntime)
        (.addShutdownHook (Thread. #(api/stop-server server))))))


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
      (pprint (create-config options))

      :else
      (run-app! options))))
