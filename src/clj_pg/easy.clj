(ns clj-pg.easy
  (:require
   [clojure.java.jdbc :as jdbc]
   [clj-pg.pool :as pool]
   [clj-pg.honey :as pghoney]
   [clj-pg.coerce :as coerce]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [environ.core :as env]))


(def ^:dynamic *db* nil)

(defonce datasources (atom {}))
(defonce datasource-fn (atom nil))

(defn shutdown-connections []
  (doseq [[nm {conn :datasource}] @datasources]
    (pool/close-pool conn))
  (reset! datasources {}))

(defn shutdown-connections-for-db [db-name]
  (when-let [{conn :datasource} (get @datasources db-name)]
    (pool/close-pool conn)
    (swap! datasources dissoc db-name)))

(defn get-datasource [db-name]
  (let [ds-fn @datasource-fn]
    (if (get @datasources db-name)
      (get @datasources db-name)
      (let [pool {:datasource (pool/create-pool (ds-fn db-name))}]
        (swap! datasources assoc db-name pool)
        pool))))

(defmacro with-db [db-name & body]
  `(binding [*db* (get-datasource ~db-name)] ~@body))

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

(defn create-database [& args]
  (apply pghoney/create-database *db* args))

(defn drop-database [& args]
  (apply pghoney/drop-database *db* args))

(defn create [& args]
  (apply pghoney/create *db* args))

(defn update [& args]
  (apply pghoney/update *db* args))

(defn delete [& args]
  (apply pghoney/delete *db* args))

(defn table-exists? [& args]
  (apply pghoney/table-exists? *db* args))
