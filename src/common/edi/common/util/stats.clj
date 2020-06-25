(ns edi.common.util.stats
  (:require [clojure.string :as string])
  (:import (java.time Clock Duration)
           (edi.common.util NanoClock)))


(def ^:private ^Clock clock (if (string/starts-with? (System/getProperty "java.version") "1.8")
                              (NanoClock.)
                              (Clock/systemUTC)))


(defn now []
  (.instant clock))


(defn micro-seconds [^Duration duration]
  (-> duration .toNanos (/ 1000)))
