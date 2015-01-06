#!/bin/bash
ant qbf-automaton-generator-jar && java -Xms2G -ea -jar jars/qbf-automaton-generator.jar qbf/testing/fsm_3s2e2a_160.sc --ltl qbf/testing/fsm_3s3e3a-true.ltl --size 3 --timeout 5 --depth 1 -qs SKIZZO --complete --result qbf/generated-fsm.gv --useSAT
