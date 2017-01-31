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

(sqlf/register-clause! :inherits 3)

(defmethod sqlf/format-clause :inherits [[_ tbls] sql-map]
  (when tbls (str " INHERITS (" (str/join "," (map name tbls)) ")")))


(defmethod sqlf/format-clause :drop-table [[_ tbl-name] sql-map]
  (str "DROP TABLE " (when (:if-exists sql-map) " IF EXISTS ") (sqlf/to-sql tbl-name)))

(sqlf/register-clause! :drop-table 1)

;; UPSERT (from https://github.com/nilenso/honeysql-postgres/)

(sqlf/register-clause! :do-update-set 235)
(sqlf/register-clause! :do-update-set! 235)
(sqlf/register-clause! :do-nothing 235)
(sqlf/register-clause! :upsert 225)

(defmethod sqlf/format-clause :on-conflict-constraint [[_ k] _]
  (let [get-first #(if (sequential? %)
                     (first %)
                     %)]
    (str "ON CONFLICT ON CONSTRAINT " (-> k
                                          get-first
                                          sqlf/to-sql))))

(defmethod sqlf/format-clause :on-conflict [[_ ids] _]
  (let [comma-join-args #(if (nil? %)
                           ""
                           (->> %
                                (map sqlf/to-sql)
                                sqlf/comma-join
                                sqlf/paren-wrap))]
    (str "ON CONFLICT " (comma-join-args ids))))

(defmethod sqlf/format-clause :do-nothing [_ _]
  "DO NOTHING")

(defmethod sqlf/format-clause :do-update-set! [[_ values] _]
  (str "DO UPDATE SET " (sqlf/comma-join (for [[k v] values]
                                           (str (sqlf/to-sql k) " = " (sqlf/to-sql v))))))

(defmethod sqlf/format-clause :do-update-set [[_ values] _]
  (str "DO UPDATE SET "
       (sqlf/comma-join (map #(str (sqlf/to-sql %) " = EXCLUDED." (sqlf/to-sql %))
                             values))))

(defn- format-upsert-clause [upsert]
  (let [ks (keys upsert)]
    (map #(sqlf/format-clause % (find upsert %)) upsert)))

(defmethod sqlf/format-clause :upsert [[_ upsert] _]
  (sqlf/space-join (format-upsert-clause upsert)))

;; END UPSERT

(defmethod sqlf/fn-handler "ilike" [_ col qstr]
  (str (sqlf/to-sql col) " ilike " (sqlf/to-sql qstr)))

(doseq [op ["@@" "@>" "<@" "||" "&&" "->" "->>" "#>>" "#>" "?" "?|" "?&" "#-"]]
  (defmethod sqlf/fn-handler op [_ col qstr]
    (str (sqlf/to-sql col) " " op " " (sqlf/to-sql qstr))))

(defmethod sqlf/fn-handler "not-ilike" [_ col qstr]
  (str (sqlf/to-sql col) " not ilike " (sqlf/to-sql qstr)))

(defn- honetize [hsql]
  (cond (map? hsql) (sql/format hsql :quoting :ansi)
        (vector? hsql) (if (keyword? (first hsql)) (sql/format (apply sql/build hsql) :quoting :ansi) hsql)
        (string? hsql) [hsql]))

(defmacro from-start [start]
  `(Math/floor (/ (double (- (. java.lang.System nanoTime) ~start)) 1000000.0)))

(defn query
  "query honey SQL"
  ([db hsql]
   (pr-error
    (let [sql (honetize hsql)
          start (. java.lang.System nanoTime)]
      (log/debug hsql)
      (try
        (let [res (jdbc/query db sql)]
          (log/info (str "[" (from-start start) "ms]") sql)
          res)
        (catch Exception e
          (log/error (str "[" (from-start start) "ms]") sql)
          (throw e))))))
  ([db h & more]
   (query db (into [h] more))))

(defn debug-query [db sql]
  (let [start (. java.lang.System nanoTime)]
    (try
      (let [res (pr-error (jdbc/query db sql))
            duration (from-start start)]
        (log/info (str "[" duration " ms]") sql)
        {:result res
         :duration duration
         :status "success"
         :query sql})
      (catch Exception e
        {:error (.getMessage e)
         :duration (from-start start)
         :status "error"
         :query sql}))))

(defn query-first [db & hsql]
  (first
   (apply query db hsql)))

(defn query-value [db & hsql]
  (when-let [row (apply query-first db hsql)]
    (first (vals row))))

(defn with-connection [db f]
  (if-let [conn (jdbc/db-find-connection db)]
    (f conn)
    (with-open [conn (jdbc/get-connection db)]
      (f conn))))

(defn execute
  "execute honey SQL"
  [db hsql]
  (pr-error
   (let [sql (honetize hsql)
         start (. java.lang.System nanoTime)]
     (log/debug hsql)
     (try
       (let [res (jdbc/execute! db sql)]
         (log/info (str "[" (from-start start) "ms]") sql)
         res)
       (catch Exception e
         (log/error (str "[" (from-start start) "ms]") sql)
         (throw e))))))

(defn exec!
  "execute raw SQL without escape processing"
  [db sql]
  (pr-error
   (let [start (. java.lang.System nanoTime)]
     (try
       (with-connection db
         (fn [con]
           (let [stmt (.prepareStatement con sql)
                 _    (.setEscapeProcessing stmt false)
                 res  (.execute stmt)]
             (log/info (str "[" (from-start start) "ms]") " batch " (.substring sql 0 (min 100 (count sql))))
             res)))
       (catch Exception e
         (log/error (str "[" (from-start start) "ms]") " batch " (.substring sql 0 (min 100 (count sql))))
         (throw e))))))

(defn- coerce-entry [conn spec ent]
  (reduce (fn [acc [k v]]
            (assoc acc k (cond
                           (vector? v) (coerce/to-pg-array conn v (get-in spec [:columns k :type]))
                           (map? v)    (coerce/to-pg-json v)
                           :else v)))
          {} ent))

(defn- to-column [k props]
  (filterv identity [k (str (name (:type props)) (when (:array props) "[]"))
                     (when (:primary props) "PRIMARY KEY")
                     (when-let [d (:default props)] (str "DEFAULT " d))
                     (when (:not-null props) "NOT NULL")]))

(defn- to-columns [cols]
  (reduce (fn [acc [k props]]
            (conj acc (to-column k props)))
          [] (sort-by #(or (get-in % [1 :weight]) 100) cols)))

(defn create-table-hsql [spec]
  {:create-table (:table spec)
   :columns  (to-columns (:columns spec))
   :inherits (:inherits spec)})


(defn create-table [db spec]
  "expected spec in format
      {:table :test_items
       :columns {:id    {:type :serial :default "5" :primary true :weighti 0}
                 :label {:type :text :weight 1}}}"
  {:pre [(map? spec)]}
  (execute db (create-table-hsql spec)))

(defn drop-table [db spec & [opts]]
  {:pre [(map? spec)]}
  (let [stm (merge (or opts {}) {:drop-table (:table spec)})]
    (execute db stm)))

(defn create-database [db db-name & [template]]
  (let [sql (str "CREATE DATABASE " (name db-name) (when template (str " TEMPLATE = " template)))]
    (log/info sql)
    (jdbc/execute! db [sql] {:transaction? false})))

(defn drop-database [db db-name]
  (let [sql (str "DROP DATABASE IF EXISTS " (name db-name))]
    (log/info sql)
    (jdbc/execute! db [sql] {:transaction? false})))


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

(defn update [db {tbl :table pk :pk :as spec} data]
  (with-connection db
    (fn [conn]
      (let [pk (or pk :id)]
        (->> {:update tbl
              :set (coerce-entry conn spec (dissoc data :id))
              :where [:= pk (pk data)]
              :returning [:*]}
             (query-first {:connection conn}))))))

(defn delete [db {tbl :table :as spec} id]
  (->> {:delete-from tbl :where [:= :id id] :returning [:*]}
       (query-first db)))


(defn quailified-name [tbl]
  (let [[i1 i2] (str/split (name tbl) #"\." 2)
        tbl (if i2 i2 i1)
        sch (if i2 i1 "public")]
    [sch tbl]))


(defn table-exists? [db tbl]
  (let [tbl (if (map? tbl) (:table tbl) tbl)
        [sch tbl] (quailified-name tbl)]
    (= 1
       (->> {:select [1]
             :from [:information_schema.tables]
             :where [:and [:= :table_schema sch] [:= :table_name (name tbl)]]}
            (query-value db)))))


(defn database-exists? [db db-name]
  (->> {:select [true]
        :from [:pg_database]
        :where [:= :datname (name db-name)]}
       (query-value db)))
