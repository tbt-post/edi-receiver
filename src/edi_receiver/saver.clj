(ns edi-receiver.saver
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [edi-receiver.backend.core :as backend]
            [edi-receiver.db.jdbc :as db]
            [edi-receiver.db.models :as models]
            [edi-receiver.upstream :as upstream]))


(defn- save! [db topic message]
  (let [[table values] (models/converter (:driver db) topic message)]
    (db/insert! db table values)))


(defn process-message! [{:keys [db backend upstream]} topic message]
  (log/debug "validating message from" topic)
  (upstream/validate upstream topic message)
  (jdbc/with-db-transaction
    [tx db]
    (log/debug "saving message from" topic)
    (let [result (save! tx topic message)]
      (backend/send-message backend topic message)
      result)))


(defn- test-message [{:keys [db upstream] :as context} topic message]
  (upstream/validate upstream topic message)
  (jdbc/with-db-transaction
    [tx db]
    (swap! (:rollback tx) (constantly true))
    (save! tx topic message)))


(defn run-tests [{:keys [upstream] :as context}]
  (upstream/run-tests upstream (partial test-message context)))
