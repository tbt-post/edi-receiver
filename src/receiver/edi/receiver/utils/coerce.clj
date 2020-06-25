(ns edi.receiver.utils.coerce
  (:require [clojure.edn :as edn]))


; coercion between types from json


(defn- ^Number parse-number [^String s]
  (if (re-find #"^-?\d+\.?\d*$" s)
    (edn/read-string s)
    (throw (ex-info (str "Can't parse number: " s) {:value s}))))


(defn- ^BigInteger parse-bigint [^String s]
  (if (re-find #"^-?\d+$" s)
    (BigInteger. s)
    (throw (ex-info (str "Can't parse bigint: " s) {:value s}))))


(defn as-str [v]
  (when (some? v)
    (str v)))


(defn as-int [v]
  (when (some? v)
    (cond
      (number? v)
      (.intValue v)

      (string? v)
      (-> (parse-number v)
          .intValue))))


(defn as-long [v]
  (when (some? v)
    (cond
      (number? v)
      (.longValue v)

      (string? v)
      (-> (parse-number v)
          .longValue))))


(defn as-bigint [v]
  (when (some? v)
    (cond
      (instance? BigInteger v)
      v

      (number? v)
      (BigInteger/valueOf (.longValue v))

      (string? v)
      (parse-bigint v))))


(defn as-double [v]
  (when (some? v)
    (cond
      (instance? BigInteger v)
      (.doubleValue v)

      (number? v)
      (.doubleValue v)

      (string? v)
      (-> (parse-number v)
          .doubleValue))))
