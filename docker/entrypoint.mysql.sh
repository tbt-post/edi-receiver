#!/bin/bash
mysqld &
echo "Check DB!"
while ! mysqladmin ping -h localhost -u root -pmysql; do
    echo "Wait ..."
    sleep 1
done
echo "DB ready!"
java -jar `ls target/*-standalone.jar` --db mysql --autoinit-tables --sync
