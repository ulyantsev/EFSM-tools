#!/bin/bash
states=4
cd .. && ant qbf-automaton-generator-jar && cd qbf && java -ea -Xms2G -Xmx4G -jar ../jars/qbf-automaton-generator.jar walkinshaw/editor.sc --ltl walkinshaw/editor.ltl --size $states --eventNumber 5 --eventNames load,save,close,exit,edit --actionNumber 1 --actionNames invalid --varNumber 0 --timeout 1000 --result generated-fsm.gv --strategy HYBRID_COUNTEREXAMPLE --hybridSecToGenerateFormula 15 --hybridSecToSolve 30 --complete --completenessType NO_DEAD_ENDS_WALKINSHAW --satSolver INCREMENTAL_LINGELING
