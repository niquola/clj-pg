(defproject clj-pg "0.0.3"
  :description "Collection of helpers to work with pg from clj"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :repositories   [["clojars"  {:url "https://clojars.org/repo" :sign-releases false}]]

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [honeysql "0.8.1"]
                 [clj-time "0.12.0"]
                 [environ "1.1.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [com.zaxxer/HikariCP "2.5.1"]
                 [org.postgresql/postgresql "9.4.1208.jre7"]
                 [org.clojure/java.jdbc "0.6.1"]
                 [cheshire "5.6.3"]
                 [functionalbytes/mount-lite "0.9.8"]]

  :source-paths  ["src" "test"]

  :profiles {:dev {:plugins [[lein-ancient "0.6.10"]] 
                   :dependencies  [[ch.qos.logback/logback-classic "1.1.7"]]}})
