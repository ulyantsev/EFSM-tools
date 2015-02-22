#!/bin/bash
states=10
instance=0
ant qbf-automaton-generator-jar && java -ea -Xms2G -jar jars/qbf-automaton-generator.jar "qbf/testing-daniil/nstates=$states/$instance/plain-scenarios" --ltl "qbf/testing-daniil/nstates=$states/$instance/formulae" --size $states --eventNumber 2 --actionNumber 2 --varNumber 2 --timeout 1000 --result qbf/generated-fsm.gv --strategy HYBRID
