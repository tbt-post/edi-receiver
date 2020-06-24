(ns edi.receiver.utils.sandbox-overrides
  (:refer-clojure :exclude [and or]))


(defn and [& args]
  (every? identity args))


(defn or [& args]
  (or (some identity args) false))
