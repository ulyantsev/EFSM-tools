#!/bin/bash
states=4
cd .. && ant qbf-automaton-generator-jar && cd qbf && java -ea -Xms2G -Xmx4G -jar ../jars/qbf-automaton-generator.jar walkinshaw/editor.sc --ltl walkinshaw/editor.ltl --negsc walkinshaw/editor.negsc --size $states --eventNumber 5 --eventNames load,save,close,exit,edit --actionNumber 0 --varNumber 0 --timeout 1000 --result generated-fsm.gv --strategy COUNTEREXAMPLE --completenessType NO_DEAD_ENDS --satSolver LINGELING
