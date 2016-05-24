(defproject clj_pg "0.1.0-SNAPSHOT"
  :description "Collection of helpers to work with pg from clj"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [honeysql "0.6.2"]
                 [clj-time "0.11.0"]
                 [environ "1.0.1"]
                 [org.clojure/tools.logging "0.3.1"]
                 [com.zaxxer/HikariCP "2.4.3"]
                 [org.postgresql/postgresql "9.4.1208.jre7"]
                 [org.clojure/java.jdbc "0.5.0"]
                 [cheshire "5.5.0"]]

  :source-paths  ["src" "test"]

  :profiles {:dev {:dependencies  [[ch.qos.logback/logback-classic "1.1.3"]]}})
