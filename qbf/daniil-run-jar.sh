#!/bin/bash
states=10
instance=0
cd .. && ant qbf-automaton-generator-jar && cd qbf && java -ea -Xms2G -jar ../jars/qbf-automaton-generator.jar "testing-daniil/nstates=$states/$instance/plain-scenarios" --ltl "testing-daniil/nstates=$states/$instance/formulae" --size $states --eventNumber 2 --actionNumber 2 --varNumber 2 --timeout 1000 --result generated-fsm.gv --strategy HYBRID
