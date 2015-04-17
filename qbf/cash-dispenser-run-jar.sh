#!/bin/bash
states=12
cd .. && ant qbf-automaton-generator-jar && cd qbf && java -ea -Xms2G -Xmx4G -jar ../jars/qbf-automaton-generator.jar cash-dispenser/plain-scenarios --ltl cash-dispenser/formulas --size $states --eventNumber 14 --actionNumber 13 --varNumber 0 --timeout 100000 --result generated-fsm.gv --strategy ITERATIVE_SAT --hybridSecToGenerateFormula 1800 --hybridSecToSolve 60 --complete --completenessType NO_DEAD_ENDS --satSolver INCREMENTAL_LINGELING
