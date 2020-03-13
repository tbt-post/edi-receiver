(ns edi-receiver.api.handlers.topic
  (:require [clj-helpers-common.core :as hc]
            [mount.tools.graph :refer [states-with-deps]]
            [edi-receiver.config :as config]
            [schema.core :as s]
            [edi-receiver.upstream :as upstream]
            [clojure.pprint :refer [pprint]]))


(defn post
  {:summary    "Validates and saves message"
   :parameters {:path {(s/optional-key :topic) s/Keyword}}}
  [{{{:keys [topic]} :path} :parameters
    message :body-params}]
  {:status 200
   :body   (upstream/validate topic message)})
