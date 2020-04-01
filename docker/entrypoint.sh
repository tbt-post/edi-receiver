#!/bin/bash
pg_ctlcluster 12 main start
java -jar `ls target/*-standalone.jar` --autoinit-tables --sync
