(ns edi-receiver.upstream
  (:require [cheshire.core :as json]
            [clojure.tools.logging :as log]
            [json-schema.core :as json-schema]
            [clojure.java.io :as io]
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


(defn- split-name [path]
  (second (re-find #"/([^/]+)\.json$" path)))


(defn- load-files-github [list-url]
  (let [^HttpClient client (HttpClient. (SslContextFactory$Client.))]
    (.start client)
    (->> (-> list-url
             log-download
             (get-body client)
             json/parse-string)
         (map #(get % "download_url"))
         (map #(identity {:name (-> % split-name)
                          :body (-> %
                                    log-download
                                    (get-body client))})))))


(defn- load-files-local [dir]
  (->> (-> dir io/file file-seq next)
       (map #(identity {:filename (-> % .getName)
                        :name     (-> % .getPath split-name)
                        :body     (-> % slurp)}))))


(defn- load-files [url]
  (if (string/starts-with? url "/")
    (load-files-local url)
    (load-files-github url)))

(defn create [{:keys [schema-list test-list]}]
  (log/debug "Creating Upstream")
  {:schemas (->> (load-files schema-list)
                 (map (fn [{:keys [name body]}]
                        [(keyword name) (json-schema/prepare-schema body)]))
                 (into {}))
   :tests   (->> (load-files test-list)
                 (map (fn [{:keys [name body]}]
                        {:name    name
                         :topic   (keyword (first (string/split name, #"\." 2)))
                         :message (json/parse-string body true)})))})


(defn validate [this topic value]
  (if-let [schema (topic (:schemas this))]
    (try
      (json-schema/validate schema value)
      nil
      (catch Exception e
        {:message (.getMessage e)
         :data    (ex-data e)}))
    (throw (ex-info (str "Schema not found: " (name topic)) {:topic topic}))))


(defn run-tests! [this executor]
  (->> (for [{:keys [topic message name]} (:tests this)]
         (try
           (executor topic message)
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
