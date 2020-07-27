(ns edi.receiver.backend.core
  (:require [clojure.tools.logging :as log]
            [clojure.tools.reader.edn :as edn]
            [edi.receiver.backend.kafka :as kafka]
            [edi.receiver.backend.dump :as dump]
            [edi.receiver.backend.http :as http]
            [edi.receiver.backend.protocol :as protocol]
            [edi.receiver.backend.javamail :as javamail]
            [edi.receiver.utils.expression :as expression]
            [edi.receiver.utils.transform :as transform]
            [edi.common.util.core :as util]
            [edi.common.util.core :as utils]))


(defn- create-backend [{:keys [name type] :as config}]
  (let [config (dissoc config :name :type :enabled)]
    [(keyword name)
     (case type
       "http" (http/create config)
       "kafka" (kafka/create config)
       "smtp" (javamail/create config)
       "dump" (dump/create config)
       (throw (ex-info (format "Unknown backend: %s" type) config)))]))


(defn- create-backends [config]
  (->> config
       util/ordered-vals
       (filter :enabled)
       (map create-backend)
       (into {})))


(defn- wrap-condition [send condition-text]
  (let [condition (-> condition-text edn/read-string expression/prepare)]
    (fn [backend topic message]
      (log/debugf "applying condition %s to message\n%s" condition-text (utils/pretty message))
      (if (expression/evaluate condition message)
        (send backend topic message)
        (do (log/debugf "Proxying to %s, topic %s skipped via condition" (.getName (class backend)) topic)
            :skipped-via-condition)))))


(defn- wrap-transform [send transform]
  (let [rules (-> transform edn/read-string transform/prepare)]
    (fn [backend topic message]
      (let [message' (transform/transform rules message)]
        (log/debugf "message after transform\n%s" (utils/pretty message))
        (send backend topic message')))))


(defn- wrap-unreliable [send]
  (fn [backend topic message]
    (try
      (send backend topic message)
      (catch Exception e
        (log/warnf "cant send to unreliable %s, topic %s: %s: %s\ndata:\n%s\nmessage:\n%s"
                   (.getName (class backend))
                   topic
                   (-> e class .getName)
                   (ex-message e)
                   (util/pretty (ex-data e))
                   (util/pretty message))))))


(defn- create-proxies [config backends]
  (let [create-proxy (fn [{:keys [backend reliable source target condition transform]}]
                       (let [send (cond-> protocol/send-message
                                          transform (wrap-transform transform)
                                          condition (wrap-condition condition)
                                          (not reliable) wrap-unreliable)]
                         {:source source
                          :send   (partial send (get backends (keyword backend)) target)}))]
    (->> config
         util/ordered-vals
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
      (log/debug "proxying result" (util/pretty result)))))