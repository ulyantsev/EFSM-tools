#!/bin/bash

echo "Compiling..."
ant qbf-automaton-generator-jar
echo "Evaluating..."
timeout=30
fsm="qbf/generated-fsm.gv"

for suffix in true false; do
    echo SUFFIX $suffix
    for ((size = 5; size <= 5; size++)); do
        for ((events = 2; events <= 5; events++)); do
            for ((actions = 2; actions <= 5; actions++)); do
                name="qbf/testing/fsm_${size}s${events}e${actions}a"
                for i in 20 40 80 160 320; do
                    fullname=${name}_$i.sc
                    echo ">>> $fullname"
                    rm -f "$fsm"
                    java -ea -jar jars/qbf-automaton-generator.jar "$fullname" --ltl "$name-$suffix.ltl" --size $size --eventNumber $events --actionNumber $actions --timeout "$timeout" --complete --strategy BACKTRACKING --result "$fsm" 2>&1 | grep "\\(INFO\\|WARNING\\|SEVERE\\|Exception\\)"
                    if [ -f "$fsm" ]; then
                        if [[ $(diff -u "$name.dot" "$fsm" | wc -l) == 0 ]]; then
                            echo "FSM MATCH"
                        else
                            echo "FSM MISMATCH!!!"
                        fi
                    fi
                done
            done
        done
    done
done
