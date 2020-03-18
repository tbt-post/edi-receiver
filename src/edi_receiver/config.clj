(ns edi-receiver.config
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.tools.logging :as log]))


(defn- load-props [file]
  (with-open [^java.io.Reader reader (io/reader file)]
    (let [props (java.util.Properties.)]
      (.load props reader)
      (into {} (for [[k v] props] [(keyword k) (read-string v)])))))


(defn create-config [{:keys [config]}]
  (log/debug "Creating config")
  (merge
    (-> "edi-receiver.properties" io/resource load-props
        (assoc :version (or (some-> "edi_receiver.VERSION"
                                    io/resource
                                    slurp
                                    string/trim)
                            "devel-current")))
    (when config
      (-> config string/trim io/file load-props))))
