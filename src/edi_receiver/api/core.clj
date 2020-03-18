(ns edi-receiver.api.core
  (:require [aleph.http :as http]
            [clojure.tools.logging :as log]
            [reitit.coercion.schema]
            [reitit.ring :as ring]
            [reitit.ring.coercion :as coercion]
            [reitit.ring.middleware.exception :as exception]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.not-modified :refer [wrap-not-modified]]
            [ring.middleware.params :as params]
            [ring.middleware.resource :refer [wrap-resource]]
            [muuntaja.core :as m]
            [edi-receiver.api.routes :as routes])
  (:import (java.sql SQLException)))


(defn- str-exception [exception _]
  {:status 500
   :body   (str exception)})


(defn with-context [handler context]
  (fn [request] (handler (assoc request :context context))))


(defn create-app [context]
  (log/debug "Creating api")
  (-> routes/routes
      (ring/router {:conflicts nil
                    :data      {:muuntaja   (m/create m/default-options)
                                :middleware [#(with-context % context)
                                             params/wrap-params
                                             (exception/create-exception-middleware
                                               (merge
                                                 exception/default-handlers
                                                 {SQLException str-exception}))
                                             muuntaja/format-middleware
                                             coercion/coerce-exceptions-middleware
                                             coercion/coerce-request-middleware
                                             coercion/coerce-response-middleware
                                             #_#(wrap-resource % "static")
                                             #_wrap-content-type
                                             #_wrap-not-modified]}})

      (ring/ring-handler
        (ring/routes
          (ring/create-default-handler)))))


(defn start-server [app {:keys [api-host api-port]}]
  (log/info "Started API on" api-host ":" api-port)
  (http/start-server app {:host api-host
                          :port api-port}))

(defn stop-server [server]
  (do
    (log/info "Stopping HTTP server")
    (.close server)))
