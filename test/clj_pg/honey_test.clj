(ns clj-pg.honey-test
  (:require [clj-pg.honey :as sut]
            [clj-pg.test-db :refer [db]]
            [clojure.test :as t]
            [clojure.test :refer [deftest is testing]]))


(def test_items
  {:table :test_items
   :columns {:id    {:type :serial :primary true :weighti 0}
             :tz    {:type :timestamptz}
             :label {:type :text :weight 1}}})

(def test_types_items
  {:table :test_types_items
   :columns {:id    {:type :serial :primary true :weighti 0}
             :jsonb_content {:type :jsonb}
             :varchar_array_content {:type "varchar" :array true}
             :int_array_content     {:type "integer" :array true}
             :date_array_content     {:type "timestamp" :array true}
             :text_array_content    {:type "text" :array true}}})

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
      (is (= 1 (:id item)))))

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
        (is (= (get test_types_items-item* k) v))))))
