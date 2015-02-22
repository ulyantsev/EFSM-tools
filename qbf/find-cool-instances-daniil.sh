#!/bin/bash

res=cfsm.gv

for ((size = 5; size <= 10; size++)); do
    for ((instance = 0; instance <= 49; instance++)); do
        java -jar ../jars/sat-builder.jar "testing-daniil/nstates=$size/$instance/plain-scenarios" --result $res --size $size 2>/dev/null
        report_str="s=$size n=$instance"
        ltl="testing-daniil/nstates=$size/$instance/formulae"
        verified=$(java -jar verifier.jar $res $size $ltl | wc -l)
        formula_num=$(cat "$ltl" | wc -l)
        if [[ $verified == $formula_num ]]; then
            echo "EASY $report_str"
        else
            echo "HARD $report_str"
        fi
        rm -f $res
    done
done

