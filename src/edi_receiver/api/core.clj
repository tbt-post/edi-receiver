(ns edi-receiver.api.core
  (:require [clojure.tools.logging :as log]
            [io.pedestal.http :as server]
            [muuntaja.core :as m]
            [reitit.coercion.schema]
            [reitit.http :as http]
            [reitit.http.coercion :as coercion]
            [reitit.http.interceptors.exception :as exception]
            [reitit.http.interceptors.muuntaja :as muuntaja]
            [reitit.http.interceptors.parameters :as parameters]
            [reitit.pedestal :as pedestal]
            [reitit.ring :as ring]
            [edi-receiver.api.routes :as routes])
  (:import (java.sql SQLException)))


(defn- str-exception [exception _]
  {:status 500
   :body   (str exception)})


(defn- context-interceptor [context]
  {:name  ::parameters
   :enter (fn [ctx]
            (update ctx :request #(assoc % :context context)))})


(defn- create-router [context]
  (log/debug "Creating api")
  (pedestal/routing-interceptor
    (http/router
      routes/routes
      {;:exception pretty/exception
       :data {:coercion     reitit.coercion.schema/coercion
              :muuntaja     m/instance
              :interceptors [(context-interceptor context)
                             ;; query-params & form-params
                             (parameters/parameters-interceptor)
                             ;; content-negotiation
                             (muuntaja/format-negotiate-interceptor)
                             ;; encoding response body
                             (muuntaja/format-response-interceptor)
                             ;; exception handling
                             (exception/exception-interceptor
                               (merge
                                 exception/default-handlers
                                 {SQLException str-exception}))
                             ;; decoding request body
                             (muuntaja/format-request-interceptor)
                             ;; coercing exceptions
                             ;(coercion2/coerce-exceptions-interceptor)
                             ;; coercing response bodys
                             (coercion/coerce-response-interceptor)
                             ;; coercing request parameters
                             (coercion/coerce-request-interceptor)]}})
    ;; optional default ring handlers (if no routes have matched)
    (ring/routes (ring/create-default-handler))))


(defn start-server [host port context]
  (log/info "Starting HTTP server on" host ":" port)
  (let [pedestal (-> {::server/type   :jetty
                      ::server/host   host
                      ::server/port   port
                      ::server/join?  false
                      ;; no pedestal routes
                      ::server/routes []}
                     (server/default-interceptors)
                     ;; use the reitit router
                     (pedestal/replace-last-interceptor (create-router context))
                     (server/dev-interceptors)
                     (server/create-server))]
    (server/start pedestal)
    (log/info "Joined HTTP Server")
    pedestal))


(defn stop-server [pedestal]
  (log/info "Stopping HTTP server")
  (server/stop pedestal))
