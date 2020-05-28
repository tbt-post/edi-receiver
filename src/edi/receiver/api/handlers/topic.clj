(ns edi.receiver.api.handlers.topic
  (:require [clojure.tools.logging :as log]
            [edi.receiver.saver :as saver]
            [edi.common.utils :as utils])
  (:import (clojure.lang ExceptionInfo)))


(defn post [{context                 :context
             {{:keys [topic]} :path} :parameters
             message                 :body-params}]
  (try
    {:status 200
     :body   {:rowcount (saver/process-message! context topic message)}}
    (catch Exception e
      (let [error (if (instance? ExceptionInfo e)
                    {:message (ex-message e)
                     :data    (dissoc (ex-data e) :bad-request?)}
                    {:exception (.getName (class e))
                     :message   (ex-message e)})]
        (log/errorf "Error processing message:\n%s\nMessage caused error:\n%s"
                    (utils/pretty error)
                    (utils/pretty message))
        {:status (if (and (instance? ExceptionInfo e)
                          (:bad-request? (ex-data e)))
                   400
                   422)
         :body   error}))))
