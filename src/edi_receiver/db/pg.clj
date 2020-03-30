(ns edi-receiver.db.pg
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [clojure.string :as string])
  (:import (com.mchange.v2.c3p0 ComboPooledDataSource)))


(defn connect [{:keys [driver-class
                       host
                       port
                       database
                       user
                       password
                       max-idle-time-excess-connections
                       max-idle-time
                       initial-pool-size
                       min-pool-size
                       max-pool-size
                       max-statements
                       max-statements-per-connection
                       description
                       acquire-increment
                       unreturned-connection-timeout]
                :or   {driver-class                     "org.postgresql.Driver"
                       host                             "localhost"
                       port                             5432
                       ;; expire excess connections after 30 minutes of inactivity:
                       max-idle-time-excess-connections (* 30 60)
                       ;; expire connections after 3 hours of inactivity:
                       max-idle-time                    (* 3 60 60)
                       initial-pool-size                3
                       min-pool-size                    3
                       max-pool-size                    10
                       max-statements                   0
                       max-statements-per-connection    0
                       description                      "clojure c3p0 pool"
                       acquire-increment                1
                       unreturned-connection-timeout    0}}]
  (log/info "Creating PostgreSQL pool")
  (System/setProperty "user.timezone" "UTC")
  {:datasource (doto
                 (ComboPooledDataSource.)
                 (.setDriverClass driver-class)
                 (.setJdbcUrl (format "jdbc:postgresql://%s:%s/%s?prepareThreshold=0" host port database))
                 (.setUser user)
                 (.setPassword password)
                 (.setMaxIdleTimeExcessConnections max-idle-time-excess-connections)
                 (.setMaxIdleTime max-idle-time)
                 (.setInitialPoolSize initial-pool-size)
                 (.setMinPoolSize min-pool-size)
                 (.setMaxPoolSize max-pool-size)
                 (.setMaxStatements max-statements)
                 (.setMaxStatementsPerConnection max-statements-per-connection)
                 (.setDescription description)
                 (.setAcquireIncrement acquire-increment)
                 (.setUnreturnedConnectionTimeout unreturned-connection-timeout))})


(defn close [pool]
  (log/info "Destroing PostgreSQL pool")
  (.close (:datasource pool)))


(defn execute! [pool query]
  (first (jdbc/execute! pool query)))


(defn insert-q [table fields]
  (str "INSERT INTO " (name table) " ("
       (string/join "," (map name fields))
       ") VALUES ("
       (string/join "," (repeat (count fields) "?"))
       ")"))


(defn insert! [pool table values]
  (let [fields (keys values)]
    (execute! pool (cons (insert-q table fields)
                         (for [field fields] (field values))))))


(defn run-script! [pool sql]
  (jdbc/with-db-transaction
    [tr pool]
    (->> (-> sql
             (string/replace #"(?s)/\*.*\*/|--[^\n]*" "")
             (string/split #";"))
         (map string/trim)
         (remove #(= "" %))
         (map #(do
                 (log/debug "Executing" %)
                 (execute! tr %)))
         (doall))))
