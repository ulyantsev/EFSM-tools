#!/bin/bash
states=10
events=5
actions=5
inst=0
cd .. && ant qbf-automaton-generator-jar && cd qbf && java -jar ../jars/qbf-automaton-generator.jar testing/fsm-$states-$inst.sc --ltl testing/fsm-$states-$inst-true.ltl --size $states --eventNumber $events --actionNumber $actions --timeout 300 --result generated-fsm.gv --strategy ITERATIVE_SAT --complete
