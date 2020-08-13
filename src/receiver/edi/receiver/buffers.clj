(ns edi.receiver.buffers
  (:require [cheshire.core :as json]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [edi.common.db.jdbc :as db]
            [edi.common.util.core :as utils]))


(def ^:private sqls
  (-> {:common
       {:drop        "DELETE FROM ringbuffer WHERE id=?"
        :buffer-size "SELECT COUNT(*) AS size FROM ringbuffer WHERE instance_id=?"
        :update      "UPDATE ringbuffer SET tries=?, error=? WHERE id=?"}
       :postgresql
       {:push   (str "INSERT INTO ringbuffer (instance_id, created_at, planned_at, payload, error) "
                     "VALUES (?, now(), now() + make_interval(secs => ?/1000.), ?, ?)")
        :plan   (str "UPDATE ringbuffer "
                     "SET planned_at=now()+make_interval(secs => ?/1000.) "
                     "WHERE instance_id=?")
        :select (str "SELECT id, instance_id, extract('epoch' from (now() - created_at)) AS age, tries, payload "
                     "FROM ringbuffer "
                     "WHERE now() > planned_at "
                     "ORDER BY tries, created_at "
                     "LIMIT 1")}
       :mysql
       {:push   (str "INSERT INTO ringbuffer (instance_id, created_at, planned_at, payload, error) "
                     "VALUES (?, now(6), date_add(now(6), interval ?/1000. second), ?, ?)")
        :plan   (str "UPDATE ringbuffer "
                     "SET planned_at=date_add(now(6), interval ?/1000. second) "
                     "WHERE instance_id=?")
        :select (str "SELECT id, instance_id, timestampdiff(second, created_at, now(6)) AS age, tries, payload "
                     "FROM ringbuffer "
                     "WHERE now(6) > planned_at "
                     "ORDER BY tries, created_at "
                     "LIMIT 1")}}
      (utils/merge-common :common)))


(defn- sql [{:keys [driver]} name]
  (get-in sqls [driver name]))


(defn- drop! [db id]
  (db/execute! db [(sql db :drop) id]))


(defn push! [{:keys [db instance-id config] :as _instance} payload error]
  (log/debug "pushing payload to buffer")
  (let [{:keys [max-size tries-interval-ms]} config]
    (when-not (-> (db/one db [(sql db :buffer-size) instance-id])
                  :size
                  (>= max-size))
      (db/execute! db [(sql db :push)
                       instance-id
                       tries-interval-ms
                       (json/generate-string payload)
                       (json/generate-string error)]))))


(defn- reformat-json [s]
  (-> s
      (json/parse-string)
      (json/generate-string {:pretty true})))


(defn- expire! [{:keys [db] :as _instance} {:keys [id instance_id payload error] :as _item}]
  (log/warnf "dropping expired #%s" id)
  (log/warnf "buffer %s: dropping expired payload, last error:\n%s\ndropped message:\n%s"
             instance_id
             (reformat-json error)
             (reformat-json payload))
  (drop! db id))


(defn- pushback! [{:keys [db config] :as instance}
                  {:keys [id instance_id age tries error] :as item}]
  (log/debugf "pushback #%s" id)
  (let [{:keys [max-tries expire-time-s tries-interval-ms]} config
        tries (inc tries)]
    (cond
      (>= tries max-tries)
      (do
        (log/debugf "try count %s exceeded" tries)
        (expire! instance item))

      (and expire-time-s (>= age expire-time-s))
      (do
        (log/debugf "try payload expired (%s >= %s sec)" age expire-time-s)
        (expire! instance item))

      :else
      (jdbc/with-db-transaction
        [tx db]
        (db/execute! tx [(sql db :plan) tries-interval-ms instance_id])
        (db/execute! tx [(sql db :update) tries error id])))))


(defn- main-loop [{:keys [db defaults *instances *thread] :as _buffers}]
  (log/info "started buffers thread")
  (loop []
    (if-let [{:keys [id instance_id payload] :as item}
             (db/one db [(sql db :select)])]
      (if-let [{:keys [handler] :as instance} (get @*instances instance_id)]
        (do
          (log/debug "got item" item)
          (log/debugf "retrying #%s, try %s" (:id item) (:tries item))
          (let [{:keys [error] :as result} (handler (json/parse-string payload keyword))]
            (log/debug "handler returned" result)
            (if-not error
              (drop! db id)
              (pushback! instance
                         (-> item
                             (assoc :error (json/generate-string error)))))))
        (do (log/errorf "unknown buffer instance: %s, dropping payload %s" instance_id payload)
            (drop! db id)))
      ; empty buffers
      (Thread/sleep (:tries-interval-ms defaults)))
    (when @*thread
      (recur))))


(defn start [{:keys [*instances *thread] :as buffers}]
  (if (not-empty @*instances)
    (let [thread (Thread. #(main-loop buffers))]
      (.start thread)
      (reset! *thread thread))
    (log/info "no buffers registered, worker thread not started")))


(defn stop [{:keys [*thread]}]
  (log/info "stopping buffers thread")
  (when-let [thread (swap! *thread (constantly nil))]
    (.join thread 0)))


(defn register! [{:keys [db defaults *instances]} instance-id handler config]
  (log/debug "registering buffer" instance-id)
  (let [instance {:db          db
                  :instance-id instance-id
                  :handler     handler
                  :config      (merge defaults config)}]
    (swap! *instances #(assoc % instance-id instance))
    instance))


(defn create [{:keys [config db]}]
  (log/debug "creating buffers")
  {:db         db
   :defaults   (-> config :buffer)
   :*instances (atom {})
   :*thread    (atom nil)})
