(ns edi-receiver.config
  (:require [clojure.java.io :as io]
            [mount.core :as mount]
            [clojure.string :as string]))


(defn version [] (or (some-> "edi_receiver.VERSION"
                             io/resource
                             slurp
                             string/trim)
                     "devel-current"))


(defn- load-props [file]
  (with-open [^java.io.Reader reader (io/reader file)]
    (let [props (java.util.Properties.)]
      (.load props reader)
      (into {} (for [[k v] props] [(keyword k) (read-string v)])))))


(defn create [{:keys [config]}]
  (merge
    (-> ".properties" io/resource load-props)
    (when config
      (-> config string/trim io/file load-props))))


(mount/defstate config
  :start (create (mount/args)))

(defn properties []
  config)


(defn prop [k]
  (k config))