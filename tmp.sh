#!/bin/bash
ant qbf-automaton-generator-jar && java -ea -Xms2G -jar jars/qbf-automaton-generator.jar tmp.sc --ltl tmp.ltl --size 3 --eventNumber 2 --actionNumber 2 --timeout 1000 --result qbf/generated-fsm.gv --strategy ITERATIVE_SAT --varNumber 2
