(ns edi-receiver.db.models
  (:require [cheshire.core :as json]
            [clojure.string :as string]
            [edi-receiver.utils :as utils])
  (:import (java.util UUID)
           (org.postgresql.util PGobject)))


(def models
  {:documents                   (array-map :sender {:type :text :required true}
                                           :timestamp {:type :timestamp :required true :alias :ts}
                                           :doctype {:type :text :required true}
                                           :id {:type :uuid :required true}
                                           :body {:type :json :required true}
                                           :checksum {:type :text})

   :event_parcel_order_return   (array-map :sender {:type :text :required true}
                                           :timestamp {:type :timestamp :required true :alias :ts}
                                           :msgtype {:type :text :required true}
                                           :id {:type :uuid :required true}
                                           :order_id {:type :text :required true}
                                           :reason {:type :text :required true}
                                           :items {:type :json :required true}
                                           :total_price {:type :money :required true}
                                           :money_back {:type :boolean :required true}
                                           :money_dest {:type :text :required true}
                                           :document_id {:type :text})

   :event_parcel_change_state   (array-map :sender {:type :text :required true}
                                           :timestamp {:type :timestamp :required true :alias :ts}
                                           :msgtype {:type :text :required true}
                                           :id {:type :uuid :required true}
                                           :state {:type :text :required true}
                                           :pentity {:type :uuid}
                                           :weight {:type :integer}
                                           :dimensions {:type :json}
                                           :parcel_source {:type :text}
                                           :delivery_service {:type :text}
                                           :is_quasi {:type :boolean})

   :fms_contragent_announcement (array-map :sender {:type :text :required true}
                                           :timestamp {:type :timestamp :required true :alias :ts}
                                           :contragent_id {:type :uuid :required true}
                                           :contragent_reg_id {:type :text :required true}
                                           :action {:type :text :required true}
                                           :reason {:type :text :required true}
                                           :merchant_id {:type :text}
                                           :comment {:type :text})

   :fms_emoney_event            (array-map :sender {:type :text :required true}
                                           :timestamp {:type :timestamp :required true :alias :ts}
                                           :serial {:type :uuid :required true}
                                           :money_type {:type :text :required true}
                                           :account {:type :text :required true}
                                           :emoney_transaction {:type :text :required true}
                                           :transaction_id {:type :uuid :required true}
                                           :emoney_amount {:type :money :required true}
                                           :direction {:type :text :required true}
                                           :action {:type :text :required true}
                                           :reason {:type :text :required true}
                                           :contragent_id {:type :uuid}
                                           :contragent_reg_id {:type :text}
                                           :merchant_id {:type :text}
                                           :note {:type :text}
                                           :source_wallet {:type :text}
                                           :target_wallet {:type :text})

   :order_payment               (array-map :sender {:type :text :required true}
                                           :timestamp {:type :timestamp :required true :alias :ts},
                                           :id {:type :uuid :required true}
                                           :parcel_id {:type :uuid :required true}
                                           :contragent_id {:type :uuid :required true}
                                           :posop {:type :text :required true}
                                           :posorder_id {:type :text :required true}
                                           :merchant_id {:type :text :required true}
                                           :amount {:type :money :required true}
                                           :payer_phone {:type :text}
                                           :comment {:type :text})

   :refill_payment              (array-map :sender {:type :text :required true}
                                           :timestamp {:type :timestamp :required true :alias :ts},
                                           :id {:type :uuid :required true}
                                           :payer_phone {:type :text :required true}
                                           :user_id {:type :text}
                                           :dest {:type :text :required true}
                                           :posop {:type :text :required true}
                                           :posorder_id {:type :text :required true}
                                           :merchant_id {:type :text :required true}
                                           :amount {:type :money :required true}
                                           :comment {:type :text})

   :wms_event                   (array-map :serial {:type :uuid :required true}
                                           :items {:type :json :required true}
                                           :condition {:type :text :required true :alias :cond}
                                           :ev_type {:type :text}
                                           :ev_task {:type :text}
                                           :ev_stage {:type :text}
                                           :coordinates {:type :json}
                                           :flow {:type :text}
                                           :direction {:type :text}
                                           :reason {:type :text}
                                           :version {:type :text}
                                           :origin {:type :uuid}
                                           :owner {:type :uuid})

   :wms_item_announcement       (array-map :serial {:type :uuid :required true}
                                           :item {:type :json :required true}
                                           :zone_from {:type :text}
                                           :zone_to {:type :text}
                                           :reference {:type :uuid :required true}
                                           :version {:type :text}
                                           :origin {:type :uuid}
                                           :owner {:type :uuid})

   :wms_registry_announcement   (array-map :source {:type :text :required true}
                                           :timestamp {:type :timestamp :required true :alias :ts},
                                           :generated {:type :integer :required true :alias :gener}
                                           :serial {:type :integer :required true}
                                           :uid {:type :uuid :required true}
                                           :userial {:type :uuid :required true}
                                           :state {:type :text :required true})

   :wms_stocktaking_message     (array-map :serial {:type :uuid :required true}
                                           :reference {:type :uuid :required true}
                                           :message_type {:type :text :required true}
                                           :label {:type :text}
                                           :item {:type :json}
                                           :version {:type :text}
                                           :origin {:type :uuid}
                                           :owner {:type :uuid})})


(def topics
  {:event_parcel [{:table :event_parcel_order_return
                   :when? #(not= "ChangeState" (:msgtype %))}
                  {:table :event_parcel_change_state
                   :when? #(= "ChangeState" (:msgtype %))}]
   :document     [{:table :documents
                   :when? (constantly true)}]})


(def types
  {:postgresql {:boolean   {:sql "boolean"}
                :integer   {:sql "integer"}
                :json      {:sql "json"
                            :as  #(doto (PGobject.)
                                    (.setType "json")
                                    (.setValue (some-> % json/generate-string)))}
                :money     {:sql "numeric(10,2)"
                            :as  #(some-> % str bigdec)}
                :text      {:sql "text"}
                :timestamp {:sql "timestamp with time zone"
                            :as  #(doto (PGobject.)
                                    (.setType "timestamptz")
                                    (.setValue %))}
                :uuid      {:sql "uuid"
                            :as  #(some-> % UUID/fromString)}}
   :mysql      {:boolean   {:sql "boolean"}
                :integer   {:sql "integer"}
                :json      {:sql "json"
                            :as  #(some-> % json/generate-string)}
                :money     {:sql "decimal(10,2)"
                            :as  #(some-> % str bigdec)}
                :text      {:sql "text"}
                :timestamp {:sql "datetime"
                            :as  #(when %
                                    (utils/iso-datetime->java-util-date %))}
                :uuid      {:sql "binary(16)"
                            :as  #(some-> % UUID/fromString utils/uuid->byte-array)}}})


(defn- field-q [driver [field {:keys [type required alias]}]]
  (str "    " (name (or alias field))
       " " (-> types driver type :sql)
       (if required " NOT NULL" "")))


(defn- fields-q [driver table]
  (->> (table models)
       (map (partial field-q driver))
       (string/join ",\n")))


(defn- create-table-q [driver table]
  (format "DROP TABLE IF EXISTS %s; CREATE TABLE IF NOT EXISTS %s (\n%s);"
          (name table)
          (name table)
          (fields-q driver table)))


(defn- tables-meta [topic]
  (or (get topics topic)
      [{:table topic
        :when? (constantly true)}]))


(defn create-tables-q [driver topic]
  (->> (tables-meta topic)
       (map :table)
       (map (partial create-table-q driver))
       (string/join "\n")))


(defn- coerce-item [types model [key value]]
  (let [{:keys [type alias]} (get model key)
        as (-> (get types type)
               :as
               (or identity))]
    [(or alias key) (as value)]))


(defn- coerce-message [types model message]
  (->> message
       (map (partial coerce-item types model))
       (into {})))


(defn converter [driver topic message]
  (->> (for [{:keys [table when?]} (tables-meta topic)]
         (when (when? message)
           [table (->> message
                       (coerce-message (get types driver)
                                       (get models table)))]))
       (remove nil?)
       first))

