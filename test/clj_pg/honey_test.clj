(ns clj-pg.honey-test
  (:require [clj-pg.honey :as sut]
            [clj-pg.test-db :as tdb]
            [clojure.test :as t :refer [deftest is testing]]))

(tdb/reset)

(def test_items
  {:table :test_items
   :columns {:id    {:type :serial :primary true :weighti 0}
             :tz    {:type :timestamptz}
             :label {:type :text :weight 1}}})

(def test_types_items
  {:table :test_types_items
   :columns {:id    {:type :serial :primary true :weighti 0}
             :jsonb_content {:type :jsonb}
             :varchar_array_content  {:type "varchar" :array true}
             :int_array_content      {:type "integer" :array true}
             :date_array_content     {:type "timestamp" :array true}
             :text_array_content     {:type "text" :array true}}})

(def test_types_items-item
  {:jsonb_content {:a 1 :b 2}
   :varchar_array_content ["a" "b"]
   :int_array_content [1 2]
   :date_array_content [#inst"1970-01-01"]
   :text_array_content ["a" "b"]})

(def test_types_items-item*
  {:id 1
   :jsonb_content {:c 3}
   :varchar_array_content ["c"]
   :int_array_content [3]
   :date_array_content [#inst"1990-01-01"]
   :text_array_content ["c"]})

(deftest test-coerce
  (let [db (tdb/db)]
   (testing "CRUD"
     (sut/table-exists? db :test_items)

     (sut/drop-table db test_items {:if-exists true})

     (is (not (sut/table-exists? db :test_items)))

     (sut/create-table db test_items)

     (is (sut/table-exists? db :test_items))

     (is (empty (sut/query db :select :* :from :test_items)))

     (is (empty (sut/query db "SELECT * FROM test_items")))

     (is (empty (sut/query db {:select [:*] :from [:test_items]})))

     (let [item (sut/create db test_items {:label "item-1" :tz #inst"1966-01-01"})]
       (is (not (nil? (:id item)))))

     (let [items  (sut/create db test_items [{:label "item-1"}
                                             {:label "item-2"}])]
       (is (= 2 (count items))))

     (let [item (sut/update db test_items {:id 1 :label "changed"})]
       (is (= 1 (:id item))))

     (let [item (sut/delete db test_items 1)]
       (is (= 1 (:id item))))))

  (testing "quilified-name"

    (is (= ["public" "test_items"] (sut/quailified-name :test_items)))
    (is (= ["ups" "test_items"] (sut/quailified-name :ups.test_items)))
    (is (= ["ups" "mups.test_items"] (sut/quailified-name :ups.mups.test_items))))

  (let [db (tdb/db)]
    (testing "JSONB"
     (sut/drop-table db test_types_items {:if-exists true})

     (sut/create-table db test_types_items)

     (let [item (sut/create db test_types_items test_types_items-item)]
       (is (not (nil? item)))
       (doseq [[k v] (dissoc item :id)]
         (is (= (get test_types_items-item k) v))))

     (let [item (sut/query-first db {:select [:*] :from [:test_types_items] :limit 1})]
       (doseq [[k v] (dissoc item :id)]
         (is (= (get test_types_items-item k) v))))

     (let [item (sut/update db test_types_items test_types_items-item*)]
       (doseq [[k v] (dissoc item :id)]
         (is (= (get test_types_items-item* k) v))))

     (let [item (sut/query-first db {:select [:*] :from [:test_types_items] :limit 1})]
       (doseq [[k v] (dissoc item :id)]
         (is (= (get test_types_items-item* k) v)))))))

(def base-create-table-spec
  {:table :basecreatetablespec
   :columns {:id    {:type :serial :primary true :weight 0}}})

(def create-table-spec
  {:table :createtablespec
   :columns {:id    {:type :serial :primary true :weight 2}
             :tz    {:type :timestamptz :default "CURRENT_TIMESTAMP" :weight 200}
             :num {:type :integer :default 5 :weight 3}
             :label {:type :text :not-null true :weight 0}}
   :inherits [:basecreatetablespec]})


(deftest test-coerce
  (let [db (tdb/db)]
    (sut/drop-table db create-table-spec {:if-exists true})
    (sut/drop-table db base-create-table-spec {:if-exists true})

    (sut/create-table db base-create-table-spec)
    (sut/create-table db create-table-spec)

    (testing "defaults"
      (let [res (sut/create db create-table-spec {:label "item-1"})]
        (is (not (nil? (:tz res))))
        (is (= 5 (:num res)))))

    (testing "not-nil"
      (is (thrown? org.postgresql.util.PSQLException #".*violates not-null constraint.*"
                   (sut/create db create-table-spec {:label nil}))))

    (testing "inheritance"
      (let [res (sut/query db {:select [:*] :from [:basecreatetablespec]})]
        (is (not (empty? res)))))

    ))
