#!/bin/bash
states=3
events=4
actions=4
inst=0
compl=incomplete
cd .. && ant qbf-automaton-generator-jar && cd qbf && java -jar ../jars/qbf-automaton-generator.jar testing/$compl/fsm-$states-$inst.sc --ltl testing/$compl/fsm-$states-$inst-true.ltl --size $states --eventNumber $events --actionNumber $actions --timeout 300000 -qs DEPQBF --result generated-fsm.gv --completenessType NO_DEAD_ENDS --strategy QSAT
