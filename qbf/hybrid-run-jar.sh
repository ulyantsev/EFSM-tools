#!/bin/bash
states=3
events=4
actions=4
inst=14
compl=complete
cd .. && ant qbf-automaton-generator-jar && cd qbf && java -ea -Xms2G -jar ../jars/qbf-automaton-generator.jar testing/$compl/fsm-$states-$inst.sc --ltl testing/$compl/fsm-$states-$inst-true.ltl --size $states --eventNumber $events --actionNumber $actions --timeout 30000 --result generated-fsm.gv --strategy HYBRID --complete --hybridSecToGenerateFormula 15 --hybridSecToSolve 30
