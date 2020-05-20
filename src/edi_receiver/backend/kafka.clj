(ns edi-receiver.backend.kafka
  (:require [cheshire.core :as json]
            [clojure.tools.logging :as log]
            [edi-receiver.backend.protocol :as protocol])
  (:import (org.apache.kafka.clients.producer KafkaProducer ProducerRecord)
           (org.apache.kafka.common.serialization StringSerializer)))


(deftype KafkaBackend [producer]

  protocol/Backend

  (send-message [_ topic message]
    (log/debug "proxying message to kafka, topic" topic)
    @(.send producer (ProducerRecord. topic (json/encode message))))

  (close [_]
    (log/info "Closing kafka producer")
    (.close producer)))


(defn create [{:keys [bootstrap-servers]}]
  (log/info "Initializing kafka producer")
  (KafkaBackend. (KafkaProducer. {"bootstrap.servers" bootstrap-servers}
                                 (StringSerializer.)
                                 (StringSerializer.))))
