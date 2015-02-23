#!/bin/bash

strategy=$1
timeout=$2
min_size=$3
max_size=$4
suffix=$5

echo "Compiling..."
ant ../qbf-automaton-generator-jar
echo "Evaluating..."
fsm="generated-fsm.gv"

events=5
actions=5
cnt=10

for ((size = $min_size; size <= $max_size; size++)); do
    for ((iteration = 0; iteration < 50; iteration++)); do
        for l in $((10 * size)) $((50 * size)); do
            name=testing/fsm-$size-$iteration-$l
            sc_name=$name.sc
            instance_description="s=$size n=$instance l=$l"
            ev_name=evaluation/$strategy-$suffix-$size-$instance-$l}
            if [ -f $ev_name.done ]; then
                continue
            fi
            if [[ $(cat cool.log | grep "^HARD $instance_description$") == "" ]]; then
                continue
            fi
            ltl_name=$name-$suffix.ltl
            echo ">>> $instance_description"
            rm -f "$fsm"
            java -Xms2G -jar jars/qbf-automaton-generator.jar "$sc_name" \
                --ltl "$ltl_name" --size $size --eventNumber $events --actionNumber $actions \
                --timeout $timeout -qs SKIZZO --result "$fsm" --strategy $strategy --complete \
                2>&1 | grep "\\(INFO\\|WARNING\\|SEVERE\\|Exception\\|OutOfMemoryError\\)" > $ev_name.log && touch $ev_name.done
        done
    done
done
