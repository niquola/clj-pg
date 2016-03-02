(ns clj-pg.test-db
  (:require  [clojure.test :as t]
             [clj-pg.pool :as poll]
             [environ.core :refer [env]]))


(def db-uri (or "jdbc:postgresql://localhost:5432/pgclj?user=aidbox&password=aidbox&stringtype=unspecified"
                (env :database-url) ))

(str db-uri)

(def db {:datasource (poll/create-pool {:idle-timeout       1000
                                        :minimum-idle       0
                                        :maximum-pool-size  2
                                        :connection-init-sql "select 1"
                                        :data-source.url  db-uri})})

