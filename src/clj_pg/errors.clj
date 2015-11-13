(ns clj-pg.errors
  (:require [clojure.java.jdbc :as jdbc]))

(defmacro pr-error [& body]
  `(try
     ~@body
     (catch java.sql.SQLException e#
       (if (.getNextException e#) ;; rethrow exception containing SQL error
         (let [msg# (.getMessage (.getNextException e#))]
           (throw (java.sql.SQLException.
                   (str (cs/replace (.getMessage e#)
                                    "Call getNextException to see the cause." """")
                        "\n" msg#))))
         (throw e#)))))
