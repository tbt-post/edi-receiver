(ns edi-receiver.api.handlers.topic
  (:require [edi-receiver.upstream :as upstream]
            [edi-receiver.db.pg :as pg]
            [cheshire.core :as json])
  (:import (java.util UUID)
           (org.postgresql.util PGobject)))




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


(defn- error [message]
  {:status 400 :body {:message message}})

(defn post [{{:keys [upstream pg]}   :context
             {{:keys [topic]} :path} :parameters
             message                 :body-params}]
  (if-let [error (upstream/validate upstream topic message)]
    {:status 400 :body error}
    (if-let [converter (topic converters)]
      {:status 200
       :body   {:rowcount (let [[table values]
                                (converter topic message)]
                            (pg/insert! pg table values))}}
      (error (str "Topic not found: " (name topic))))))
