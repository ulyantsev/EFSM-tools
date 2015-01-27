#!/bin/bash
ant qbf-automaton-generator-jar && java -Xms2G -ea -jar jars/qbf-automaton-generator.jar qbf/testing/fsm_4s4e4a_20.sc --ltl qbf/testing/fsm_4s4e4a-true.ltl --size 4 --timeout 5 --depth 2 -qs SKIZZO --complete --result qbf/generated-fsm.gv --strategy ITERATIVE_SAT
