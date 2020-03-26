(ns edi-receiver.deploy
  (:require [edi-receiver.db.pg :as pg]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]))


(defn deploy [{pg-conf :pg}]
  (log/info "Initializing database:" (:database pg-conf))
  (pg/run-script!
    (pg/connect pg-conf)
    (-> "sql/init.sql" io/resource slurp))
  (log/info "Ok"))
