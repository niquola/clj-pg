(ns clj-pg.database-url
  (:require [environ.core :refer [env]]))

(defn parse-url
  "Parse DATABASE_URL"
  [s]
  (let [uri (java.net.URI. s)
        db (.getPath uri)
        [user pass] (clojure.string/split (.getUserInfo uri) #":")]
    {:username       user
     :password       pass
     :server-name    (.getHost uri)
     :port-number    (or (.getPort uri) 5432)}))

(comment
  (parse-url (env :database-url)))
