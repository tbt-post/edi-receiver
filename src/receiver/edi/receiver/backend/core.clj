(ns edi.receiver.backend.core
  (:require [cheshire.core :as json]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [clojure.tools.reader.edn :as edn]
            [edi.common.util.core :as util]
            [edi.receiver.backend.dump :as dump]
            [edi.receiver.backend.http :as http]
            [edi.receiver.backend.javamail :as javamail]
            [edi.receiver.backend.kafka :as kafka]
            [edi.receiver.backend.protocol :as protocol]
            [edi.receiver.buffers :as buffers]
            [edi.receiver.db-log :as db-log]
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


(defn- create-proxy [backends buffers log
                     {:keys [key backend reliable buffer source target condition transform logging] :as proxy-conf}]
  (letfn [(wrap-logging [send {:keys [enabled reference-fields]}]
            (if enabled
              (let [context          (-> proxy-conf
                                         (select-keys [:key :target :backend])
                                         json/generate-string)
                    reference-fields (->> (string/split reference-fields #",")
                                          (map string/trim)
                                          (map keyword))]
                (fn [message]
                  (let [response (send message)
                        [content raw] (if (-> response :content bytes?)
                                        [(dissoc response :body :content) (:content response)]
                                        [response nil])]
                    (db-log/write! log
                                   context
                                   (select-keys message reference-fields)
                                   (util/remove-vals nil? content)
                                   raw)
                    response)))
              send))

          (wrap-unreliable [send]
            (fn [message]
              (try
                (send message)
                (catch Exception e
                  (let [e-class   (-> e class .getName)
                        e-message (ex-message e)
                        e-data    (ex-data e)]
                    (log/warnf "cant send to unreliable %s, topic %s: %s: %s\ndata:\n%s\ndropped message:\n%s"
                               backend target e-class e-message (util/pretty e-data) (util/pretty message))
                    {:action  :dropped-by-unreliable-backend
                     :error   e-class
                     :message e-message
                     :data    e-data})))))

          (wrap-buffer [send {:keys [enabled] :as config}]
            (if enabled
              (let [instance-id (-> key name edn/read-string)
                    instance    (buffers/register! buffers instance-id (wrap-error send) config)]
                (fn [message]
                  (try
                    (send message)
                    (catch Exception e
                      (let [error (exception->dict e)]
                        (if (buffers/push! instance message error)
                          {:action :sent-to-buffer-for-retry}
                          (do
                            ; buffer overflow
                            (log/debug "buffer #%s overflow" instance-id)
                            (throw (ex-info (format "Buffer %s overflow" instance-id) error)))))))))

              send))

          (wrap-transform [send transform]
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
                      {:action :skipped-via-condition})))))]
    {:source source
     :send   (cond-> (partial protocol/send-message
                              (get backends (keyword backend))
                              target)
                     logging (wrap-logging logging)
                     (not reliable) wrap-unreliable
                     buffer (wrap-buffer buffer)
                     transform (wrap-transform transform)
                     condition (wrap-condition condition))}))



(defn- create-proxies [config backends buffers log]
  (->> config
       util/ordered-configs
       (filter :enabled)
       (filter #(get backends (keyword (:backend %))))
       (map (partial create-proxy backends buffers log))
       (group-by :source)
       (map (fn [[source proxies]] [(keyword source)
                                    (mapv :send proxies)]))
       (into {})))


(defn create [{:keys [config buffers log]}]
  (let [{:keys [backend proxy]} config
        backends (create-backends backend)]
    {:backends backends
     :proxies  (create-proxies proxy backends buffers log)}))


(defn close [{:keys [backends]}]
  (->> backends
       vals
       (run! protocol/close)))


(defn send-message [{:keys [proxies]} topic message]
  (doseq [proxy (get proxies topic)]
    (let [result (proxy message)]
      (log/debug "proxying result" (util/pretty (dissoc result :media-type :encoding :content))))))