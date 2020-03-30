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
            [reitit.dev.pretty :as pretty]
            [edi-receiver.api.auth :as auth]
            [edi-receiver.api.routes :as routes])
  (:import (java.sql SQLException)))


(defn- str-exception [exception _]
  {:status 500
   :body   (str exception)})


(defn- context-interceptor [context]
  {:name  ::context
   :enter (fn [ctx]
            (update ctx :request #(assoc % :context context)))})


(defn- create-router [context auth]
  (pedestal/routing-interceptor
    (http/router
      routes/routes
      {:exception pretty/exception
       :data      {:coercion     reitit.coercion.schema/coercion
                   :muuntaja     m/instance
                   :interceptors [(context-interceptor context)
                                  ;; query-params & form-params
                                  (parameters/parameters-interceptor)
                                  ;; content-negotiation
                                  (muuntaja/format-negotiate-interceptor)
                                  ;; encoding response body
                                  (muuntaja/format-response-interceptor)
                                  ;; exception handling
                                  #_<(exception/exception-interceptor
                                      (merge
                                        exception/default-handlers
                                        {SQLException str-exception}))
                                  ;; decoding request body
                                  (muuntaja/format-request-interceptor)
                                  ;; coercing exceptions
                                  ;(coercion/coerce-exceptions-interceptor)
                                  ;; coercing response bodys
                                  (coercion/coerce-response-interceptor)
                                  ;; coercing request parameters
                                  (coercion/coerce-request-interceptor)
                                  (when auth (auth/basic-auth-interceptor auth "EDI receiver"))]}})
    ;; optional default ring handlers (if no routes have matched)
    (ring/routes (ring/create-default-handler))))


(defn start [{:keys [host port auth]} context]
  (let [server (-> {::server/type   :jetty
                    ::server/host   host
                    ::server/port   port
                    ::server/join?  false
                    ;; no pedestal routes
                    ::server/routes []}
                   (server/default-interceptors)
                   ;; use the reitit router
                   (pedestal/replace-last-interceptor (create-router context auth))
                   #_(server/dev-interceptors)
                   (server/create-server))]
    (server/start server)
    (log/infof "Started HTTP server on %s:%s" host port)
    server))


(defn stop [server]
  (log/info "Stopping HTTP server")
  (server/stop server))
