#!/bin/bash
states=3
events=5
actions=5
inst=2
cd .. && ant qbf-automaton-generator-jar && cd qbf && java -ea -jar ../jars/qbf-automaton-generator.jar testing/fsm-$states-$inst.sc --ltl testing/fsm-$states-$inst-true.ltl --size $states --eventNumber $events --actionNumber $actions --timeout 300 -qs SKIZZO  --solverParams "" --result generated-fsm.gv --complete
