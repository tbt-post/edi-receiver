(ns edi-receiver.api.handlers.topic
  (:require [edi-receiver.upstream :as upstream]))


(defn post [{{:keys [upstream pg]}   :context
             {{:keys [topic]} :path} :parameters
             message                 :body-params}]
  {:status 200
   :body   (upstream/validate upstream topic message)})
