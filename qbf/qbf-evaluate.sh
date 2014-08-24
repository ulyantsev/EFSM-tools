#!/bin/bash

echo "Evaluating..."
solver=SKIZZO
timeout=20
fsm="generated-fsm.gv"
#suffix="false"
suffix="true"

jv=/usr/lib/jvm/jdk1.7.0_45/bin/java
#jv=java

for ((size = 2; size <= 4; size++)); do
    for ((events = 2; events <= 5; events++)); do
        for ((actions = 2; actions <= 5; actions++)); do
            name="testing/fsm_${size}s${events}e${actions}a"
            for i in 20 40 80 160; do
                fullname=${name}_$i.sc
                echo ">>> $fullname"
                rm -f "$fsm"
                java -ea -jar ../jars/qbf-automaton-generator.jar "$fullname" --ltl "$name-$suffix.ltl" --size "$size" --timeout "$timeout" --depth "$size" -qs "$solver" --complete --result "$fsm" 2>&1 | grep "\\(INFO\\|WARNING\\|SEVERE\\|Exception\\)"
                if [ -f "$fsm" ]; then
                    if [[ $(diff -u "$name.dot" "$fsm" | wc -l) == 0 ]]; then
                        echo "FSM MATCH"
                    else
                        echo "FSM MISMATCH!!!"
                    fi
                    correct_formulas=$(java -jar verifier.jar "$fsm" "$size" "$name-$suffix.ltl" | wc -l)
                    if (( $(cat "$name-$suffix.ltl" | wc -l) == 0 )); then
                        echo "NOTHING TO VERIFY"
                    elif (( correct_formulas == 1 )); then
                        echo "VERIFIED"
                    else 
                        echo "NOT VERIFIED!!!"
                    fi
                fi
            done
        done
    done
done
