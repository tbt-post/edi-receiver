#!/bin/bash
pg_ctlcluster 12 main start
java -jar `ls target/*-standalone.jar` --db pg --autoinit-tables --sync
