#!/bin/bash
states=7
java -Xmx4G -jar ../jars/qbf-automaton-generator.jar priority-queue.sc --ltl priority-queue.ltl --size $states --eventNames OFFER,POLL --actionNames z1,z2,z3,z4,z5,z6,z7,z8 --varNames x1,x2,x3,x4 --result generated-fsm.gv --strategy COUNTEREXAMPLE --completenessType NO_DEAD_ENDS --satSolver LINGELING 
