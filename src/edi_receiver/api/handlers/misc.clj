(ns edi-receiver.api.handlers.misc)


(defn dump-req [request]
  {:status 200
   :body   (-> request
               (dissoc :reitit.core/match
                       :reitit.core/router))})


(defn version [request]
  {:status 200
   :body   {:version (-> request :context :config :version)}})
