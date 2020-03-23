(ns edi-receiver.config
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.tools.logging :as log]))


(declare group-config)


(defn- load-props [file]
  (with-open [^java.io.Reader reader (io/reader file)]
    (let [props (java.util.Properties.)]
      (.load props reader)
      (into {} (for [[k v] props]
                 [k (read-string v)])))))


(defn- map-vals [f m]
  (into {} (for [[k v] m] [k (f v)])))


(defn- map-keys [f m]
  (into {} (for [[k v] m] [(f k) v])))


(defn- deeper [d]
  (or (get d nil)
      (group-config d)))


(defn- strip-prefix [pairs]
  (->> pairs
       (map-keys #(second (string/split % #"\." 2)))
       deeper))


(defn group-config [d]
  (->> d
       (group-by #(keyword (first (string/split (first %) #"\."))))
       (map-vals strip-prefix)))


(defn create [{:keys [config]}]
  (log/debug "Creating config" (io/resource "edi-receiver.properties"))
  (let [config (group-config
                 (merge
                   (-> "edi-receiver.properties" io/resource load-props
                       (assoc "version" (or (some-> "edi_receiver.VERSION"
                                                    io/resource
                                                    slurp
                                                    string/trim)
                                            "devel-current")))
                   (when config
                     (-> config string/trim io/file load-props))))]
    config))
