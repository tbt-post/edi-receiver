(ns edi-receiver.api.core
  (:require [aleph.http :as http]
            [clojure.pprint :refer [pprint]]
            [clojure.tools.logging :as log]
            [mount.core :as mount]
            [reitit.coercion.schema]
            [reitit.ring :as ring]
            [reitit.ring.coercion :as coercion]
            [reitit.ring.middleware.exception :as exception]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.swagger :as swagger]
            [ring.logger :refer [wrap-with-logger]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.not-modified :refer [wrap-not-modified]]
            [ring.middleware.params :as params]
            [ring.middleware.resource :refer [wrap-resource]]
            [edi-receiver.api.routes :as routes]
            [edi-receiver.config :as config]
            [muuntaja.core :as m])
  (:import (java.sql SQLException)))


(def http-method? #{:get :post :put :delete :options :patch})


(defn- expand-route [[url conf & routes]]
  (letfn [(expand-handler [handler]
            (if (map? handler)
              handler
              (-> {:handler (var-get handler)}
                  (merge (dissoc (meta handler) :ns :name :file :column :line :arglists)))))]
    (vec (concat [url
                  (->> conf
                       (map (fn [[k v]]
                              [k
                               (if (http-method? k)
                                 (expand-handler v)
                                 v)]))
                       (into {}))]
                 (map expand-route routes)))))


(defn- str-exception [exception _]
  {:status 500
   :body   (str exception)})


(def app
  (-> (->> routes/routes
           (mapv expand-route))
      (ring/router {:conflicts
                          nil
                    :data {:muuntaja   (m/create m/default-options)
                           :middleware [swagger/swagger-feature
                                        params/wrap-params
                                        (exception/create-exception-middleware
                                          (merge
                                            exception/default-handlers
                                            {SQLException str-exception}))
                                        muuntaja/format-middleware
                                        coercion/coerce-exceptions-middleware
                                        coercion/coerce-request-middleware
                                        coercion/coerce-response-middleware
                                        ;#(wrap-resource % "static")
                                        ;wrap-content-type
                                        ;wrap-not-modified
                                        #_(when (contains? #{:dev :local} (config/profile))
                                            #(wrap-with-logger % {:printer :no-color}))]}})
      (ring/ring-handler
        (ring/routes
          (ring/create-default-handler)))))


(mount/defstate api
  :start (let [host (config/prop :api.host)
               port (config/prop :api.port)]
           (log/info "Started API on" host ":" port)
           (http/start-server app {:host host :port port}))
  :stop (do
          (log/info "Stopping API.")
          (.close api)))
