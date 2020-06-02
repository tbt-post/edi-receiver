(ns edi.common.app
  (:require [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]]
            [clojure.string :as string]
            [clojure.tools.cli :as cli]
            [clojure.tools.logging :as log]
            [edi.common.config :as config]
            [edi.common.utils :as utils]))


(def ^:private default-option-specs
  [["-h" "--help" "show help"]
   ["-c" "--config CONFIG" "External config file"
    :parse-fn string/trim
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
   ["-d" "--db DB" "Database, pg|mysql, default is pg"
    :parse-fn string/trim
    :validate [#(#{"pg" "mysql"} %) "Invalid db"]]
   [nil "--dump-config" "dump system configuration"]])


(defn start! [args option-specs run-app!]
  (let [{:keys [options _ summary errors]} (cli/parse-opts args (concat default-option-specs option-specs))]
    (cond
      errors
      (do
        (println "Errors:" (string/join "\n\t" errors))
        1)

      (:help options)
      (println summary)

      (:dump-config options)
      (pprint (config/create options))

      :else
      (run-app! options (config/create options)))))
