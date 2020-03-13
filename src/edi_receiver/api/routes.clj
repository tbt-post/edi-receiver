(ns edi-receiver.api.routes
  (:require
    [edi-receiver.api.handlers.misc :as misc]
    [edi-receiver.api.handlers.topic :as topic]))


(def routes
  [["/api" {:coercion reitit.coercion.schema/coercion}
    ["" {:get #'misc/version}]
    ["/debug" {:get #'misc/dump-req}]
    ["/debug/:state" {:get #'misc/debug-state}]
    ["/topic/:topic" {:post #'topic/post}]]])
