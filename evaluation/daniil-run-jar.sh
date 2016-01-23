#!/bin/bash
states=7
instance=7
l=50
cd .. && ant qbf-automaton-generator-jar && cd qbf && java -ea -Xms2G -Xmx4G -jar ../jars/qbf-automaton-generator.jar "testing-daniil/${l}n/nstates=$states/$instance/plain-scenarios" --ltl "testing-daniil/${l}n/nstates=$states/$instance/formulae" --size $states --eventNumber 2 --actionNumber 2 --varNumber 2 --timeout 10000 --result generated-fsm.gv --strategy BACKTRACKING --completenessType NO_DEAD_ENDS
