#!/bin/bash

echo "Compiling..."
ant qbf-automaton-generator-jar
echo "Evaluating..."
solver=SKIZZO
timeout=15
fsm="qbf/generated-fsm.gv"
#suffix="false"
suffix="true"
solver_params=""

for ((size = 2; size <= 4; size++)); do
    for ((events = 2; events <= 5; events++)); do
        for ((actions = 2; actions <= 5; actions++)); do
            name="qbf/testing/fsm_${size}s${events}e${actions}a"
            for i in 20 40 80 160; do
                fullname=${name}_$i.sc
                echo ">>> $fullname"
                rm -f "$fsm"
                java -ea -jar jars/qbf-automaton-generator.jar "$fullname" --ltl "$name-$suffix.ltl" --size "$size" --timeout "$timeout" --depth "$size" -qs "$solver"  --solverParams "$solver_params" --complete --result "$fsm" 2>&1 | grep "\\(INFO\\|WARNING\\|SEVERE\\|Exception\\)"
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
