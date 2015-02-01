#!/bin/bash
ant qbf-automaton-generator-jar && java -ea -jar jars/qbf-automaton-generator.jar qbf/testing/fsm_2s2e4a_20.sc --ltl qbf/testing/fsm_2s2e4a-true.ltl --size 2 --timeout 15 --depth 2 -qs SKIZZO  --solverParams "" --complete --result qbf/generated-fsm.gv
