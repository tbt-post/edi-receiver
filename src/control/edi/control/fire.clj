(ns edi.control.fire
  (:require [cheshire.core :as json]
            [edi.common.util.jetty-client :as http]
            [edi.common.util.stats :as stats])
  (:import (java.time Duration)))


(defn- request [client payload]
  (http/request client payload))


(defn- thread [{:keys [payloads requests-per-thread]} stats]
  (let [client   (http/client)
        start-at (stats/now)]
    (dotimes [_ requests-per-thread]
      (request client (rand-nth payloads)))
    (swap! stats #(conj % {:total-time-micros (stats/micro-seconds (Duration/between start-at (stats/now)))
                           :count             requests-per-thread}))))


(defn fire! [{:keys [fire]}]
  (let [{:keys [threads requests-per-thread] :as config}
        (->> (-> fire
                 slurp
                 (json/decode keyword)))
        stats (atom [])]

    (println (format "Requests per thread: %s" requests-per-thread))
    (println (format "Starting %s threads" threads))

    (let [threads (repeatedly threads #(Thread. ^Runnable (fn [] (thread config stats))))]

      (run! #(.start %) threads)

      (println "Working...")

      (run! #(.join %) threads)

      (println (format "Average request time: %s ms"
                       (float (/ (reduce + (map :total-time-micros @stats))
                                 (reduce + (map :count @stats))
                                 1000)))))))
