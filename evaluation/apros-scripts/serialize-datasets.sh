#!/bin/bash
#java -jar ../../jars/apros-builder.jar --type prepare-dataset --traces ../plant-synthesis/traces-plant --tracePrefix "recorded_" --paramScales ../apros-configurations/scaling.txt
#exit

for prefix in "correct_" ""; do
    /usr/bin/time -f "\t%U user,\t%S system,\t%e elapsed,\t%Mk maxresident" java -jar ../../jars/apros-builder.jar --type prepare-dataset --traces ../plant-synthesis/traces-plant --tracePrefix "$prefix" --paramScales ../apros-configurations/scaling.txt --includeFirstElement
done
