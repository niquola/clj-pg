(ns clj-pg.coerce-test
  (:require [clj-pg.coerce :as sut]
            [clojure.java.jdbc :as jdbc]
            [clj-time.core :as time]
            [clj-pg.test-db :as tdb]
            [clojure.test :refer [deftest is testing]]))

(defn query
  ([s]
   (let [db (tdb/db)]
     (:result (first (jdbc/query db [(str "SELECT " s " as result")])))))
  ([s arg]
   (let [db (tdb/db)]
     (:result (first (jdbc/query db [(str "SELECT " s " as result") arg]))))))

(deftest test-coerce
  (testing "result coerce"
    (is (= (query "1") 1))
    (is (= (query "'string'") "string"))
    (is (= (query "'{\"a\":1}'::json") {:a 1}))
    (is (= (query "'{\"a\":1}'::jsonb") {:a 1}))
    (is (= (query "ARRAY[1,2,3]") [1 2 3]))
    (is (= (query "ARRAY['1','2','3']") ["1" "2" "3"]))

    (is (= (type (query "'2015-01-01'::timestamp"))
           org.joda.time.DateTime))

    (is (= (type (query "'2015-01-01'::timestamptz"))
           org.joda.time.DateTime))

    (is (= (type (query "?::timestamptz" (time/now)))
           org.joda.time.DateTime))

    (is (= (type (query "?::timestamptz" (java.util.Date.)))
           org.joda.time.DateTime))

    (is (= 1 (query "(?)->'a'" {:a 1})))

    (is (= "ups" (query "?::text" :ups)))

    (is (= "ups" (query "?::text" "ups")))

    (query "'(1,5)'::int8range")))

(deftest test-arrays
  (testing "arrays"
    (let [db (tdb/db)]
      (with-open [conn (jdbc/get-connection db)]
        (is (= (type (sut/to-pg-array conn [1 2 3]))
               org.postgresql.jdbc.PgArray))

        (is (= (type (sut/to-pg-array conn ["a" "b"] "varchar"))
               org.postgresql.jdbc.PgArray))))))
