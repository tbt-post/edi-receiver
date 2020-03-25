(ns edi-receiver.saver
  (:require [cheshire.core :as json]
            [clojure.java.jdbc :as jdbc]
            [edi-receiver.db.pg :as pg]
            [edi-receiver.upstream :as upstream])
  (:import (java.util UUID)
           (org.postgresql.util PGobject)))


(defn- as-uuid [s]
  (when s
    (UUID/fromString s)))

(defn- as-json [v]
  (doto (PGobject.)
    (.setType "json")
    (.setValue (json/generate-string v))))

(defn- as-enum
  "Convert a keyword value into an enum-compatible object."
  [enum-type value]
  (doto (PGobject.)
    (.setType (name enum-type))
    (.setValue value)))

(defn- as-timestamp [v]
  (doto (PGobject.)
    (.setType "timestamp with time zone")
    (.setValue v)))

(defn- as-decimal [v]
  (when v
    (bigdec (if (string? v) v (str v)))))


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


(defn- order_payment->row [topic message]
  [topic
   (-> message
       (update :sender (partial as-enum :t_sender))
       (update :timestamp as-timestamp)
       (update :id as-uuid)
       (update :parcel_id as-uuid)
       (update :contragent_id as-uuid)
       (update :posop (partial as-enum :t_posop))
       (update :amount as-decimal))])


(defn- refill_payment->row [topic message]
  [topic
   (-> message
       (update :sender (partial as-enum :t_sender))
       (update :timestamp as-timestamp)
       (update :id as-uuid)
       (update :dest (partial as-enum :t_money_dest))
       (update :posop (partial as-enum :t_posop))
       (update :amount as-decimal))])


(defn- wms_event->row [topic message]
  [topic
   (-> message
       (update :serial as-uuid)
       (update :items as-json)
       (update :condition (partial as-enum :t_wms_event_condition))
       (update :ev_type (partial as-enum :t_wms_event_type))
       (update :coordinates as-json)
       (update :flow (partial as-enum :t_flow))
       (update :direction (partial as-enum :t_direction))
       (update :origin as-uuid)
       (update :owner as-uuid))])

(defn- wms_item_announcement->row [topic message]
  [topic
   (-> message
       (update :serial as-uuid)
       (update :item as-json)
       (update :reference as-uuid)
       (update :origin as-uuid)
       (update :owner as-uuid))])


(defn- wms_registry_announcement->row [topic message]
  [topic
   (-> message
       (update :source (partial as-enum :t_sender))
       (update :timestamp as-timestamp)
       (update :uid as-uuid)
       (update :userial as-uuid)
       (update :state (partial as-enum :t_wms_registry_state)))])


(defn- wms_stocktaking_message->row [topic message]
  [topic
   (-> message
       (update :serial as-uuid)
       (update :reference as-uuid)
       (update :message_type (partial as-enum :t_wms_stocktaking_message_type))
       (update :item as-json)
       (update :origin as-uuid)
       (update :owner as-uuid))])


(def converters {:document                  document->row
                 :event_parcel              event-parcel->row
                 :order_payment             order_payment->row
                 :refill_payment            refill_payment->row
                 :wms_event                 wms_event->row
                 :wms_item_announcement     wms_item_announcement->row
                 :wms_registry_announcement wms_registry_announcement->row
                 :wms_stocktaking_message   wms_stocktaking_message->row})


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


(defn run-tests! [{:keys [upstream] :as context}]
  (upstream/run-tests! upstream (partial test-message! context)))
