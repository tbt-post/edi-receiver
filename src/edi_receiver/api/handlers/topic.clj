(ns edi-receiver.api.handlers.topic
  (:require [edi-receiver.saver :as saver]))


(defn post [{context                 :context
             {{:keys [topic]} :path} :parameters
             message                 :body-params}]
  (try
    {:status 200
     :body   {:rowcount (saver/process-message! context topic message)}}
    (catch Exception e
      {:status 400
       :body   {:message (ex-message e)
                :data    (ex-data e)}})))
