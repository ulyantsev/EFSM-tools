#!/bin/bash
states=4
java -Xmx4G -jar ../jars/qbf-automaton-generator.jar editor.sc --ltl editor.ltl --negsc editor.negsc --size $states --eventNumber 5 --eventNames load,save,close,exit,edit --actionNumber 0 --varNumber 0 --result generated-fsm.gv --strategy COUNTEREXAMPLE --completenessType NO_DEAD_ENDS --satSolver LINGELING
