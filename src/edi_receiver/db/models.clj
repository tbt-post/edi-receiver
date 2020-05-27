(ns edi-receiver.db.models)

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
                                           :comment {:type :text}
                                           :operation {:type :text}
                                           :correction_id {:type :uuid})

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


(def version 1)
(def migrations {1 {:order_payment [[:add-column :operation {:type :text}]
                                    [:add-column :correction_id {:type :uuid}]]}})


; migrations example
#_(def version 2)
#_(def migrations {1 {:documents [[:add-column :altertest {:type     :uuid
                                                           :required true
                                                           :default  {:postgresql "'00000000-0000-0000-0000-000000000000'"
                                                                      :mysql      "0x0"}}]
                                  [:alter-column :altertest {:rename   :alter_test
                                                             :type     :text
                                                             :required false}]
                                  [:alter-column :alter_test {:type     :text
                                                              :required true
                                                              :default  {:postgresql "'00000000-0000-0000-0000-000000000000'"
                                                                         :mysql      "0x0"}}]]}
                   2 {:documents [[:drop-column :alter_test nil]]}})


(def tbtapi-docs-refs {0 "edi#v0.1.0"
                       1 "edi#v0.1.1"})

(def tbtapi-docs-ref (tbtapi-docs-refs version))


(def topics
  {:event_parcel [{:table :event_parcel_order_return
                   :when? #(not= "ChangeState" (:msgtype %))}
                  {:table :event_parcel_change_state
                   :when? #(= "ChangeState" (:msgtype %))}]
   :document     [{:table :documents
                   :when? (constantly true)}]})


(defn tables-meta [topic]
  (or (get topics topic)
      [{:table topic
        :when? (constantly true)}]))
