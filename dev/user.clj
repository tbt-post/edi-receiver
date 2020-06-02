(ns user
  (:require
    [com.stuartsierra.component.repl :refer [reset set-init start stop]]
    [clojure.tools.namespace.repl :as tn]
    [hawk.core :as hawk]
    [system :as system]))


(set-init (fn [_] (system/edi-system {:config "local.properties"})))


;; Do not try to load source code from 'resources' directory
(tn/set-refresh-dirs "src" "test" "dev")


(defn autoreload []
  (let [-last-reload (volatile! 0)]
    (hawk/watch!
      [{:paths   ["src"]
        :filter  (fn [_ {:keys [file]}]
                   (and (.isFile file)
                        (re-find #".cljc?$" (.getName file))
                        (not (.contains (.getPath file) "/src/admin/front/"))
                        (not (.contains (.getPath file) "/src/styles/"))))
        :handler (fn [_ env]
                   (when (> (System/currentTimeMillis) (+ @-last-reload 1000))
                     (vreset! -last-reload (System/currentTimeMillis))
                     (binding [*ns* *ns*]
                       (reset))))}])))


(defn go []
  (autoreload)
  (start))
