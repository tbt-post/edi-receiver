(ns edi-receiver.api.handlers.misc
  (:require [clj-helpers-common.core :as hc]
            [mount.tools.graph :refer [states-with-deps]]
            [edi-receiver.config :as config]
            [schema.core :as s]))


(defn dump-req
  {:summary "Dumps request"
   :tags    ["debug"]}
  [req]
  {:status 200
   :body   (-> req
               (dissoc :reitit.core/match
                       :reitit.core/router))})


(defn debug-state
  {:summary    "Lists mount states"
   :parameters {:path {(s/optional-key :state) s/Str}}
   :tags       ["debug"]}
  [{{{:keys [state]} :path} :parameters}]
  (let [item   (str "#'" (clojure.string/replace state #":" "/"))
        states (states-with-deps)]
    {:status 200
     :body   (or (hc/take-by-property :name item states)
                 states)}))


(defn version
  {:summary   "Version info"
   :responses {200 {:body {:version s/Str}}}}
  [_]
  {:status 200
   :body   {:version (config/version)}})
