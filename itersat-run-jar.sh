#!/bin/bash
states=7
events=2
actions=3
ant qbf-automaton-generator-jar && java -ea -jar jars/qbf-automaton-generator.jar qbf/testing/fsm_${states}s${events}e${actions}a_20.sc --ltl qbf/testing/fsm_${states}s${events}e${actions}a-true.ltl --size $states --eventNumber $events --actionNumber $actions --timeout 15 --complete --result qbf/generated-fsm.gv --strategy ITERATIVE_SAT --bfsConstraints
