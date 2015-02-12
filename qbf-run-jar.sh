#!/bin/bash
states=10
events=2
actions=3
ant qbf-automaton-generator-jar && java -ea -jar jars/qbf-automaton-generator.jar qbf/testing/fsm_${states}s${events}e${actions}a_10_40.sc --ltl qbf/testing/fsm_${states}s${events}e${actions}a-2-true.ltl --size $states --eventNumber $events --actionNumber $actions --timeout 300 -qs SKIZZO  --solverParams "" --complete --bfsConstraints --result qbf/generated-fsm.gv
