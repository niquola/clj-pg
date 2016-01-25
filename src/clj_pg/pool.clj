(ns clj-pg.pool
  (:import (com.zaxxer.hikari HikariConfig HikariDataSource)
           (java.util Properties))
  (:require [clojure.string :as str]))

(def defaults
  {:auto-commit        true
   :read-only          false
   :connection-timeout 30000
   :validation-timeout 5000
   :idle-timeout       600000
   :max-lifetime       1800000
   :minimum-idle       10
   :maximum-pool-size  10})


(defn upcase [s]
  (str
   (.toUpperCase (.substring s 0 1))
   (.substring s 1)))

(defn propertize [k]
  (let [parts (str/split (name k) #"-")]
    (str (first parts) (str/join "" (map upcase (rest parts))))))

(defn create-pool [db-name opts]
  (let [props (Properties.)]
    (.setProperty props "dataSourceClassName" "org.postgresql.ds.PGSimpleDataSource")
    (doseq [[k v] (merge defaults opts)]
      (.setProperty props (propertize k) (str v)))
    (-> props
        HikariConfig.
        HikariDataSource.)))

(defn close-pool [datasource] (.close datasource))
