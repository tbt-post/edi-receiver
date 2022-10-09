(ns edi.common.db.jdbc
  (:require [clojure.java.data :as java-data]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [java-properties.core :as jconf])
  (:import (com.mchange.v2.c3p0 ComboPooledDataSource)))


(defn connect [{:keys [db jdbc] :as config}]
  (let [props (merge jdbc ((keyword db) config))
        {:keys [driver host port database query-string]} props
        props (-> props
                  (dissoc :driver :host :port :database :query-string)
                  (assoc :jdbc-url (format "jdbc:%s://%s:%s/%s%s" driver host port database query-string))
                  jconf/kebab-conf-to-camelcase)]
    (log/infof "Creating %s pool" driver)
    (System/setProperty "user.timezone" "UTC")
    {:driver     (keyword driver)
     :datasource (java-data/to-java ComboPooledDataSource props)}))


(defn close [pool]
  (log/infof "Destroing %s pool" (-> pool :driver name))
  (.close (:datasource pool)))


(defn execute! [pool query]
  (first (jdbc/execute! pool query)))


(def query jdbc/query)


(def one (comp first query))


(defn- insert-q [table fields]
  (str "INSERT INTO " (name table) " ("
       (string/join "," (map name fields))
       ") VALUES ("
       (string/join "," (repeat (count fields) "?"))
       ")"))


(defn insert! [pool table values]
  (let [fields (keys values)]
    (execute! pool (cons (insert-q table fields)
                         (for [field fields] (field values))))))


(defn run-script!
  ([pool sql] (run-script! pool sql nil))
  ([pool sql verbose]
   (jdbc/with-db-transaction
     [tr pool]
     (->> (-> sql
              (string/replace #"(?s)/\*.*\*/|--[^\n]*" "")
              (string/split #";"))
          (map string/trim)
          (remove #(= "" %))
          (map #(do
                  (when verbose
                    (log/info "Run SQL:" %))
                  (execute! tr %)))
          (doall)))))


(defn table-versions [pool]
  (->> (jdbc/query pool "SELECT table_name, MAX(model_version) AS model_version FROM migrations GROUP BY table_name")
       (map (juxt (comp keyword :table_name) :model_version))
       (into {})))


(defn db-version [pool]
  (->> (jdbc/query pool "SELECT version()") first :version))
