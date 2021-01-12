#!/bin/bash
pg_ctlcluster 13 main start
java -jar target/edi-control-standalone.jar --db pg --deploy && \
java -jar target/edi-receiver-standalone.jar --db pg
