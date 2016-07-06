#!/bin/bash
states=7
java -Xmx4G -jar ../jars/qbf-automaton-generator.jar jhotdraw.sc --ltl jhotdraw.ltl --size $states --eventNumber 6 --eventNames figure,text,setpos,edit,setdim,finalise --actionNumber 0 --varNumber 0 --result generated-fsm.gv --strategy COUNTEREXAMPLE --completenessType NO_DEAD_ENDS --satSolver LINGELING
