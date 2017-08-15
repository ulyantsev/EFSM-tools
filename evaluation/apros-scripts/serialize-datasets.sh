#!/bin/bash

format="\t%U user,\t%S system,\t%e elapsed,\t%Mk maxresident"

# no unmodified-controller traces, no first elements
/usr/bin/time -f "$format" java -jar ../../jars/apros-builder.jar --type prepare-dataset --traces ../plant-synthesis/traces-plant --tracePrefix "recorded_" --paramScales ../apros-configurations/scaling.txt

# with unmodified-controller traces, with first element
for prefix in "correct_" ""; do
    /usr/bin/time -f "$format" java -jar ../../jars/apros-builder.jar --type prepare-dataset --traces ../plant-synthesis/traces-plant --tracePrefix "$prefix" --paramScales ../apros-configurations/scaling.txt --includeFirstElement
done
