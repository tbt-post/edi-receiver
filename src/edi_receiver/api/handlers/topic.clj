(ns edi-receiver.api.handlers.topic
  (:require [edi-receiver.upstream :refer [upstream-validate]]))


(defn post [{{:keys [upstream db]}   :context
             {{:keys [topic]} :path} :parameters
             message                 :body-params}]
  {:status 200
   :body   (upstream-validate upstream topic message)})
