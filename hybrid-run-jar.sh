#!/bin/bash
states=10
events=3
actions=2
ant qbf-automaton-generator-jar && java -ea -Xms2G -jar jars/qbf-automaton-generator.jar qbf/testing/fsm_${states}s${events}e${actions}a_5_80.sc --ltl qbf/testing/fsm_${states}s${events}e${actions}a-2-true.ltl --size $states --eventNumber $events --actionNumber $actions --timeout 1000 --result qbf/generated-fsm.gv --strategy HYBRID
