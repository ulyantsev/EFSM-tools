#!/bin/bash
states=7
events=4
actions=4
inst=6
compl=incomplete
cd .. && ant qbf-automaton-generator-jar && cd qbf && java -ea -jar ../jars/qbf-automaton-generator.jar testing/$compl/fsm-$states-$inst.sc --ltl testing/$compl/fsm-$states-$inst-true.ltl --size $states --eventNumber $events --actionNumber $actions --timeout 300 --result generated-fsm.gv --strategy BACKTRACKING --completenessType NO_DEAD_ENDS

