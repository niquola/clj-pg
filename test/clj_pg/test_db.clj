(ns clj-pg.test-db
  (:require  [clojure.test :as t]
             [clj-pg.pool :as poll]
             [environ.core :refer [env]]))


(def db-uri (or (env :database-url)
                "jdbc:postgresql://localhost:5432/pgclj?user=root&password=root&stringtype=unspecified"))

(def db {:datasource (poll/create-pool {:idle-timeout       10000
                                        :minimum-idle       1
                                        :maximum-pool-size  3 
                                        :connection-init-sql "select 1"
                                        :data-source.url  db-uri})})

(comment
  (poll/close-pool (:datasource db)))
