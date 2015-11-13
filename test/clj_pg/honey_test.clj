(ns clj-pg.honey-test
  (:require [clj-pg.honey :as sut]
            [clj-pg.test-db :refer [db]]
            [clojure.test :as t]))

(comment
  (sut/execute db
               {:create-table :test_items
                :columns [[:id :serial :primary :key]
                          [:label :text]]})

  (sut/query db :select :* :from :test_items)
  (sut/query db "SELECT * FROM test_items")
  (sut/query db {:select [:*] :from [:test_items]})

  (sut/create db :test_items {:label "item-1"})

  (sut/create db :test_items [{:label "item-1"} {:label "item-2"}])

  (sut/update db :test_items {:id 1 :label "changed"})

  (sut/delete db :test_items 1))

