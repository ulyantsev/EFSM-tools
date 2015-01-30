#!/bin/bash
ant qbf-automaton-generator-jar && java -ea -jar jars/qbf-automaton-generator.jar qbf/testing/fsm_3s2e2a_20.sc --ltl qbf/testing/fsm_3s2e2a-true.ltl --size 3 --timeout 30 --complete --result qbf/generated-fsm.gv --strategy BACKTRACKING
