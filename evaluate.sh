#!/bin/bash

strategy=$1
timeout=$2
min_size=$3
max_size=$4

echo "Compiling..."
ant qbf-automaton-generator-jar
echo "Evaluating..."
fsm="qbf/generated-fsm.gv"
solver_params=""

for suffix in true false; do
    echo SUFFIX $suffix
    for ((size = $min_size; size <= $max_size; size++)); do
        for ((events = 2; events <= 5; events++)); do
            for ((actions = 2; actions <= 5; actions++)); do
                name="qbf/testing/fsm_${size}s${events}e${actions}a"
                for cnt in 5 10 20; do
                    for l in 20 30 40 60 80 120 160 240; do
                        sc_name=${name}_${cnt}_$i.sc
                        for ((formula_num = 1; formula_num <= 5; formula_num++)); do
                            instance_description="s=$size e=$events a=$actions cnt=$cnt l=$l f=$formula_num"
                            if [ cat cool.log | grep "GOOD \\[VERIFIED\\] $instance_description" ]; then
                                echo $instance_description
                            fi
                            continue
                            ltl_name=$name-$formula_num-$suffix.ltl
                            echo ">>> $instance_description"
                            rm -f "$fsm"
                            java -Xms2G -ea -jar jars/qbf-automaton-generator.jar "$sc_name" \
                                --ltl "$ltl_name" --size $size --eventNumber $events --actionNumber $actions \
                                --timeout $timeout -qs SKIZZO --complete --bfsConstraints \
                                --result  "$fsm" --strategy $strategy \
                                2>&1 | grep "\\(INFO\\|WARNING\\|SEVERE\\|Exception\\)"
                        done
                    done
                done
            done
        done
    done
done
