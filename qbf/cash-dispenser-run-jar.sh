#!/bin/bash
states=12
cd .. && ant qbf-automaton-generator-jar && cd qbf && java -ea -Xms2G -Xmx4G -jar ../jars/qbf-automaton-generator.jar cash-dispenser/plain-scenarios --ltl cash-dispenser/formulas --size $states --eventNumber 14 --actionNumber 13 --varNumber 0 --timeout 100000 --result generated-fsm.gv --strategy COUNTEREXAMPLE --hybridSecToGenerateFormula 60 --hybridSecToSolve 30 --completenessType NO_DEAD_ENDS --satSolver LINGELING
