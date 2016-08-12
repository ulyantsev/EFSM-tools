#!/bin/bash
states=3
java -Xmx4G -jar ../jars/qbf-automaton-generator.jar clock.sc --ltl clock.ltl --size $states --eventNames A,T,H,M --actionNames z1,z2,z3,z4,z5,z6,z7 --varNumber 2 --result generated-fsm.gv --strategy COUNTEREXAMPLE --completenessType NORMAL --satSolver LINGELING
