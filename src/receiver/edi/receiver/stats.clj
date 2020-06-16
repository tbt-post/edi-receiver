(ns edi.receiver.stats
  (:require [edi.common.db.jdbc :as db]
            [edi.common.config :as config]
            [clojure.string :as string]
            [clojure.tools.logging :as log])
  (:import (java.time Instant Duration Clock)
           (java.time.temporal ChronoUnit)
           (edi.common.util NanoClock)))


#_{:created-at                        <datetime>
   :topics                            [<str>]
   :db-version                        <str>

   :last-request-at                   <datetime>
   :last-sql-at                       <datetime>

   :total-request-time                <timedelta>
   :request-count                     <long>
   :total-content-length              <long>

   :total-sql-time                    <timedelta>
   :sql-count                         <long>

   :last-activity-at                  {:topic <datetime>}

   :current-hour                      <datetime>
   :current-hour-request-count        <long>
   :current-hour-total-content-length <long>

   :current-day                       <date>
   :current-day-request-count         <long>
   :current-day-total-content-length  <long>}

(def ^:private stats (atom {}))
(def ^:private ^Clock clock (if (string/starts-with? (System/getProperty "java.version") "1.8")
                              (NanoClock.)
                              (Clock/systemUTC)))

(defn- safe-div [a b]
  (when (-> b (or 0) (not= 0))
    (-> a (or 0) (/ b) double)))


(defn- safe-inc [x]
  (inc (or x 0)))


(defn now []
  (.instant clock))


(defn- current-unit-stats
  "returns [request-count-per-s avg-content-length]"
  [^Instant now current-at request-count total-content-length unit]
  (let [start-at (-> now (.truncatedTo unit))]
    (if (= start-at current-at)
      (let [uptime-s (-> (Duration/between start-at now) .toMillis (/ 1000))]
        [(safe-div request-count uptime-s)
         (safe-div total-content-length request-count)])
      [0 nil])))


(defn- format-duration [^Duration d]
  (let [ss (-> d .toMillis (quot 1000))
        s  (rem ss 60)
        mm (quot ss 60)
        m  (rem mm 60)
        hh (quot mm 60)]
    (format "%sh %sm %ss" hh m s)))


(defn get-stats [stats]
  (let [{:keys [created-at
                request-count
                total-request-time
                sql-count
                total-sql-time
                total-content-length
                current-day
                current-day-request-count
                current-day-total-content-length
                current-hour
                current-hour-request-count
                current-hour-total-content-length] :as stats} @stats]
    (let [now                 (now)
          uptime              (Duration/between created-at now)
          uptime-s            (-> uptime .toMillis (/ 1000))

          request-count-per-s (safe-div request-count uptime-s)
          avg-content-length  (safe-div total-content-length request-count)

          [current-day-request-count-per-s
           current-day-avg-content-length] (current-unit-stats now
                                                               current-day
                                                               current-day-request-count
                                                               current-day-total-content-length
                                                               ChronoUnit/DAYS)
          [current-hour-request-count-per-s
           current-hour-avg-content-length] (current-unit-stats now
                                                                current-hour
                                                                current-hour-request-count
                                                                current-hour-total-content-length
                                                                ChronoUnit/HOURS)]
      (->> (-> stats
               (select-keys [:topics
                             :db-version
                             :last-activity-at
                             :last-activity
                             :last-request-at
                             :last-sql-at
                             :request-count
                             :current-day-request-count
                             :current-hour-request-count])
               (assoc
                 :x-raw-stats stats

                 :uptime (format-duration uptime)

                 :avg-request-time-micros (safe-div total-request-time request-count)
                 :avg-sql-time-micros (safe-div total-sql-time sql-count)

                 :request-count-per-s request-count-per-s
                 :request-count-per-m (* request-count-per-s 60)
                 :request-count-per-h (* request-count-per-s 60 60)

                 :current-day-request-count-per-s current-day-request-count-per-s
                 :current-day-request-count-per-m (* current-day-request-count-per-s 60)
                 :current-day-request-count-per-h (* current-day-request-count-per-s 60 60)

                 :current-hour-request-count-per-s current-hour-request-count-per-s
                 :current-hour-request-count-per-m (* current-hour-request-count-per-s 60)
                 :current-hour-request-count-per-h (* current-hour-request-count-per-s 60 60)

                 :avg-content-length avg-content-length
                 :current-day-avg-content-length current-day-avg-content-length
                 :current-hour-avg-content-length current-hour-avg-content-length))
           (sort-by first)
           (apply concat)
           (apply array-map)
           #_(into (array-map))))))


(defn- micro-seconds [^Duration duration]
  (-> duration .toNanos (/ 1000)))


(defn create [{:keys [config db]}]
  (log/info "Initializing stats, clock is" (.getName (class clock)))
  (swap! stats #(assoc % :created-at (now)
                         :topics (config/get-topics config)
                         :db-version (db/db-version db)))
  stats)


(defn- update-current-day-stats [stats content-length]
  (let [current-day (-> (now)
                        (.truncatedTo ChronoUnit/DAYS))]
    (if (= current-day (:current-day stats))
      (-> stats
          (update :current-day-request-count inc)
          (update :current-day-total-content-length #(+ % content-length)))
      (-> stats
          (assoc :current-day current-day
                 :current-day-request-count 1
                 :current-day-total-content-length content-length)))))


(defn- update-current-hour-stats [stats content-length]
  (let [current-hour (-> (now)
                         (.truncatedTo ChronoUnit/HOURS))]
    (if (= current-hour (:current-hour stats))
      (-> stats
          (update :current-hour-request-count inc)
          (update :current-hour-total-content-length #(+ % content-length)))
      (-> stats
          (assoc :current-hour current-hour
                 :current-hour-request-count 1
                 :current-hour-total-content-length content-length)))))


(defn before-activity [stats now]
  (swap! stats #(assoc % :last-activity-at now)))


(defn before-request [stats topic now]
  (swap! stats (fn [stats]
                 (update stats :last-activity #(assoc % topic now)))))


(defn after-request [stats started-at content-length]
  (let [now (now)
        dt  (Duration/between started-at now)]
    (swap! stats (fn [stats]
                   (-> stats
                       (assoc :last-request-at now)
                       (update :total-request-time #(-> % (or 0) (+ (micro-seconds dt))))
                       (update :request-count safe-inc)
                       (update :total-content-length #(-> % (or 0) (+ content-length)))
                       (update-current-day-stats content-length)
                       (update-current-hour-stats content-length))))))


(defn after-sql [stats started-at]
  (let [now (now)
        dt  (Duration/between started-at now)]
    (swap! stats (fn [stats]
                   (-> stats
                       (assoc :last-sql-at now)
                       (update :total-sql-time #(-> % (or 0) (+ (micro-seconds dt))))
                       (update :sql-count safe-inc))))))
