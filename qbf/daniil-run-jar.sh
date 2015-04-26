#!/bin/bash
states=8
instance=3
l=200
cd .. && ant qbf-automaton-generator-jar && cd qbf && java -ea -Xms2G -Xmx4G -jar ../jars/qbf-automaton-generator.jar "testing-daniil/${l}n/nstates=$states/$instance/plain-scenarios" --ltl "testing-daniil/${l}n/nstates=$states/$instance/formulae" --size $states --eventNumber 2 --actionNumber 2 --varNumber 2 --timeout 1000 --result generated-fsm.gv --strategy COUNTEREXAMPLE --hybridSecToGenerateFormula 15 --completenessType NO_DEAD_ENDS
