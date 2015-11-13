(defproject clj_pg "0.1.0-SNAPSHOT"
  :description "Collection of helpers to work with pg from clj"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [honeysql "0.6.2"]
                 [clj-time "0.11.0"]
                 [environ "1.0.0"]
                 [com.zaxxer/HikariCP "2.4.2"]
                 [org.postgresql/postgresql "9.4-1205-jdbc41"]
                 [org.clojure/java.jdbc "0.3.7"]
                 [cheshire "5.5.0"] ])
