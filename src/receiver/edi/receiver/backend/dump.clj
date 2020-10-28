(ns edi.receiver.backend.dump
  (:require [edi.receiver.backend.protocol :as protocol]))


(deftype DumpBackend [fail?]

  protocol/Backend

  (send-message [_ topic message]
    (println "-------------- DUMP --------------")
    (println topic)
    (println "----------------------------------")
    (clojure.pprint/pprint message)
    (println "----------------------------------")
    (if fail?
      (do (println "Performing test fail!")
          (throw (ex-info "Test backend fail" {:some "data"})))
      {:some "value"}))

  (close [_]))


(defn create [{:keys [fail]}]
  (DumpBackend. fail))
