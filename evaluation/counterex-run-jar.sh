#!/bin/bash
states=6
events=4
actions=4
inst=35
compl=complete
cd .. && ant qbf-automaton-generator-jar && cd qbf && java -jar -ea ../jars/qbf-automaton-generator.jar testing/$compl/fsm-$states-$inst.sc --ltl testing/$compl/fsm-$states-$inst-true.ltl --size $(($states - 2)) --eventNumber $events --actionNumber $actions --timeout 3000 --result generated-fsm.gv --strategy COUNTEREXAMPLE --completenessType NORMAL --satSolver LINGELING #--noCompletenessHeuristics 
