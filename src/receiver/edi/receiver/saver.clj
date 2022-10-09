(ns edi.receiver.saver
  (:require [cheshire.core :as json]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [edi.common.db.jdbc :as db]
            [edi.common.db.models :as models]
            [edi.common.util.core :as util]
            [edi.common.util.timer :as timer]
            [edi.receiver.backend.core :as backend]
            [edi.receiver.stats :as stats]
            [edi.receiver.upstream :as upstream]
            [java-properties.core :as jconf])
  (:import (java.util UUID)
           (org.postgresql.util PGobject)))


(def type-coerce
  {:postgresql {:money     #(some-> % str bigdec)
                :json      #(doto (PGobject.)
                              (.setType "json")
                              (.setValue (some-> % json/generate-string)))
                :timestamp #(doto (PGobject.)
                              (.setType "timestamptz")
                              (.setValue %))
                :uuid      #(some-> % UUID/fromString)}
   :mysql      {:money     #(some-> % str bigdec)
                :json      #(some-> % json/generate-string)
                :timestamp #(when %
                              (jconf/parse-java-util-date %))
                :uuid      #(some-> % UUID/fromString util/uuid->byte-array)}})


(defn- coerce-item [type-coerce model [key value]]
  (when-let [{:keys [type alias]} (get model key)]
    (let [as (-> (get type-coerce type)
                 (or identity))]
      [(or alias key) (as value)])))


(defn- coerce-message [type-coerce model message]
  (->> message
       (map (partial coerce-item type-coerce model))
       (remove nil?)
       (into {})))


(defn- converter [driver topic message]
  (->> (for [{:keys [table when?]} (models/tables-meta topic)]
         (when (when? message)
           [table (->> message
                       (coerce-message (get type-coerce driver)
                                       (get models/models table)))]))
       (remove nil?)
       first))


(defn- save! [db topic message stats]
  (let [[table values] (converter (:driver db) topic message)]
    (let [started-at (timer/now)]
      (let [rowcount (db/insert! db table values)]
        (when stats
          (stats/after-sql stats started-at))
        rowcount))))


(defn process-message! [{:keys [db backend upstream stats]} topic message]
  (log/debug "validating message from" topic)
  (upstream/validate upstream topic message)
  (jdbc/with-db-transaction
    [tx db]
    (log/debug "saving message from" topic)
    (let [result (save! tx topic message stats)]
      (backend/send-message backend topic message)
      result)))


(defn- test-message [{:keys [db upstream] :as context} topic message]
  (upstream/validate upstream topic message)
  (jdbc/with-db-transaction
    [tx db]
    (swap! (:rollback tx) (constantly true))
    (save! tx topic message nil)))


(defn run-tests [{:keys [upstream] :as context}]
  (upstream/run-tests upstream (partial test-message context)))
