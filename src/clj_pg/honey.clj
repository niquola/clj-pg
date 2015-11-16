(ns clj-pg.honey
  (:refer-clojure :exclude [update])
  (:require [clj-pg.errors :refer [pr-error]]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as cs]
            [clojure.string :as str]
            [environ.core :refer [env]]
            [honeysql.core :as sql]
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

(sql/format {:create-table :users
             :columns [[:id :serial :primary-key]]})


(defmethod sqlf/format-clause :drop-table [[_ tbl-name] sql-map]
  (str "DROP TABLE " (when (:if-exists sql-map) " IF EXISTS ") (sqlf/to-sql tbl-name)))

(sqlf/register-clause! :drop-table 1)

(sql/format {:drop-table :users :if-exists true})


(defn- honetize [hsql]
  (cond (map? hsql) (sql/format hsql)
        (vector? hsql) (if (keyword? (first hsql)) (sql/format (apply sql/build hsql)) hsql)
        (string? hsql) [hsql]))

(defn query
  "query honey SQL"
  ([db hsql]
   (pr-error (->> (honetize hsql) (jdbc/query db))))
  ([db h & more]
   (query db (into [h] more))))

(defn execute
  "execute honey SQL"
  [db hsql]
  (pr-error (jdbc/execute! db (honetize hsql))))

(defn create [db tbl data]
  (println (vector? data))
  (let [values (if (vector? data) data [data])
        res (->> {:insert-into tbl :values values :returning [:*]}
                (query db))]
    (if (vector? data) res (first res))))

(defn update [db tbl data]
  (->> {:update tbl
       :set (dissoc data :id)
       :where [:= :id (:id data)]
       :returning [:*]}
       (query db)
       (first)))

(defn delete [db tbl id]
  (->> {:delete-from tbl :where [:= :id id] :returning [:*]}
       (query db)
       (first)))

#_(sql/format {:delete-from :x :where [:= :id 1] :returning [:*]})

(comment
  (def db "postgresql://aidbox:aidbox@localhost:5432/test")

  (query db :select :* :from :users :limit 1))
