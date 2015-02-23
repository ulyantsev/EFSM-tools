#!/bin/bash
states=4
events=2
actions=2
ant ../qbf-automaton-generator-jar && java -ea -jar ../jars/qbf-automaton-generator.jar testing/fsm_${states}s${events}e${actions}a_20.sc --ltl testing/fsm_${states}s${events}e${actions}a-true.ltl --size $states --eventNumber $events --actionNumber $actions --timeout 300 --result generated-fsm.gv --strategy BACKTRACKING
