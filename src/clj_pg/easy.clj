(ns clj-pg.easy
  (:require
   [clojure.java.jdbc :as jdbc]
   [clj-pg.pool :as pool]
   [clj-pg.honey :as pghoney]
   [clj-pg.coerce :as coerce]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [environ.core :as env])
  (:refer-clojure :exclude [update]))


(def ^:dynamic *db* nil)

(def datasources (atom {}))

(defn shutdown-connections []
  (doseq [[nm {conn :datasource}] @datasources]
    (log/info "Closing connections for " nm)
    (pool/close-pool conn))
  (reset! datasources {}))

(defn shutdown-connections-for-db [db-name]
  (when-let [{conn :datasource} (get @datasources db-name)]
    (pool/close-pool conn)
    (swap! datasources dissoc db-name)))

(defn get-datasource [db-name ds-fn]
  (log/info "Get datasource for " db-name)
  (if-let [ds (get @datasources db-name)]
    ds
    (let [ds-opts  (ds-fn db-name)
          ds (pool/create-pool ds-opts)
          pool {:datasource ds}]
      (log/info "Building poll for " db-name " " ds-opts)
      (swap! datasources assoc db-name pool)
      pool)))

(defmacro with-db [db-name ds-fn & body]
  `(binding [*db* (get-datasource ~db-name ~ds-fn)] ~@body))

(defmacro with-db-spec [conn & body]
  `(binding [*db* ~conn] ~@body))

(defmacro transaction  [& body]
  `(jdbc/with-db-transaction  [t-db# *db*]
     (with-db-spec t-db# ~@body)))

(defmacro try-with-rollback  [& body]
  `(jdbc/with-db-transaction  [t-db# *db*]
     (jdbc/db-set-rollback-only! t-db#)
     (with-db-spec t-db# ~@body)
     (log/info "ROLLBACK")))

(defn query [& args] (apply pghoney/query *db* args))

(defn debug-query [sql] (pghoney/debug-query *db* sql))

(defn query-first [& args] (apply pghoney/query-first *db* args))

(defn query-value [& args] (apply pghoney/query-value *db* args))

(defn execute [& args] (apply pghoney/execute *db* args))

(defn create-table [spec]
  "expected spec in format
      {:table :test_items
       :columns {:id    {:type :serial :primary true :weighti 0}
                 :label {:type :text :weight 1}}}"
  (pghoney/create-table *db* spec))

(defn drop-table [& args]
  (apply pghoney/drop-table *db* args))

(defn create-database [db & [template]]
  (pghoney/create-database *db* db template))

(defn drop-database [db-name]
  (pghoney/drop-database *db* db-name))

(defn create [spec data]
  (pghoney/create *db* spec data))

(defn update [spec ent]
  (pghoney/update *db* spec ent))

(defn delete [spec id]
  (pghoney/delete *db* spec id))

(defn table-exists? [& args]
  (apply pghoney/table-exists? *db* args))

(defn database-exists? [& args]
  (apply pghoney/database-exists? *db* args))
