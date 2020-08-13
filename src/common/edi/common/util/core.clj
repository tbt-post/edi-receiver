(ns edi.common.util.core
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
  (string/trimr
    (let [out (StringWriter.)]
      (doseq [arg args]
        (clojure.pprint/pprint arg out))
      (.toString out))))


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


(defn parse-java-util-date [^String v]
  (->> (cond-> v
               (= \space (nth v 10)) (#(str (subs % 0 10) "T" (subs % 11)))
               (re-matches #".*[+-]\d\d$" (subs v 19)) (str ":00"))
       (.parse DateTimeFormatter/ISO_OFFSET_DATE_TIME)
       Instant/from
       Date/from))


(defn merge-common [d keyword]
  (let [c (get d keyword)]
    (->> (dissoc d keyword)
         (map (fn [[k d]] [k (merge c d)]))
         (into {}))))


(defn ordered-configs [d]
  (->> d
       keys
       sort
       (map #(-> (get d %)
                 (assoc :key %)))))


