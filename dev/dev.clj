(ns dev
  "Tools for interactive development with the REPL. This file should
  not be included in a production build of the application.

  Call `(reset)` to reload modified code and (re)start the system.

  The system under development is `system`, referred from
  `com.stuartsierra.component.repl/system`.

  See also https://github.com/stuartsierra/component.repl"
  (:require
    [com.stuartsierra.component.repl :refer [reset set-init start stop system]]
    [clojure.tools.namespace.repl :as tn]
    [hawk.core :as hawk]
    [system :as system]))


(set-init (fn [_] (system/edi-receiver-system {})))


;; Do not try to load source code from 'resources' directory
(tn/set-refresh-dirs "src" #_"test" #_"dev")


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


(prn ">> loaded dev")
