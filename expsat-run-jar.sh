#!/bin/bash
states=4
events=5
actions=4
ant qbf-automaton-generator-jar && java -ea -Xms2G -jar jars/qbf-automaton-generator.jar qbf/testing/fsm_${states}s${events}e${actions}a_20_30.sc --ltl qbf/testing/fsm_${states}s${events}e${actions}a-1-false.ltl --size $states --eventNumber $events --actionNumber $actions --timeout 100 --complete --bfsConstraints --result qbf/generated-fsm.gv --strategy EXP_SAT
