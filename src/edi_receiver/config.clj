(ns edi-receiver.config
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [edi-receiver.utils :as utils]))


(declare group-config)


(defn- load-props [file]
  (with-open [^java.io.Reader reader (io/reader file)]
    (let [props (java.util.Properties.)]
      (.load props reader)
      (into {} (for [[k v] props]
                 [k (read-string v)])))))


(defn- deeper [d]
  (if (contains? d nil)
    (get d nil)
    (group-config d)))


(defn- strip-prefix [pairs]
  (->> pairs
       (utils/map-keys #(second (string/split % #"\." 2)))
       deeper))


(defn group-config [d]
  (->> d
       (group-by #(keyword (first (string/split (first %) #"\."))))
       (utils/map-vals strip-prefix)))


(defn create [{:keys [config] :as options}]
  (log/debug "Creating config" (or config ""))
  (-> (group-config
        (merge
          (-> "edi-receiver.properties" io/resource load-props)
          (when config
            (-> config io/file load-props))))
      (update-in [:upstream :topics] #(utils/split-comma-separated %))
      (update :upstream #(merge % (select-keys options [:sync :topics])))
      (merge (select-keys options [:autoinit-tables :db]))
      (assoc :version (or (some-> "edi_receiver.VERSION"
                                  io/resource
                                  slurp
                                  string/trim)
                          "devel-current"))))

