(ns edi.receiver.backend.http
  (:require [cheshire.core :as json]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [edi.receiver.backend.protocol :as protocol]
            [edi.receiver.utils.jetty-client :as http])
  (:import (clojure.lang ExceptionInfo)))


(deftype HttpBackend [client request]

  protocol/Backend

  (send-message [_ topic message]
    (let [request (-> request
                      (update :uri #(string/replace % #"\{topic\}" topic))
                      (assoc :headers {"Content-Type" "application/json"
                                       "Accept"       "application/json"}
                             :body (json/encode message)
                             :throw-for-status true))]
      (log/debug "proxying message to " (:uri request))
      (try
        (http/request client request)
        (catch ExceptionInfo e
          (throw (ex-info "Can't send message to http backend"
                          (-> (select-keys request [:method :uri])
                              (assoc :response (ex-data e)))))))))

  (close [_]))


(defn create [config]
  (log/info "Initializing http client to" (:uri config))
  (HttpBackend. (http/client) config))
