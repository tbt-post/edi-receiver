(ns edi.control.deploy
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [edi.common.db.jdbc :as db]
            [edi.common.db.models :as models]
            [clojure.string :as string]
            [edi.common.util.core :as utils]
            [edi.common.config :as config]))


(def ^:private type-sql
  (-> {:common     {:boolean "boolean"
                    :integer "integer"
                    :text    "text"
                    :money   "decimal(10,2)"
                    :json    "json"}
       :postgresql {:timestamp "timestamp with time zone"
                    :uuid      "uuid"}
       :mysql      {:timestamp "datetime"
                    :uuid      "binary(16)"}}
      (utils/merge-common :common)))


(def ^:private column-sql
  (-> {:common     {:add    "ALTER TABLE %s ADD COLUMN %s;"
                    :drop   "ALTER TABLE %s DROP COLUMN %s;"
                    :rename "ALTER TABLE %s RENAME COLUMN %s TO %s;"}
       :postgresql {:alter-type-set-not-null
                    (str "UPDATE %1$s SET %2$s = %4$s WHERE %2$s IS NULL;\n"
                         "ALTER TABLE %1$s ALTER COLUMN %2$s SET DATA TYPE %3$s;\n"
                         "ALTER TABLE %1$s ALTER COLUMN %2$s SET NOT NULL;")
                    :alter-type-drop-not-null
                    (str "ALTER TABLE %1$s ALTER COLUMN %2$s SET DATA TYPE %3$s;\n"
                         "ALTER TABLE %1$s ALTER COLUMN %2$s DROP NOT NULL;")}
       :mysql      {:alter-type-drop-not-null
                    "ALTER TABLE %1$s MODIFY %2$s %3$s;"
                    :alter-type-set-not-null
                    (str "UPDATE %1$s SET %2$s = %4$s WHERE %2$s IS NULL;\n"
                         "ALTER TABLE %1$s MODIFY %2$s %3$s NOT NULL;")}}
      (utils/merge-common :common)))


(defn- type-q [driver type]
  (-> type-sql driver type))


(defn- field-q [driver [field {:keys [type required alias]}]]
  (str (name (or alias field))
       " " (type-q driver type)
       (if required " NOT NULL" "")))


(defn- default-q [driver default]
  (let [default-q (if (string? default)
                    default
                    (get default driver))]
    (when (nil? default-q)
      (throw (ex-info "must provide default value when setting NOT NULL" {})))
    default-q))


(defn- migration-qs [driver table op column {:keys [rename type required default]}]
  (let [column-sql (get column-sql driver)
        alter-type-set-not-null-q
                   #(format (:alter-type-set-not-null column-sql)
                            (name table)
                            (name column)
                            (type-q driver type)
                            (default-q driver default))]
    (case op
      :add-column [(format (:add column-sql) (name table) (field-q driver [column {:type type}]))
                   (when required
                     (alter-type-set-not-null-q))]
      :alter-column [(when (or type (some? required))
                       (when-not type
                         (throw (ex-info "required must be provided with type due to mysql limitation" {})))
                       (if required
                         (alter-type-set-not-null-q)
                         (format (:alter-type-drop-not-null column-sql)
                                 (name table)
                                 (name column)
                                 (type-q driver type))))
                     (when rename
                       (format (:rename column-sql) (name table) (name column) (name rename)))]
      :drop-column [(format (:drop column-sql) (name table) (name column))])))


(defn- insert-migration-q [table version]
  (format "INSERT INTO migrations (table_name, model_version) VALUES ('%s', %s);" (name table) version))


(defn- migrate-table-qs [driver table version]
  (-> (->> (get-in models/migrations [version table])
           (map #(apply (partial migration-qs driver table) %))
           (reduce concat))
      (concat [(insert-migration-q table version)])))


(defn- fields-q [driver table]
  (->> (table models/models)
       (map #(str "    " (field-q driver %)))
       (string/join ",\n")))


(defn- create-table-qs [driver table]
  [(format "CREATE TABLE %s (\n%s);"
           (name table)
           (fields-q driver table))
   (insert-migration-q table models/version)])


(defn- deploy-table-qs [driver table table-version]
  (if (nil? table-version)
    (create-table-qs driver table)
    (->> (range table-version models/version)
         (map #(migrate-table-qs driver table (inc %)))
         (reduce concat))))


(defn- deploy-q [db topics]
  (let [table-versions  (db/table-versions db)
        deploy-topic-qs (fn [topic]
                          (->> (models/tables-meta topic)
                               (map :table)
                               (map #(deploy-table-qs (:driver db) % (get table-versions %)))
                               (reduce concat)))]
    (->> topics
         (map #(deploy-topic-qs (keyword %)))
         (reduce concat)
         (remove nil?)
         (string/join "\n"))))


(defn- init-q [driver]
  (some->> (format "sql/init/%s.sql" (name driver)) io/resource slurp))


(defn print-deploy-sql [{:keys [db config]}]
  (some->> (init-q (:driver db))
           (db/run-script! db))
  (println (deploy-q db (-> config :upstream :topics))))


(defn deploy! [{:keys [db config]}]
  (log/info "Initializing database")
  (some->> (init-q (:driver db))
           (db/run-script! db))
  (db/run-script! db (deploy-q db (config/get-topics config)) true)
  (log/info "Database initialization succeeded"))
