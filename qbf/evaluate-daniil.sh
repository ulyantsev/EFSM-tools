#!/bin/bash

min_size=5
max_size=10

echo "Compiling..."
cd .. && ant qbf-automaton-generator-jar && cd qbf
echo "Evaluating..."
fsm="generated-fsm.gv"

#for l in 50 100 200; do
for l in 200; do
    timeout=$(($l * 6))
    for ((size = $min_size; size <= $max_size; size++)); do
        for ((instance = 0; instance < 50; instance++)); do
            ev_name=evaluation-daniil/$size-$instance-$l
            if [[ $(cat daniil-hard/$l-hard-runs | grep "^$size/$instance$") == "" ]]; then
                continue
            fi
            if [ -f $ev_name.done ]; then
                continue
            fi
            name="testing-daniil/${l}n/nstates=$size/$instance"
            sc_name=$name/plain-scenarios
            ltl_name=$name/formulae
            echo ">>> s=$size num=$instance l=$l"
            rm -f "$fsm"
            java -Xms2G -Xmx4G -jar ../jars/qbf-automaton-generator.jar "$sc_name" \
                --ltl "$ltl_name" --size $size --eventNumber 2 --actionNumber 2 --varNumber 2 \
                --timeout $timeout -qs SKIZZO --result "$fsm" --strategy HYBRID_COUNTEREXAMPLE \
                --hybridSecToGenerateFormula 15 --hybridSecToSolve 30 \
                2>&1 | grep "\\(INFO\\|WARNING\\|SEVERE\\|Exception\\|OutOfMemoryError\\)" > $ev_name.log && touch $ev_name.done
        done
    done
done
