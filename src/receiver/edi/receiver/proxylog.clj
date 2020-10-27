(ns edi.receiver.proxylog
  (:require [cheshire.core :as json]
            [clojure.tools.logging :as log]
            [edi.common.db.jdbc :as db]))


(def ^:private write-qs
  {:postgresql "INSERT INTO proxylog (created_at, context, reference, response) VALUES (now(), ?::jsonb, ?::jsonb, ?::jsonb)"
   :mysql      "INSERT INTO proxylog (created_at, context, reference, response) VALUES (now(6), ?, ?, ?)"})


(defn- write-q [{:keys [driver]}]
  (get write-qs driver))


(defn write! [{:keys [db]} context reference response]
  (log/debug "proxy logging")
  (db/execute! db [(write-q db)
                   context
                   (json/generate-string reference)
                   (json/generate-string response)]))


(defn create [{:keys [db]}]
  (log/debug "creating proxylog")
  {:db db})
