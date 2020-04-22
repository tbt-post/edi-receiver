(ns edi-receiver.utils
  (:require [clojure.string :as string])
  (:import (java.io StringWriter)
           (java.nio ByteBuffer)
           (java.time Instant)
           (java.time.format DateTimeFormatter)
           (java.util UUID Date)))


(defn map-vals [f m]
  (into {} (for [[k v] m] [k (f v)])))


(defn map-keys [f m]
  (into {} (for [[k v] m] [(f k) v])))


(defn split-comma-separated [s]
  (->> (string/split s #",")
       (map string/trim)))


(defn pretty [& args]
  (let [out (StringWriter.)]
    (doseq [arg args]
      (clojure.pprint/pprint arg out))
    (.toString out)))


(defn- kebab-to-camelcase [k]
  (let [parts (-> k
                  name
                  (string/split #"\-"))]
    (-> (first parts)
        (cons (->> parts
                   next
                   (map string/capitalize)))
        string/join
        keyword)))


(defn kebab-conf-to-camelcase [conf]
  (map-keys kebab-to-camelcase conf))


(defn uuid->byte-array [^UUID v]
  (let [buffer (ByteBuffer/wrap (byte-array 16))]
    (doto buffer
      (.putLong (.getMostSignificantBits v))
      (.putLong (.getLeastSignificantBits v)))
    (.array buffer)))


(defn iso-datetime->java-util-date [^String v]
  (->> v
       (.parse DateTimeFormatter/ISO_INSTANT)
       Instant/from
       Date/from))
