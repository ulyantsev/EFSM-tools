#!/bin/bash
states=6
events=5
actions=5
inst=28
compl=complete
cd .. && ant qbf-automaton-generator-jar && cd qbf && java -jar ../jars/qbf-automaton-generator.jar testing/$compl/fsm-$states-$inst.sc --ltl testing/$compl/fsm-$states-$inst-true.ltl --size $states --eventNumber $events --actionNumber $actions --timeout 300 --result generated-fsm.gv --strategy ITERATIVE_SAT --complete
