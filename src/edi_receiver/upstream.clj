(ns edi-receiver.upstream
  (:require
    [cheshire.core :as json]
    [clj-http.client :as http]
    [clojure.pprint :refer [pprint]]
    [json-schema.core :as json-schema]
    [mount.core :as mount]
    [edi-receiver.config :as config]
    [clojure.tools.logging :as log]))


(defn- log-download [url]
  (log/info "Downloading" url)
  url)


(defn- make-item [url]
  [(->> url
        (re-find #"/([^/]+)\.json$")
        second
        keyword)
   (-> url
       log-download
       http/get
       :body
       json-schema/prepare-schema)])


(defn- create [_]
  (->> (-> (config/prop :upstream.list)
           log-download
           http/get
           :body
           json/parse-string)
       (map #(get % "download_url"))
       ;(take 1)
       (map make-item)
       (into {})))


(mount/defstate schemas
  :start (create (mount/args)))


(defn validate [schema value]
  (try
    {:result (json-schema/validate (schema schemas) value)}
    (catch Exception e
      {:error {:class   (class e)
               :message (.getMessage e)
               :data    (ex-data e)}})))
