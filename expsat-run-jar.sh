#!/bin/bash
ant qbf-automaton-generator-jar && java -Xms2G -ea -jar jars/qbf-automaton-generator.jar qbf/testing/fsm_4s3e2a_20.sc --ltl qbf/testing/fsm_4s3e2a-true.ltl --size 4 --timeout 15 --depth 3 --complete --bfsConstraints --result qbf/generated-fsm.gv --strategy EXP_SAT
