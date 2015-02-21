#!/bin/bash
states=4
events=2
actions=2
ant qbf-automaton-generator-jar && java -ea -jar jars/qbf-automaton-generator.jar qbf/testing/fsm_${states}s${events}e${actions}a_20.sc --ltl qbf/testing/fsm_${states}s${events}e${actions}a-true.ltl --size $states --eventNumber $events --actionNumber $actions --timeout 300 --result qbf/generated-fsm.gv --strategy BACKTRACKING
