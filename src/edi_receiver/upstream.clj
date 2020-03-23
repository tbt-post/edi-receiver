(ns edi-receiver.upstream
  (:require [cheshire.core :as json]
            [clojure.tools.logging :as log]
            [json-schema.core :as json-schema]
            [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]]
            [clojure.string :as string])
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


(defn create-schemas [config]
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


(defn- create-test [filename]
  {:topic    (keyword (first (string/split filename, #"\." 2)))
   :filename filename
   :message  (-> (str "tests/" filename)
                 io/resource
                 slurp
                 (json/parse-string true))})


(defn- create-tests []
  (->> (-> "tests/index.txt"
           io/resource
           slurp
           string/split-lines)
       (map string/trim)
       (remove #(string/starts-with? % "#"))
       (map create-test)))


(defn create [config]
  (log/debug "Creating Upstream")
  {:schemas (create-schemas config)
   :tests   (create-tests)})


(defn validate [this topic value]
  (if-let [schema (topic (:schemas this))]
    (try
      (json-schema/validate schema value)
      nil
      (catch Exception e
        {:message (.getMessage e)
         :data    (ex-data e)}))
    (log/warn "Schema not found for:" topic)))


(defn run-tests! [this executor]
  (->> (for [{:keys [topic message filename]} (:tests this)]
         (try
           (executor topic message)
           (log/info (format "PASSED %s" filename))
           true
           (catch Exception e
             (log/error (format "FAILED %s: %s %s %s"
                                filename
                                (class e)
                                (ex-message e)
                                (ex-data e)))
             false)))
       (doall)
       (every? identity)))
