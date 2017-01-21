#!/bin/bash
for prefix in "correct_" ""; do
    java -jar ../../jars/apros-builder.jar --type prepare-dataset --traces ../plant-synthesis/traces-plant --tracePrefix "$prefix" --paramScales ../apros-configurations/scaling.txt --includeFirstElement
done
