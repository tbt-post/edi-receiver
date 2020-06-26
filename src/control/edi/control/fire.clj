(ns edi.control.fire
  (:require [cheshire.core :as json]
            [clojure.tools.logging :as log]
            [edi.common.util.jetty-client :as http]
            [edi.common.util.timer :as timer]))


(defn- request [client payload]
  (try
    (-> (http/request client payload)
        :status
        (< 300))
    (catch Exception e
      (log/errorf "HTTP request exception: %s: %s" (.getName (class e)) e)
      false)))


(defn- thread [{:keys [payloads requests-per-thread]} stats]
  (let [client    (http/client)
        start-at  (timer/now)
        successes (->> requests-per-thread
                       range
                       (map (fn [_] (request client (rand-nth payloads))))
                       (filter true?)
                       count)]
    (swap! stats #(conj % {:total-time-micros (timer/delta-micros start-at (timer/now))
                           :count             requests-per-thread
                           :successes         successes
                           :fails             (- requests-per-thread successes)}))))


(defn fire! [{:keys [fire]}]
  (let [{:keys [threads requests-per-thread] :as config}
        (->> (-> fire
                 slurp
                 (json/decode keyword)))
        stats (atom [])]

    (println (format "Requests per thread: %s" requests-per-thread))
    (println (format "Starting %s threads" threads))

    (let [jobs     (repeatedly threads #(Thread. ^Runnable (fn [] (thread config stats))))
          start-at (timer/now)]

      (run! #(.start %) jobs)

      (println "Working...")

      (run! #(.join %) jobs)

      (let [total-time      (timer/delta-micros start-at (timer/now))
            total-requests  (reduce + (map :count @stats))
            total-successes (reduce + (map :successes @stats))]
        (println (format "Average request time: %s ms"
                         (double (/ (reduce + (map :total-time-micros @stats))
                                    total-requests
                                    1000))))
        (println (format "Requests per second: %s" (-> total-successes
                                                       (* 1000000)
                                                       (/ total-time)
                                                       double)))
        (println (format "Total requests: %s" total-requests))
        (println (format "Total successes: %s" total-successes))
        (println (format "Total fails: %s" (reduce + (map :fails @stats)))))

      ; why no exit without this?
      (System/exit 0))))
