(ns edi-receiver.api.routes
  (:require [reitit.coercion.schema :as schema]
            [schema.core :as s]
            [edi-receiver.api.handlers.misc :as misc]
            [edi-receiver.api.handlers.topic :as topic]))


(def routes
  [["/api" {:coercion schema/coercion}
    ["" {:get       misc/version
         :summary   "Version info"
         :responses {200 {:body {:version s/Str}}}}]
    ["/debug" {:get     misc/dump-req
               :summary "Dumps request"}]
    ["/topic/:topic" {:post       topic/post
                      :summary    "Validates and saves message"
                      :parameters {:path {:topic s/Keyword}}}]]])
