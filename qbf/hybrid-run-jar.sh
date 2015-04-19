#!/bin/bash
states=6
events=4
actions=4
inst=10
compl=complete
if [[ $compl == "complete" ]]; then
    compstr="NORMAL"
else
    compstr="NO_DEAD_ENDS"
fi
cd .. && ant qbf-automaton-generator-jar && cd qbf && java -ea -Xms2G -jar ../jars/qbf-automaton-generator.jar testing/$compl/fsm-$states-$inst.sc --ltl testing/$compl/fsm-$states-$inst-false.ltl --size $states --eventNumber $events --actionNumber $actions --timeout 30000 --result generated-fsm.gv --strategy HYBRID --completenessType $compstr --hybridSecToGenerateFormula 15 --hybridSecToSolve 30 
