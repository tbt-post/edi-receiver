(ns edi-receiver.utils
  (:require [clojure.string :as string])
  (:import (clojure.lang Keyword)
           (java.io StringWriter)
           (java.nio ByteBuffer)
           (java.time Instant)
           (java.time.format DateTimeFormatter)
           (java.util UUID Date Base64)
           (java.util.concurrent TimeUnit)
           (org.eclipse.jetty.client HttpClient)
           (org.eclipse.jetty.client.api Request ContentResponse)
           (org.eclipse.jetty.client.util StringContentProvider)
           (org.eclipse.jetty.util.ssl SslContextFactory$Client)))


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


(defn iso-datetime->java-util-date [^String v]
  (->> v
       (.parse DateTimeFormatter/ISO_INSTANT)
       Instant/from
       Date/from))


(defn merge-common [d keyword]
  (let [c (get d keyword)]
    (->> (dissoc d keyword)
         (map (fn [[k d]] [k (merge c d)]))
         (into {}))))


(defn base64-encode [s]
  (.encodeToString (Base64/getEncoder) (.getBytes s)))


(defn ordered-vals [d]
  (->> d
       keys
       sort
       (map #(get d %))))


;--------------------
; JETTY HTTP CLIENT
;--------------------

(defn ^HttpClient http-client []
  (let [^HttpClient client (HttpClient. (SslContextFactory$Client.))]
    (.start client)
    client))


(defn- set-query-params [^Request request params]
  (doseq [[^String k ^String v] params]
    (.param request k v))
  request)


(defn- set-headers [^Request request headers]
  (doseq [[^String k ^String v] headers]
    (.header request k v))
  request)


(defn- set-auth [headers {:keys [type username password]}]
  (case type
    "basic" (assoc headers "Authorization" (str "Basic " (base64-encode (str username ":" password))))
    identity))


(defn http-request [^HttpClient client {:keys [^String uri
                                               ^Keyword method
                                               query-params
                                               headers
                                               auth
                                               ^String body
                                               ^long timeout
                                               ^boolean throw-for-status]}]
  (let [^ContentResponse
        response (-> (cond-> ^Request (.newRequest client uri)
                             method (.method (name method))
                             timeout (.timeout timeout TimeUnit/MILLISECONDS)
                             query-params (set-query-params query-params)
                             headers (set-headers (cond-> headers
                                                          (:enabled auth) (set-auth auth)))
                             body (.content (StringContentProvider. body)))
                     .send)
        status   (-> response .getStatus)
        result   {:status status
                  #_:headers #_(->> response .getHeaders
                                    (map (fn [^HttpField h] [(.getName h) (.getValue h)]))
                                    (into {}))
                  :body   (-> response .getContentAsString)
                  :reason (-> response .getReason)}]
    (if (and throw-for-status (>= status 300))
      (throw (ex-info (format "Http status %s" status) result))
      result)))
