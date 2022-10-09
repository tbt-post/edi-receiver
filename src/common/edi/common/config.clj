(ns edi.common.config
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [java-properties.core :as jconf]))

(defn create [{:keys [config] :as options}]
  (log/debug "Creating config" (or config ""))

  (-> (jconf/load-config "edi-receiver" options)
      (update-in [:upstream :topics] #(jconf/split-comma-separated %))
      (update :upstream #(merge % (select-keys options [:sync :topics])))
      (merge (select-keys options [:db]))
      (assoc :version (or (some-> "edi-receiver.VERSION"
                                  io/resource
                                  slurp
                                  string/trim)
                          "devel-current"))))

(defn get-topics [config]
  (-> config :upstream :topics))
