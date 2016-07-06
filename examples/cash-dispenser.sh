#!/bin/bash
states=9
java -Xms2G -Xmx4G -jar ../jars/qbf-automaton-generator.jar cash-dispenser.sc --ltl cash-dispenser.ltl --size $states --eventNumber 14 --eventNames IC,EC,A,AE,AS,C,CR,CP,M,MR,MS,ME,MP,CNL --actionNumber 13 --actionNames z1,z2,z3,z4,z5,z6,z7,z8,z9,z10,z11,z12,z13 --varNumber 0 --result generated-fsm.gv --strategy COUNTEREXAMPLE --completenessType NO_DEAD_ENDS --satSolver LINGELING
