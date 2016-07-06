#!/bin/bash

events=25
eventnames=$(python3 -c "for i in range(0, $events - 1): print('e' + str(i), end=',')")e$((events - 1))
actions=3
actionnames="b0,b1,b2"
size=10
instance_type=cs
check_type=test

for (( instance = 1; instance <= 5; instance++ )); do
    sc_name=$instance_type/$instance_type-$size-$instance.sc
    java -jar -Xmx4G ../../jars/plant-automaton-generator.jar "$sc_name" \
        --size $size \
        --eventNumber $events --actionNumber $actions \
        --eventNames "$eventnames" \
        --actionNames "$actionnames" \
        --result "automaton.gv" \
        --deterministic --bfsConstraints --incomplete
    java -jar ../../jars/moore-scenario-compliance-checker.jar \
        automaton.gv $check_type/$check_type-$size-$instance.sc
done
