#!/bin/bash
ant qbf-automaton-generator-jar && java -ea -jar jars/qbf-automaton-generator.jar qbf/testing/fsm_2s4e2a_40.sc --ltl qbf/testing/fsm_2s4e2a-true.ltl --size 2 --timeout 300 --complete --result qbf/generated-fsm.gv --strategy BACKTRACKING
