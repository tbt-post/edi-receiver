#!/bin/bash
wait-for-url() {
    echo "Testing $1"
    timeout -s TERM 45 bash -c \
    'while [[ "$(curl -s -o /dev/null -L -w ''%{http_code}'' ${0})" != "200" ]];\
    do echo "Waiting for ${0}" && sleep 2;\
    done' ${1}
    echo "OK!"
}

pg_ctlcluster 13 main start
export LOGLEVEL=info
java -jar target/edi-control-standalone.jar --db pg --deploy && \
java -jar target/edi-receiver-standalone.jar --db pg & export PID=$!
wait-for-url http://localhost:8000/api
java -jar target/edi-control-standalone.jar --fire fire-config.json
kill -SIGINT $PID
