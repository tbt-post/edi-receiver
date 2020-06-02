#!/bin/bash
pg_ctlcluster 12 main start
java -jar target/edi-control-standalone.jar --db pg --deploy && \
java -jar target/edi-receiver-standalone.jar --db pg
