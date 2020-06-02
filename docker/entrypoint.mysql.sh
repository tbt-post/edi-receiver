#!/bin/bash
mysqld &
while ! mysqladmin ping -h localhost -u root -pmysql; do
    echo "waiting mysqld to come up..."
    sleep 1
done
java -jar target/edi-control-standalone.jar --db mysql --deploy && \
java -jar target/edi-receiver-standalone.jar --db mysql
