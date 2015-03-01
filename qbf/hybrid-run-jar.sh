#!/bin/bash
states=6
events=4
actions=4
inst=28
compl=complete
cd .. && ant qbf-automaton-generator-jar && cd qbf && java -ea -Xms2G -jar ../jars/qbf-automaton-generator.jar testing/$compl/fsm-$states-$inst.sc --ltl testing/$compl/fsm-$states-$inst-true.ltl --size $states --eventNumber $events --actionNumber $actions --timeout 300 --result generated-fsm.gv --strategy HYBRID --complete
