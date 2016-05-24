(ns clj-pg.errors
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log])
  (:import org.postgresql.util.PSQLException
           java.sql.BatchUpdateException))

(defmacro pr-error [& body]
  `(try
     ~@body
     (catch java.sql.BatchUpdateException e#
       (if-let [ne# (.getNextException e#)] ;; rethrow exception containing SQL error
         (let [msg# (.getMessage ne#)]
           (log/error ne#)
           (throw (java.sql.SQLException. msg#)))
         (do
           (log/error e#)
           (throw e#))))
     (catch org.postgresql.util.PSQLException e#
       (if-let [ne# (.getNextException e#)] ;; rethrow exception containing SQL error
         (let [msg# (.getMessage ne#)]
           (log/error ne#)
           (throw (java.sql.SQLException. msg#)))
         (do
           (log/error e#)
           (throw e#))))))
