(ns edi.common.util.timer
  (:require [clojure.string :as string])
  (:import (java.time Clock Duration Instant)
           (edi.common.util NanoClock)))


(def ^:private ^Clock clock (if (string/starts-with? (System/getProperty "java.version") "1.8")
                              (NanoClock.)
                              (Clock/systemUTC)))


(defn now []
  (.instant clock))


(defn delta-micros [^Instant start ^Instant stop]
  (-> (Duration/between start stop)
      .toNanos
      (/ 1000)))
