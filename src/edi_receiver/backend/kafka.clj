(ns edi-receiver.backend.kafka
  (:require [cheshire.core :as json]
            [clojure.tools.logging :as log]
            [clj-kafka-x.producer :as kp]
            [edi-receiver.backend.protocol :as protocol]))


(deftype KafkaBackend [producer]

  protocol/Backend

  (send-message [_ topic message]
    (log/debug "proxying message to kafka, topic" topic)
    @(kp/send producer (kp/record topic (json/encode message))))

  (close [_]
    (log/info "Closing kafka producer")
    (.close producer)))


(defn create [{:keys [bootstrap-servers]}]
  (log/info "Initializing kafka producer")
  (KafkaBackend.
    (kp/producer {"bootstrap.servers" bootstrap-servers}
                 (kp/string-serializer)
                 (kp/string-serializer))))
