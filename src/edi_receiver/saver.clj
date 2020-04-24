(ns edi-receiver.saver
  (:require [clojure.java.jdbc :as jdbc]
            [edi-receiver.db.jdbc :as db]
            [edi-receiver.db.models :as models]
            [edi-receiver.upstream :as upstream]))


(defn process-message! [{:keys [db upstream]} topic message]
  (if-let [error (upstream/validate upstream topic message)]
    (throw (ex-info "Message not valid" error))
    (let [[table values] (models/converter (:driver db) topic message)]
      (db/insert! db table values))))


(defn- test-message! [{:keys [db] :as context} topic message]
  (jdbc/with-db-transaction [tx db]
                            (swap! (:rollback tx) (constantly true))
                            (process-message! (assoc context :db tx) topic message)))


(defn run-tests! [{:keys [upstream] :as context}]
  (upstream/run-tests! upstream (partial test-message! context)))
