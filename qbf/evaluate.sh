#!/bin/bash

strategy=$1
timeout=$2
min_size=$3
max_size=$4
suffix=$5
compl=$6

if [[ $compl == "true" ]]; then
    compdir="complete"
    compcmd="--complete"
else
    compdir="incomplete"
    compcmd=""
fi

echo "Compiling..."
cd .. && ant qbf-automaton-generator-jar && cd qbf
echo "Evaluating..."

fsm="generated-fsm.gv"
events=4
actions=4

mkdir -p evaluation/$compdir

for ((size = $min_size; size <= $max_size; size++)); do
    for ((instance = 0; instance < 50; instance++)); do
        ev_name=evaluation/$compdir/$strategy-$suffix-$size-$instance
        if [ -f $ev_name.done ]; then
            continue
        fi
        name=testing/$compdir/fsm-$size-$instance
        sc_name=$name.sc
        instance_description="s=$size n=$instance"
        ltl_name=$name-$suffix.ltl
        echo ">>> $instance_description"
        rm -f "$fsm"
        java -Xms2G -Xmx4G -jar ../jars/qbf-automaton-generator.jar "$sc_name" \
            --ltl "$ltl_name" --size $size --eventNumber $events --actionNumber $actions \
            --timeout $timeout -qs SKIZZO --result "$fsm" --strategy $strategy $compcmd \
            --hybridSecToGenerateFormula 15 --hybridSecToSolve 30 \
            2>&1 | grep "\\(INFO\\|WARNING\\|SEVERE\\|Exception\\|OutOfMemoryError\\)" > $ev_name.log && touch $ev_name.done
    done
done
