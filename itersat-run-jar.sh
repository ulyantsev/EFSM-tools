#!/bin/bash
states=5
events=3
actions=3
ant qbf-automaton-generator-jar && java -jar jars/qbf-automaton-generator.jar qbf/testing/fsm_${states}s${events}e${actions}a_5_20.sc --ltl qbf/testing/fsm_${states}s${events}e${actions}a-3-true.ltl --size $states --eventNumber $events --actionNumber $actions --timeout 30 --complete --result qbf/generated-fsm.gv --strategy ITERATIVE_SAT --bfsConstraints
