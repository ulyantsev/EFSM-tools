#!/bin/bash
states=5
java -Xmx4G -jar ../jars/qbf-automaton-generator.jar smfail.sc --ltl smfail.ltl --size $states --eventNumber 10 --eventNames A,B,C,D,E,F,G,H,I,J --actionNumber 0 --varNumber 0 --result generated-fsm.gv --strategy COUNTEREXAMPLE --completenessType NO_DEAD_ENDS --satSolver LINGELING
