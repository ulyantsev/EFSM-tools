#!/bin/bash
states=5
java -Xmx4G -jar ../jars/qbf-automaton-generator.jar elevator.sc --ltl elevator.ltl --size $states --eventNames e11,e12,e2,e3,e4 --actionNames z1,z2,z3 --result generated-fsm.gv --strategy COUNTEREXAMPLE --completenessType NO_DEAD_ENDS --satSolver LINGELING 
