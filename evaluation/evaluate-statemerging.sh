#!/bin/bash
cd .. && ant qbf-automaton-generator-jar && cd evaluation 
for (( i = 0; i < 100; i++)); do
    java -ea -Xms2G -Xmx4G -jar ../jars/qbf-automaton-generator.jar testing-walkinshaw/fsm-$i.sc --ltl testing-walkinshaw/fsm-$i.ltl --size 5 --eventNumber 10 --eventNames A,B,C,D,E,F,G,H,I,J --actionNumber 0 --varNumber 0 --result testing-walkinshaw/generated-$i.dot --strategy STATE_MERGING
done
