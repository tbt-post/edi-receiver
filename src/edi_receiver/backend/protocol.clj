(ns edi-receiver.backend.protocol)


(defprotocol Backend
  (send-message [this topic message])
  (close [this]))
