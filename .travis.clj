language: clojure
lein: lein2
addons:
  postgresql: "9.4"
services:
  - postgresql
before_script:
  - psql -c "CREATE USER test WITH PASSWORD 'test'"
  - psql -c 'ALTER ROLE test WITH SUPERUSER'
  - psql -c 'CREATE DATABASE test_db;' -U postgres
script: export DATABASE_URL=postgres://test:test@localhost:5432/test_db && lein2 do clean, javac, test
jdk:
  - oraclejdk8
