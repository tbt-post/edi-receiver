(ns edi-receiver.deploy
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [edi-receiver.db.jdbc :as db]
            [edi-receiver.db.models :as models]))


(defn deploy! [{:keys [db config]}]
  (log/info "Initializing database")
  (some->> (format "sql/init/%s.sql" (-> db :driver name))
           io/resource
           slurp
           (db/run-script! db))
  (doseq [topic (-> config :upstream :topics)]
    (log/info "Initializing topic:" topic)
    (->> (keyword topic)
         (models/create-tables-q (-> db :driver))
         (db/run-script! db)))
  (log/info "Database initialization succeeded"))
