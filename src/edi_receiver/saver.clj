(ns edi-receiver.saver
  (:require [edi-receiver.upstream :as upstream]
            [edi-receiver.db.pg :as pg]
            [cheshire.core :as json]
            [clojure.java.jdbc :as jdbc])
  (:import (java.util UUID)
           (org.postgresql.util PGobject)
           (clojure.lang ExceptionInfo)))


(defn- as-uuid [s] (UUID/fromString s))

(defn- as-json [v]
  (doto (PGobject.)
    (.setType "json")
    (.setValue (json/generate-string v))))

(defn- as-enum
  "Convert a keyword value into an enum-compatible object."
  [enum-type value]
  (doto (PGobject.)
    (.setType (name enum-type))
    (.setValue (name value))))

(defn- as-timestamp [v]
  (doto (PGobject.)
    (.setType "timestamp with time zone")
    (.setValue v)))


(defn- document->row [topic message]
  [topic
   (-> message
       (update :sender (partial as-enum :t_sender))
       (update :timestamp as-timestamp)
       (update :doctype (partial as-enum :t_doctype))
       (update :id as-uuid)
       (update :body as-json))])


(defn- event-parcel->row [topic message]
  (if (= "ChangeState" (:msgtype message))
    [:change_state (-> message
                       (update :sender (partial as-enum :t_sender))
                       (update :timestamp as-timestamp)
                       (update :msgtype (partial as-enum :t_msgtype))
                       (update :id as-uuid)
                       (update :pentity as-uuid)
                       (update :dimensions as-json)
                       (update :parcel_source (partial as-enum :t_parcel_source))
                       (update :delivery_service (partial as-enum :t_delivery_service)))]
    [:order_return (-> message
                       (update :sender (partial as-enum :t_sender))
                       (update :timestamp as-timestamp)
                       (update :msgtype (partial as-enum :t_msgtype))
                       (update :id as-uuid)
                       (update :reason (partial as-enum :t_return_reason))
                       (update :items as-json)
                       (update :money_dest (partial as-enum :t_money_dest)))]))


(def converters {:document     document->row
                 :event_parcel event-parcel->row})


(defn process-message! [{:keys [pg upstream] :as context} topic message]
  (if-let [error (upstream/validate upstream topic message)]
    (throw (ex-info "Message not valid" error))
    (if-let [converter (topic converters)]
      (let [[table values]
            (converter topic message)]
        (pg/insert! pg table values))
      (throw (ex-info (str "Topic not found: " (name topic)) {:topic topic})))))


(defn test-message! [{:keys [pg] :as context} topic message]
  (jdbc/with-db-transaction [tx pg]
                            (swap! (:rollback tx) (constantly true))
                            (process-message! (assoc context :pg tx) topic message)))
