#!/bin/bash

events=25
eventnames=$(python3 -c "for i in range(0, $events - 1): print('e' + str(i), end=',')")e$((events - 1))
actions=4
actionnames="b0,b1,b2,b3"
size=20
instance_type=train
check_type=test

for (( instance = 1; instance <= 10; instance++ )); do
    sc_name=$instance_type/$instance_type-$size-$instance.sc
    java -jar -Xmx4G ../../jars/plant-automaton-generator.jar "$sc_name" \
        --size $size \
        --eventNumber $events --actionNumber $actions \
        --eventNames "$eventnames" \
        --actionNames "$actionnames" \
        --result "automaton.gv" \
        --deterministic --bfsConstraints --incomplete 2>&1 | grep "^\\(INFO\\|SEVERE\\|WARNING\\|Exception\\|Error\\)"
    for measure in STRONG MEDIUM WEAK; do
        echo "> Testing (measure=$measure):"
        java -jar ../../jars/moore-scenario-compliance-checker.jar \
            automaton.gv $check_type/$check_type-$size-$instance.sc \
            --measure $measure
        echo "> vs."
        java -jar ../../jars/moore-scenario-compliance-checker.jar \
            learned/l-$size-$instance.dot $check_type/$check_type-$size-$instance.sc \
            --measure $measure
    done
done
