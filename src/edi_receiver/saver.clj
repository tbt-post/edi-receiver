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

(defn- as-timestamp [v]
  (doto (PGobject.)
    (.setType "timestamptz")
    (.setValue v)))

(defn- as-decimal [v]
  (when v
    (bigdec (if (string? v) v (str v)))))


(defn- document->row [_ message]
  [:documents
   (-> message
       (update :timestamp as-timestamp)
       (update :id as-uuid)
       (update :body as-json))])


(defn- event_parcel->row [_ message]
  (if (= "ChangeState" (:msgtype message))
    [:event_parcel_change_state
     (-> message
         (update :timestamp as-timestamp)
         (update :id as-uuid)
         (update :pentity as-uuid)
         (update :dimensions as-json))]
    [:event_parcel_order_return
     (-> message
         (update :timestamp as-timestamp)
         (update :id as-uuid)
         (update :items as-json))]))


(defn- fms_contragent_announcement->row [topic message]
  [topic
   (-> message
       (update :timestamp as-timestamp)
       (update :contragent_id as-uuid))])


(defn- order_payment->row [topic message]
  [topic
   (-> message
       (update :timestamp as-timestamp)
       (update :id as-uuid)
       (update :parcel_id as-uuid)
       (update :contragent_id as-uuid)
       (update :amount as-decimal))])


(defn- refill_payment->row [topic message]
  [topic
   (-> message
       (update :timestamp as-timestamp)
       (update :id as-uuid)
       (update :amount as-decimal))])


(defn- wms_event->row [topic message]
  [topic
   (-> message
       (update :serial as-uuid)
       (update :items as-json)
       (update :coordinates as-json)
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
       (update :timestamp as-timestamp)
       (update :uid as-uuid)
       (update :userial as-uuid))])


(defn- wms_stocktaking_message->row [topic message]
  [topic
   (-> message
       (update :serial as-uuid)
       (update :reference as-uuid)
       (update :item as-json)
       (update :origin as-uuid)
       (update :owner as-uuid))])


(def ^:private converters
  {:document                    document->row
   :event_parcel                event_parcel->row
   :fms_contragent_announcement fms_contragent_announcement->row
   :order_payment               order_payment->row
   :refill_payment              refill_payment->row
   :wms_event                   wms_event->row
   :wms_item_announcement       wms_item_announcement->row
   :wms_registry_announcement   wms_registry_announcement->row
   :wms_stocktaking_message     wms_stocktaking_message->row})


(defn- common-converter [[table values]]
  [table (if (contains? values :timestamp)
           (-> values
               (assoc :ts (:timestamp values))
               (dissoc :timestamp))
           values)])


(defn process-message! [{:keys [pg upstream]} topic message]
  (if-let [error (upstream/validate upstream topic message)]
    (throw (ex-info "Message not valid" error))
    (if-let [converter (comp common-converter (topic converters))]
      (let [[table values]
            (converter topic message)]
        (pg/insert! pg table values))
      (throw (ex-info (str "Topic not found: " (name topic)) {:topic topic})))))


(defn- test-message! [{:keys [pg] :as context} topic message]
  (jdbc/with-db-transaction [tx pg]
                            (swap! (:rollback tx) (constantly true))
                            (process-message! (assoc context :pg tx) topic message)))


(defn run-tests! [{:keys [upstream] :as context}]
  (upstream/run-tests! upstream (partial test-message! context)))
