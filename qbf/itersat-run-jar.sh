#!/bin/bash
states=3
events=4
actions=4
inst=19
compl=complete
cd .. && ant qbf-automaton-generator-jar && cd qbf && java -jar -ea ../jars/qbf-automaton-generator.jar testing/$compl/fsm-$states-$inst.sc --ltl testing/$compl/fsm-$states-$inst-true.ltl --size $states --eventNumber $events --actionNumber $actions --timeout 300 --result generated-fsm.gv --strategy ITERATIVE_SAT --completenessType NORMAL --satSolver LINGELING 
