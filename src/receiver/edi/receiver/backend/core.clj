(ns edi.receiver.backend.core
  (:require [clojure.tools.logging :as log]
            [edi.receiver.backend.kafka :as kafka]
            [edi.receiver.backend.http :as http]
            [edi.receiver.backend.protocol :as protocol]
            [edi.common.utils :as utils]))


(defn- create-backend [{:keys [name type] :as config}]
  (let [config (dissoc config :name :type :enabled)]
    [(keyword name)
     (case type
       "http" (http/create config)
       "kafka" (kafka/create config)
       (throw (ex-info (format "Unknown backend: %s" type) config)))]))


(defn- create-backends [config]
  (->> config
       utils/ordered-vals
       (filter :enabled)
       (map create-backend)
       (into {})))


(defn- wrap-unreliable [send]
  (fn [backend topic message]
    (try
      (send backend topic message)
      (catch Exception e
        (log/warnf "cant send to unreliable %s: %s: %s\ndata:\n%s\nmessage:\n%s"
                   topic
                   (-> e class .getName)
                   (ex-message e)
                   (utils/pretty (ex-data e))
                   (utils/pretty message))))))


(defn- create-proxies [config backends]
  (let [create-proxy (fn [{:keys [backend reliable source target]}]
                       (let [send (cond-> protocol/send-message
                                          (not reliable) wrap-unreliable)]
                         {:source source
                          :send   (partial send (get backends (keyword backend)) target)}))]
    (->> config
         utils/ordered-vals
         (filter :enabled)
         (filter #(get backends (keyword (:backend %))))
         (map create-proxy)
         (group-by :source)
         (map (fn [[source proxies]] [(keyword source)
                                      (mapv :send proxies)]))
         (into {}))))


(defn create [{:keys [backend proxy]}]
  (let [backends (create-backends backend)]
    {:backends backends
     :proxies  (create-proxies proxy backends)}))


(defn close [{:keys [backends]}]
  (->> backends
       vals
       (run! protocol/close)))


(defn send-message [{:keys [proxies]} topic message]
  (doseq [proxy (get proxies topic)]
    (let [result (proxy message)]
      (log/debug "proxying result" (utils/pretty result)))))