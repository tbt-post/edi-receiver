(ns edi-receiver.api.handlers.topic
  (:require [mount.tools.graph :refer [states-with-deps]]
            [schema.core :as s]
            [edi-receiver.upstream :as upstream]))


(defn post
  {:summary    "Validates and saves message"
   :parameters {:path {(s/optional-key :topic) s/Keyword}}}
  [{{{:keys [topic]} :path} :parameters
    message                 :body-params}]
  {:status 200
   :body   (upstream/validate topic message)})
