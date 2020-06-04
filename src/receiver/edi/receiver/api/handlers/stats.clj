(ns edi.receiver.api.handlers.stats
  (:require [edi.receiver.stats :as stats]))


(defn stats [{context :context}]
  {:status 200
   :body   (stats/get-stats (:stats context))})
