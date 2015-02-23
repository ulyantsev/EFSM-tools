#!/bin/bash

res=cfsm.gv

for ((size = 3; size <= 10; size++)); do
    for ((instance = 0; instance < 50; instance++)); do
        for l in $((10 * size)) $((50 * size)); do
            java -jar ../jars/sat-builder.jar "testing/fsm-$size-$instance-$l.sc" --result $res --size $size 2>/dev/null
            report_str="s=$size n=$instance l=$l"
            ltl="testing/fsm-$size-$instance-true.ltl"
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
done

