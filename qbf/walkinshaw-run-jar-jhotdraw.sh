#!/bin/bash
states=3
cd .. && ant qbf-automaton-generator-jar && cd qbf && java -ea -Xms2G -Xmx4G -jar ../jars/qbf-automaton-generator.jar walkinshaw/jhotdraw.sc --ltl walkinshaw/jhotdraw.ltl --size $states --eventNumber 6 --eventNames figure,text,setpos,edit,setdim,finalise --actionNumber 1 --varNumber 0 --timeout 1000 --result generated-fsm.gv --strategy HYBRID --hybridSecToGenerateFormula 15 --hybridSecToSolve 30 --complete --completenessType NO_DEAD_ENDS_WALKINSHAW
