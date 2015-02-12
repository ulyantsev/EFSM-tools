#!/bin/bash
states=10
events=3
actions=2
ant qbf-automaton-generator-jar && java -ea -Xms2G -jar jars/qbf-automaton-generator.jar qbf/testing/fsm_${states}s${events}e${actions}a_5_40.sc --ltl qbf/testing/fsm_${states}s${events}e${actions}a-5-true.ltl --size $states --eventNumber $events --actionNumber $actions --timeout 300 --complete --bfsConstraints --result qbf/generated-fsm.gv --strategy EXP_SAT
