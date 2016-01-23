#!/bin/bash
states=5
cd .. && ant qbf-automaton-generator-jar && cd qbf && java -ea -Xms2G -Xmx4G -jar ../jars/qbf-automaton-generator.jar walkinshaw/smfail.sc --ltl walkinshaw/smfail.ltl --size $states --eventNumber 10 --eventNames A,B,C,D,E,F,G,H,I,J --actionNumber 0 --varNumber 0 --timeout 2000 --result generated-fsm.gv --strategy COUNTEREXAMPLE --completenessType NO_DEAD_ENDS --satSolver LINGELING
