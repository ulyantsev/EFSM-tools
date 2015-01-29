#!/bin/bash
ant qbf-automaton-generator-jar && java -ea -jar jars/qbf-automaton-generator.jar qbf/testing/fsm_6s5e5a_20.sc --ltl qbf/testing/fsm_6s5e5a-true.ltl --size 6 --timeout 3 --complete --result qbf/generated-fsm.gv --strategy ITERATIVE_SAT --bfsConstraints
