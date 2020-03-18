(ns edi-receiver.upstream
  (:require [cheshire.core :as json]
            [clj-http.client :as http]
            [clojure.pprint :refer [pprint]]
            [clojure.tools.logging :as log]
            [json-schema.core :as json-schema]))


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


(defn create-upstream [{:keys [upstream-list-url]}]
  (log/debug "Creating Upstream")
  (->> (-> upstream-list-url
           log-download
           http/get
           :body
           json/parse-string)
       (map #(get % "download_url"))
       (take 1)
       (map make-item)
       (into {})))


(defn upstream-validate [this schema value]
  (try
    {:result (json-schema/validate (schema this) value)}
    (catch Exception e
      {:error {:class   (class e)
               :message (.getMessage e)
               :data    (ex-data e)}})))
