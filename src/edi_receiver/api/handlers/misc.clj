(ns edi-receiver.api.handlers.misc
  (:require [edi-receiver.db.pg :as pg]))


(defn dump-req [request]
  {:status 200
   :body   (-> request
               (dissoc :reitit.core/match
                       :reitit.core/router))})


(defn version [{{:keys [config pg]} :context}]
  {:status 200
   :body   {:version    (:version config)
            :pg-version (:version (first (pg/query pg "SELECT version()")))}})
