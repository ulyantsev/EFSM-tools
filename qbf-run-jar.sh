#!/bin/bash
states=2
events=2
actions=2
ant qbf-automaton-generator-jar && java -ea -jar jars/qbf-automaton-generator.jar qbf/testing/fsm_${states}s${events}e${actions}a_10_20.sc --ltl qbf/testing/fsm_${states}s${events}e${actions}a-2-true.ltl --size $states --eventNumber $events --actionNumber $actions --timeout 300 -qs SKIZZO  --solverParams "" --result qbf/generated-fsm.gv
