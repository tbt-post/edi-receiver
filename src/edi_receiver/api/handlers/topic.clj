(ns edi-receiver.api.handlers.topic
  (:require [clojure.tools.logging :as log]
            [edi-receiver.saver :as saver]
            [edi-receiver.utils :as utils]))


(defn post [{context                 :context
             {{:keys [topic]} :path} :parameters
             message                 :body-params}]
  (try
    {:status 200
     :body   {:rowcount (saver/process-message! context topic message)}}
    (catch Exception e
      (let [error (cond-> {:exception (.getName (class e))
                           :message   (ex-message e)}
                          (ex-data e) (assoc :data (ex-data e)))]
        (log/errorf "Error processing message:\n%s\nMessage caused error:\n%s"
                    (utils/pretty error)
                    (utils/pretty message))
        {:status 422
         :body   error}))))
