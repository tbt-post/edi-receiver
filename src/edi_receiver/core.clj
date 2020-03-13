(ns edi-receiver.core
  (:gen-class)
  (:require
    [clojure.java.io :as io]
    [clojure.string :as string]
    [clojure.tools.cli :as cli]
    [clojure.tools.namespace.repl :as tn]
    [hawk.core :as hawk]
    [mount.core :as mount]
    [clojure.tools.logging :as log]
    [edi-receiver.config]
    [clojure.pprint :refer [pprint]]
    [edi-receiver.config :as config]
    [edi-receiver.upstream]
    [edi-receiver.api.core]))


(def cli-options
  [;; Options
   ["-h" "--help" "show help"]
   ["-c" "--config CONFIG" "External config file"
    :parse-fn #(str %)
    :validate [#(-> % string/trim io/file .exists) "Config file does not exist"]]
   ;; Flags
   [nil "--dump-config" "dump system configuration"]])


(defn start-autoreload! []
  (tn/set-refresh-dirs "src")
  (let [-last-reload (volatile! 0)]
    (hawk/watch!
      [{:paths   ["src"]
        :filter  (fn [_ {:keys [file]}]
                   (and (.isFile file)
                        (re-find #".cljc?$" (.getName file))))
        :handler (fn [_ {:keys [file]}]
                   (when (> (System/currentTimeMillis) (+ @-last-reload 1000))
                     (vreset! -last-reload (System/currentTimeMillis))
                     ;; this binding prevents clojure.tools.namespace.repl blowing
                     ;; up with "Can't set!: *ns* from non-binding thread"
                     (binding [*ns* *ns*]
                       (let [res (tn/refresh)]
                         (cond
                           (= res :ok) :pass
                           (instance? Exception res)
                           (loop [ex res]
                             (when ex
                               (println "  " (.getMessage ex))
                               (recur (.getCause ex))))
                           :else (prn res))))))}])))


(defn- run-app! [options]
  (when (-> ".properties" io/resource .getProtocol (not= "jar"))
    (log/debug "Entering developer mode...")
    (start-autoreload!))

  (log/debug "Options:" options)
  (mount/start-with-args options)

  #_(log/info "API:" api/api))


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

      :else
      (run-app! options))))
