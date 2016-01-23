#!/bin/bash
states=7
cd .. && ant qbf-automaton-generator-jar && cd qbf && java -ea -Xms2G -Xmx4G -jar ../jars/qbf-automaton-generator.jar walkinshaw/jhotdraw.sc --ltl walkinshaw/jhotdraw.ltl --size $states --eventNumber 6 --eventNames figure,text,setpos,edit,setdim,finalise --actionNumber 0 --varNumber 0 --timeout 2000 --result generated-fsm.gv --strategy COUNTEREXAMPLE --completenessType NO_DEAD_ENDS --satSolver LINGELING
