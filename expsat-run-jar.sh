#!/bin/bash
states=6
events=4
actions=4
ant qbf-automaton-generator-jar && java -Xms2G -ea -jar jars/qbf-automaton-generator.jar qbf/testing/fsm_${states}s${events}e${actions}a_20.sc --ltl qbf/testing/fsm_${states}s${events}e${actions}a-true.ltl --size $states --eventNumber $events --actionNumber $actions --timeout 15 --complete --bfsConstraints --result qbf/generated-fsm.gv --strategy EXP_SAT
