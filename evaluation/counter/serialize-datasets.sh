#!/bin/bash
for prefix in ""; do
    java -jar ../../jars/apros-builder.jar --type prepare-dataset --traces traces --tracePrefix "$prefix" --paramScales scaling.txt --includeFirstElement
done
