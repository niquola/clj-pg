(ns clj-pg.test-db
  (:require  [clojure.test :as t]
             [clj-pg.pool :as poll]
             [environ.core :refer [env]]))


(defonce pool (atom nil))

(defn db []
  (if-let [p @pool]
    p
    (reset! pool
            {:datasource (poll/create-pool {:idle-timeout       10000
                                            :minimum-idle       1
                                            :maximum-pool-size  3
                                            :connection-init-sql "select 1"
                                            :data-source.url  (env :database-url)})})))

