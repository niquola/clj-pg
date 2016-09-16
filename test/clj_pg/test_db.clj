(ns clj-pg.test-db
  (:require  [clojure.test :as t]
             [clj-pg.pool :as poll]
             [environ.core :refer [env]]))


(defonce pool (atom nil))

(def database-url "jdbc:postgresql://localhost:5432/pgclj?user=root&password=root&stringtype=unspecified")
(def database-template-url "jdbc:postgresql://localhost:5432/DATABASE?user=root&password=root&stringtype=unspecified")

(defn db []
  (if-let [p @pool]
    p
    (reset! pool
            {:datasource (poll/create-pool {:idle-timeout       10000
                                            :minimum-idle       1
                                            :maximum-pool-size  3
                                            :connection-init-sql "select 1"
                                            :data-source.url  (or (env :database-url) database-url)})})))

(defn reset []
  (when-let [p @pool]
    (poll/close-pool (:datasource p))
    (reset! pool nil)))

