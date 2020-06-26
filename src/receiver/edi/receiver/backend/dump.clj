(ns edi.receiver.backend.dump
  (:require [edi.receiver.backend.protocol :as protocol]))


(deftype DumpBackend []

  protocol/Backend

  (send-message [_ topic message]
    (println "-------------- DUMP --------------")
    (println topic)
    (println "----------------------------------")
    (clojure.pprint/pprint message)
    (println "----------------------------------"))

  (close [_]))


(defn create [_]
  (DumpBackend.))
