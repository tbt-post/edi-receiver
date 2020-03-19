(ns edi-receiver.upstream
  (:require [cheshire.core :as json]
            [clojure.tools.logging :as log]
            [json-schema.core :as json-schema])
  (:import [org.eclipse.jetty.util.ssl SslContextFactory$Client]
           [org.eclipse.jetty.client HttpClient]))


(defn- log-download [url]
  (log/info "Downloading" url)
  url)


(defn- get-body [^String url ^HttpClient client]
  (-> client
      (.GET url)
      (.getContentAsString)))


(defn- make-item [^HttpClient client ^String url]
  [(->> url
        (re-find #"/([^/]+)\.json$")
        second
        keyword)
   (-> url
       log-download
       (get-body client)
       json-schema/prepare-schema)])


(defn create [config]
  (log/debug "Creating Upstream")
  (let [^HttpClient client (HttpClient. (SslContextFactory$Client.))]
    (.start client)
    (->> (-> config
             :list-url
             log-download
             (get-body client)
             json/parse-string)
         (map #(get % "download_url"))
         ;(take 1)
         (map #(make-item client %))
         (into {}))))


(defn validate [this schema value]
  (try
    {:result (json-schema/validate (schema this) value)}
    (catch Exception e
      {:error {:class   (class e)
               :message (.getMessage e)
               :data    (ex-data e)}})))
