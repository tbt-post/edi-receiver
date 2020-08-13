(ns edi.receiver.backend.core
  (:require [clojure.tools.logging :as log]
            [clojure.tools.reader.edn :as edn]
            [edi.common.util.core :as util]
            [edi.receiver.backend.dump :as dump]
            [edi.receiver.backend.http :as http]
            [edi.receiver.backend.javamail :as javamail]
            [edi.receiver.backend.kafka :as kafka]
            [edi.receiver.backend.protocol :as protocol]
            [edi.receiver.buffers :as buffers]
            [edi.receiver.utils.expression :as expression]
            [edi.receiver.utils.transform :as transform]))


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
       util/ordered-configs
       (filter :enabled)
       (map create-backend)
       (into {})))


(defn- exception->dict [e]
  {:exception (-> e class .getName)
   :message   (ex-message e)
   :data      (ex-data e)})


(defn- wrap-error [send]
  (fn [message]
    (try
      {:result (send message)}
      (catch Exception e
        {:error (exception->dict e)}))))


(defn- create-proxy [backends buffers {:keys [key backend reliable buffer source target condition transform]}]
  (letfn [(wrap-transform [send transform]
            (let [rules (-> transform edn/read-string transform/prepare)]
              (fn [message]
                (let [message' (transform/transform rules message)]
                  ;(log/debugf "message after transform\n%s" (util/pretty message))
                  (send message')))))

          (wrap-condition [send text-condition]
            (let [condition (-> text-condition edn/read-string expression/prepare)]
              (fn [message]
                ;(log/debugf "applying condition %s to message\n%s" text-condition (util/pretty message))
                (if (expression/evaluate condition message)
                  (send message)
                  (do (log/debugf "Proxying to %s, topic %s skipped via condition" backend target)
                      :skipped-via-condition)))))

          (wrap-buffer [send buffers instance-id {:keys [enabled] :as config}]
            (if enabled
              (let [instance (buffers/register! buffers instance-id (wrap-error send) config)]
                (fn [message]
                  (try
                    (send message)
                    (catch Exception e
                      (let [error (exception->dict e)]
                        (if (buffers/push! instance message error)
                          :sent-to-buffer-for-retry
                          (do
                            ; buffer overflow
                            (log/debug "buffer #%s overflow" instance-id)
                            (throw (ex-info (format "Buffer %s overflow" instance-id) error)))))))))

              send))

          (wrap-unreliable [send]
            (fn [message]
              (try
                (send message)
                (catch Exception e
                  (log/warnf "cant send to unreliable %s, topic %s: %s: %s\ndata:\n%s\ndropped message:\n%s"
                             backend
                             target
                             (-> e class .getName)
                             (ex-message e)
                             (util/pretty (ex-data e))
                             (util/pretty message))
                  :dropped-by-unreliable-backend))))]
    {:source source
     :send   (cond-> (partial protocol/send-message
                              (get backends (keyword backend))
                              target)
                     (not reliable) wrap-unreliable
                     buffer (wrap-buffer buffers (-> key name edn/read-string) buffer)
                     transform (wrap-transform transform)
                     condition (wrap-condition condition))}))



(defn- create-proxies [config backends buffers]
  (->> config
       util/ordered-configs
       (filter :enabled)
       (filter #(get backends (keyword (:backend %))))
       (map (partial create-proxy backends buffers))
       (group-by :source)
       (map (fn [[source proxies]] [(keyword source)
                                    (mapv :send proxies)]))
       (into {})))


(defn create [{:keys [config buffers]}]
  (let [{:keys [backend proxy]} config
        backends (create-backends backend)]
    {:backends backends
     :proxies  (create-proxies proxy backends buffers)}))


(defn close [{:keys [backends]}]
  (->> backends
       vals
       (run! protocol/close)))


(defn send-message [{:keys [proxies]} topic message]
  (doseq [proxy (get proxies topic)]
    (let [result (proxy message)]
      (log/debug "proxying result" (util/pretty result)))))