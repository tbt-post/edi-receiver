(ns edi-receiver.upstream
  (:require [cheshire.core :as json]
            [clojure.tools.logging :as log]
            [json-schema.core :as json-schema]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [edi-receiver.utils :as utils])
  (:import (java.io File)))


(defn- log-download [url]
  (log/info "Downloading" url)
  url)


(defn- split-topic [path]
  (second (re-find #"/([^/\.]+)[^/]+$" path)))

(defn- split-name [path]
  (second (re-find #"/([^/]+)\.json$" path)))

(defn- http-get [url client]
  (:body (utils/http-request client {:uri url :throw-for-status true})))

(defn- load-files-github [list-url]
  (let [client (utils/http-client)]
    (->> (-> list-url
             log-download
             (http-get client)
             json/parse-string)
         (map #(get % "download_url"))
         (map (fn [url] [url (-> url
                                 log-download
                                 (http-get client))])))))


(defn- load-files-local [dir]
  (->> (-> dir io/file file-seq next)
       (map (fn [file] [(.getPath file)
                        (slurp file)]))))


(defn- load-files [url]
  (if (string/starts-with? url "/")
    (load-files-local url)
    (load-files-github url)))


(defn- download [{:keys [schema-list test-list]}]
  (log/debug "Downloading schemas")
  {:schemas (->> (load-files schema-list)
                 (map (fn [[url body]]
                        [(keyword (split-topic url))
                         body]))
                 (into {}))
   :tests   (->> (load-files test-list)
                 (map (fn [[url body]]
                        {:name    (split-name url)
                         :topic   (split-topic url)
                         :message (json/parse-string body true)})))})


(defn- cache-file [cache-dir]
  (str cache-dir "/upstream.json"))


(defn- download-and-cache [{:keys [cache-dir] :as upstream}]
  (let [data (download upstream)]
    (when cache-dir
      (let [cache (cache-file cache-dir)]
        (io/make-parents cache)
        (-> cache io/file (spit (json/generate-string data)))))
    data))


(defn- slurp-file [^File file]
  (when (.exists file)
    (slurp file)))


(defn create [{:keys [topics cache-dir sync] :as upstream}]
  (log/info "Creating Upstream")
  (let [topics (set topics)]
    (-> (if sync
          (download-and-cache upstream)
          (if-let [data (some-> cache-dir
                                cache-file
                                io/file
                                slurp-file
                                (json/parse-string true))]
            data
            (download-and-cache upstream)))
        (update :schemas #(->> (select-keys % (map keyword topics))
                               (utils/map-vals json-schema/prepare-schema)))
        (update :tests #(filter (fn [test] (topics (:topic test))) %)))))


(defn validate [this topic value]
  (if-let [schema (topic (:schemas this))]
    (try
      (json-schema/validate schema value)
      nil
      (catch Exception e
        (throw (ex-info "Message not valid" {:message      (.getMessage e)
                                             :data         (ex-data e)
                                             :bad-request? true}))))
    (throw (ex-info (str "Schema not found: " (name topic)) {:topic        topic
                                                             :bad-request? true}))))


(defn run-tests [this executor]
  (log/info "Running tests")
  (->> (for [{:keys [topic message name]} (:tests this)]
         (try
           (executor (keyword topic) message)
           (log/infof "PASSED %s" name)
           true
           (catch Exception e
             (log/errorf "FAILED %s: %s %s"
                         name
                         (ex-message e)
                         (or (ex-data e) ""))
             false)))
       (doall)
       (every? identity)))
