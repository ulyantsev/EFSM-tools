#!/bin/bash
ant qbf-automaton-generator-jar && java -ea -jar jars/qbf-automaton-generator.jar qbf/testing/fsm_4s2e2a_20.sc --ltl qbf/testing/fsm_4s2e2a-true.ltl --size 4 --timeout 300 --result qbf/generated-fsm.gv --strategy BACKTRACKING
