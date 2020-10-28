(ns edi.receiver.db-log
  (:require [cheshire.core :as json]
            [clojure.tools.logging :as log]
            [edi.common.db.jdbc :as db]))


(def ^:private write-qs
  {:postgresql "INSERT INTO log (created_at, context, reference, content, raw) VALUES (now(), ?::jsonb, ?::jsonb, ?::jsonb, ?)"
   :mysql      "INSERT INTO log (created_at, context, reference, content, raw) VALUES (now(6), ?, ?, ?, ?)"})


(defn- write-q [{:keys [driver]}]
  (get write-qs driver))


(defn write! [{:keys [db]} context reference content raw-content]
  (log/debug "proxy logging")
  (db/execute! db [(write-q db)
                   context
                   (json/generate-string reference)
                   (json/generate-string content)
                   raw-content]))


(defn create [{:keys [db]}]
  (log/debug "creating db-log")
  {:db db})
