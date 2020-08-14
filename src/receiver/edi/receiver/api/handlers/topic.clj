(ns edi.receiver.api.handlers.topic
  (:require [clojure.tools.logging :as log]
            [edi.common.util.core :as util]
            [edi.common.util.timer :as timer]
            [edi.receiver.saver :as saver]
            [edi.receiver.stats :as stats])
  (:import (clojure.lang ExceptionInfo)))


(defn post [{context                 :context
             {{:keys [topic]} :path} :parameters
             message                 :body-params
             headers                 :headers}]
  (try
    {:status 200
     :body   {:rowcount
              (let [stats      (:stats context)
                    started-at (timer/now)]
                (stats/before-activity stats started-at)
                (stats/before-request stats topic started-at)
                (let [rowcount (saver/process-message! context topic message)]
                  (stats/after-request stats started-at (or (some-> headers (get "content-length") Integer/parseInt) 0))
                  rowcount))}}
    (catch Exception e
      (let [error (if (instance? ExceptionInfo e)
                    {:message (ex-message e)
                     :data    (dissoc (ex-data e) :bad-request?)}
                    (do (.printStackTrace e)
                        {:exception (.getName (class e))
                         :message   (ex-message e)}))]
        (log/errorf "Error processing message:\n%s\nMessage caused error:\n%s"
                    (util/pretty error)
                    (util/pretty message))
        {:status (if (and (instance? ExceptionInfo e)
                          (:bad-request? (ex-data e)))
                   400
                   422)
         :body   error}))))
