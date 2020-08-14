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
    (when fail?
      (println "Performing test fail!")
      (throw (ex-info "Test backend fail" {:some "data"}))))

  (close [_]))


(defn create [{:keys [fail]}]
  (DumpBackend. fail))
