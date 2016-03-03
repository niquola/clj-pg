(ns clj-pg.honey
  (:refer-clojure :exclude [update])
  (:require [clj-pg.errors :refer [pr-error]]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as cs]
            [clj-pg.coerce :as coerce]
            [clojure.string :as str]
            [honeysql.core :as sql]
            [clojure.tools.logging :as log]
            [honeysql.format :as sqlf]
            [honeysql.helpers :as sqlh]))

(sqlf/register-clause! :returning 230)

(defmethod sqlf/format-clause :returning [[_ fields] sql-map]
  (str "RETURNING "
       (when (:modifiers sql-map)
         (str (sqlf/space-join (map (comp clojure.string/upper-case name)
                               (:modifiers sql-map)))
              " "))
       (sqlf/comma-join (map sqlf/to-sql fields))))

(sqlf/register-clause! :create-table 1)

(defmethod sqlf/format-clause :create-table [[_ tbl-name] sql-map]
  (str "CREATE TABLE " (sqlf/to-sql tbl-name)))

(sqlf/register-clause! :columns 2)

(defmethod sqlf/format-clause :columns [[_ cols] sql-map]
  (str "("
       (str/join ", " (map #(str/join " " (map name %)) cols))
   ")"))


(defmethod sqlf/format-clause :drop-table [[_ tbl-name] sql-map]
  (str "DROP TABLE " (when (:if-exists sql-map) " IF EXISTS ") (sqlf/to-sql tbl-name)))

(sqlf/register-clause! :drop-table 1)

(defmethod sqlf/fn-handler "ilike" [_ col qstr]
  (str (sqlf/to-sql col) " ilike " (sqlf/to-sql qstr)))

(defmethod sqlf/fn-handler "not-ilike" [_ col qstr]
  (str (sqlf/to-sql col) " not ilike " (sqlf/to-sql qstr)))

(defn- honetize [hsql]
  (cond (map? hsql) (sql/format hsql)
        (vector? hsql) (if (keyword? (first hsql)) (sql/format (apply sql/build hsql)) hsql)
        (string? hsql) [hsql]))

(defn query
  "query honey SQL"
  ([db hsql]
   (pr-error
    (let [sql (honetize hsql)]
      (log/debug hsql)
      (log/info sql)
      (jdbc/query db sql))))
  ([db h & more]
   (query db (into [h] more))))

(defn query-first [db & hsql]
  (first
   (apply query db hsql)))

(defn query-value [db & hsql]
  (when-let [row (apply query-first db hsql)]
    (first (vals row))))

(defn execute
  "execute honey SQL"
  [db hsql]
  (let [sql (honetize hsql)]
    (log/debug hsql)
    (log/info sql)
    (pr-error (jdbc/execute! db sql))))

(defn- coerce-entry [conn spec ent]
  (reduce (fn [acc [k v]]
            (assoc acc k (cond
                           (vector? v) (coerce/to-pg-array conn v (get-in spec [:columns k :type]))
                           (map? v) (coerce/to-pg-json v)
                           :else v))
            ) {} ent))

(defn- to-column [k props]
  (filterv identity [k (str (name (:type props)) (when (:array props) "[]"))
                     (when (:primary props) "PRIMARY KEY")]))

(defn- to-columns [cols]
  (reduce (fn [acc [k props]]
            (conj acc (to-column k props))
            ) [] cols))

(defn create-table [db spec]
  "expected spec in format
      {:table :test_items
       :columns {:id    {:type :serial :primary true :weighti 0}
                 :label {:type :text :weight 1}}}"
  {:pre [(map? spec)]}
  (let [stm {:create-table (:table spec)
             :columns  (to-columns (:columns spec))}]
    (execute db stm)))

(defn drop-table [db spec & [opts]]
  {:pre [(map? spec)]}
  (let [stm (merge (or opts {}) {:drop-table (:table spec)})]
    (execute db stm)))

(defn create-database [db db-name & [template]]
  (let [sql (str "CREATE DATABASE " (name db-name) (when template " TEMPLATE = " template))]
    (log/info sql)
    (jdbc/execute! db [sql] :transaction? false)))

(defn drop-database [db db-name]
  (let [sql (str "DROP DATABASE IF EXISTS " (name db-name))]
    (log/info sql)
    (jdbc/execute! db [sql] :transaction? false)))

(defn with-connection [db f]
  (if-let [conn (jdbc/db-find-connection db)]
    (f conn)
    (with-open [conn (jdbc/get-connection db)]
      (f conn))))

(defn create [db {tbl :table :as spec} data]
  (with-connection db
    (fn [conn]
      (let [values (if (vector? data) data [data])
            values (map #(coerce-entry conn spec %) values)
            res (->> {:insert-into tbl
                      :values values
                      :returning [:*]}
                     (query {:connection conn}))]
        (if (vector? data) res (first res))))))

(defn update [db {tbl :table :as spec} data]
  (with-connection db
    (fn [conn]
      (->> {:update tbl
            :set (coerce-entry conn spec (dissoc data :id))
            :where [:= :id (:id data)]
            :returning [:*]}
           (query-first {:connection conn})))))

(defn delete [db {tbl :table :as spec} id]
  (->> {:delete-from tbl :where [:= :id id] :returning [:*]}
       (query-first db)))

(defn table-exists? [db tbl]
  (let [[sch tbl] (str/split (name tbl) #"\." 2)
        tbl (or tbl sch)
        sch (if tbl sch "public")]
    (= 1
       (->> {:select [1]
             :from [:information_schema.tables]
             :where [:and [:= :table_schema sch] [:= :table_name (name tbl)]]}
            (query-value db)))))
