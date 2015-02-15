#!/bin/bash
states=4
events=5
actions=4
ant qbf-automaton-generator-jar && java -jar jars/qbf-automaton-generator.jar qbf/testing/fsm_${states}s${events}e${actions}a_20_30.sc --ltl qbf/testing/fsm_${states}s${events}e${actions}a-1-false.ltl --size $states --eventNumber $events --actionNumber $actions --timeout 600 --complete --result qbf/generated-fsm.gv --strategy ITERATIVE_SAT --bfsConstraints
