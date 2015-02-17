#!/bin/bash
states=10
events=2
actions=3
ant qbf-automaton-generator-jar && java -ea -Xms2G -jar jars/qbf-automaton-generator.jar qbf/testing/fsm_${states}s${events}e${actions}a_10_40.sc --ltl qbf/testing/fsm_${states}s${events}e${actions}a-3-true.ltl --size $states --eventNumber $events --actionNumber $actions --timeout 100 --complete --bfsConstraints --result qbf/generated-fsm.gv --strategy HYBRID
