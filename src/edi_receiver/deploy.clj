(ns edi-receiver.deploy
  (:require [edi-receiver.db.pg :as pg]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]))


(defn- run-script! [pg resource-path]
  (pg/run-script! pg (-> resource-path io/resource slurp)))


(defn deploy! [{:keys [pg config]}]
  (log/info "Initializing database:" (-> config :pg :database))
  (run-script! pg "sql/init.sql")
  (doseq [topic (-> config :upstream :topics)]
    (log/info "Initializing topic:" (name topic))
    (run-script! pg (format "sql/tables/%s.sql" (name topic))))
  (log/info "Database initialization succeeded"))
