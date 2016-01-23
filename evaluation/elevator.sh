#!/bin/bash
states=5
cd .. && ant qbf-automaton-generator-jar && cd qbf && java -Xms2G -Xmx4G -jar ../jars/qbf-automaton-generator.jar case-instances/elevator.sc --ltl case-instances/elevator.ltl --size $states --eventNumber 5 --eventNames e11,e12,e2,e3,e4 --actionNumber 3 --actionNames z1,z2,z3 --varNumber 0 --timeout 200000 --result generated-fsm.gv --strategy COUNTEREXAMPLE --completenessType NO_DEAD_ENDS --satSolver LINGELING 
