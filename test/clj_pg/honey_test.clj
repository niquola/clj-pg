(ns clj-pg.honey-test
  (:require [clj-pg.honey :as sut]
            [clj-pg.test-db :refer [db]]
            [clojure.test :as t]
            [clojure.test :refer [deftest is testing]]))

(deftest test-coerce

  (testing "CRUD"

    (sut/execute db {:drop-table :test_items :if-exists true})

    (sut/execute db {:create-table :test_items :columns [[:id :serial :primary :key] [:label :text]]})

    (is (empty (sut/query db :select :* :from :test_items)))

    (is (empty (sut/query db "SELECT * FROM test_items")))

    (is (empty (sut/query db {:select [:*] :from [:test_items]})))

    (let [item (sut/create db :test_items {:label "item-1"})]
      (is (not (nil? (:id item)))))

    (let [items  (sut/create db :test_items [{:label "item-1"} {:label "item-2"}])]
      (is (= 2 (count items))))

    (let [item (sut/update db :test_items {:id 1 :label "changed"})]
      (is (= 1 (:id item))))

    (let [item (sut/delete db :test_items 1)]
      (is (= 1 (:id item))))))
