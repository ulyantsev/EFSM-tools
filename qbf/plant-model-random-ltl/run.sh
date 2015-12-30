#!/bin/bash

timeout=3600
events=5
actions=5

wd=$(pwd)
cd ../.. && ant plant-automaton-generator-jar && cd "$wd"

fsm="generated-plant.gv"

size=30
instance=43

ev_name=evaluation/$size-$instance
name=plants/plant-$size-$instance
sc_name=$name.sc
ltl_name=$name.ltl
java -jar -Xmx4G ../../jars/plant-automaton-generator.jar "$sc_name" \
    --ltl "$ltl_name" --size $size --eventNumber $events --actionNumber $actions \
    --timeout $timeout --result "$fsm" --actionspec actionspec.actionspec 
