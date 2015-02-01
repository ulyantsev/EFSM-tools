#!/bin/bash
ant qbf-automaton-generator-jar && java -ea -jar jars/qbf-automaton-generator.jar qbf/testing/fsm_3s3e3a_80.sc --ltl qbf/testing/fsm_3s3e3a-true.ltl --size 3 --timeout 300 --complete --result qbf/generated-fsm.gv --strategy ITERATIVE_SAT --bfsConstraints
