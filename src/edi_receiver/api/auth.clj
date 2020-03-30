(ns edi-receiver.api.auth
  (:require [clojure.string :as string])
  (:import (java.util Base64)))


(def re-basic-header (re-pattern "^Basic (.+)$"))


(defn- base64-decode [^String s]
  (String. (.decode (Base64/getDecoder) s)))


(defn- parse-header [{:keys [headers]}]
  (when-let [decoded (some->> (get headers "authorization")
                              (re-matches re-basic-header)
                              second
                              base64-decode)]
    (when-let [[username password] (string/split decoded #":" 2)]
      {:username username
       :password password})))


(defn- terminate [ctx response]
  (-> ctx
      (assoc :response response)
      (update :io.pedestal.interceptor.chain/terminators #(cons (constantly true) %))))


(defn basic-auth-interceptor [auth realm]
  {:name  ::auth
   :enter (fn [{:keys [request] :as ctx}]
            (if-let [credentials (parse-header request)]
              (if (not= credentials auth)
                (terminate ctx {:status 403
                                :body   "Permission denied"})
                ctx)
              (terminate ctx {:status  401
                              :headers {"WWW-Authenticate" (str "Basic realm=\"" realm "\"")}
                              :body    "Unauthorized"})))})
