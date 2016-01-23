#!/bin/bash
states=3
cd .. && ant qbf-automaton-generator-jar && cd qbf && java -Xms2G -Xmx4G -jar ../jars/qbf-automaton-generator.jar case-instances/clock.sc --ltl case-instances/clock.ltl --size $states --eventNumber 4 --eventNames A,T,H,M --actionNumber 7 --actionNames z1,z2,z3,z4,z5,z6,z7 --varNumber 2 --timeout 200000 --result generated-fsm.gv --strategy COUNTEREXAMPLE --completenessType NORMAL --satSolver LINGELING
