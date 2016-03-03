(ns clj-pg.easy-test
  (:require [clj-pg.easy :as sut]
            [clojure.test :refer [deftest is testing]]
            [environ.core :refer [env]]
            [clojure.test :as t]
            [clojure.string :as str]))

(sut/shutdown-connections)

(defn datasource-fn [db-name]
  {:idle-timeout       10000
   :minimum-idle       1
   :maximum-pool-size  3
   :connection-init-sql "select 1"
   :data-source.url    (if (= :default db-name)
                         (env :database-url)
                         (str/replace (env :database-url-template) #"DATABASE" (name db-name)))})

(def table-spec {:table :cljpg1
                 :columns {:id {:type :serial :primary true}
                           :title {:type :text}}})

(deftest test-easy-interface
  (testing "easy"

    (sut/with-db :default datasource-fn
      (sut/drop-database :cljpg1))

    (sut/with-db :default datasource-fn
      (sut/drop-database :cljpg2))

    (sut/with-db :default datasource-fn
      (sut/create-database :cljpg1))

    (sut/with-db :default datasource-fn
      (sut/create-database :cljpg2))

    (sut/with-db :cljpg1 datasource-fn
      (sut/create-table table-spec)
      (sut/create table-spec {:title "Hello"})
      (let [res (sut/query-first :select :* :from :cljpg1)]
        (= "Hello" (:title res)))

      (is (= 1 (sut/query-value :select 1))))

    (sut/with-db :cljpg2 datasource-fn
      (is (not (sut/table-exists? :cljpg1)))
      (is (= 1 (sut/query-value :select 1))))))
