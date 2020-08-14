(ns edi.common.util.core
  (:require [clojure.string :as string])
  (:import (java.io StringWriter)
           (java.nio ByteBuffer)
           (java.time Instant)
           (java.time.format DateTimeFormatter DateTimeParseException)
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


(def ^:private datetime-formatters (map #(DateTimeFormatter/ofPattern %)
                                        ["yyyy-MM-dd'T'HH:mm:ss[.SSS]X"
                                         "yyyy-MM-dd HH:mm:ss[.SSS]X"
                                         "yyyy-MM-dd'T'HH:mm:ss.SSSSSSX"
                                         "yyyy-MM-dd HH:mm:ss.SSSSSSX"]))


(defn parse-java-util-date [^String s]
  (loop [formatters datetime-formatters]
    (or
      (try
        (->> s (.parse (first formatters)) Instant/from Date/from)
        (catch DateTimeParseException e
          (when-not (second formatters)
            (throw e))))
      (recur (next formatters)))))


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


