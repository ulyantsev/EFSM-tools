#!/bin/bash

print_sat() {
    printf "%13s sat %4d, unknown %4d, unsat %2d, total %4d, solved fraction %5s%%\n" $1 $2 $3 $4 $(($2 + $3 + $4)) $(python -c "print(round(float($2) / ($2 + $3 + $4) * 100, 2))")
}

print_unsat() {
    printf "%13s unsat %4d, unknown %4d, sat %2d, total %4d, solved fraction %5s%%\n" $1 $2 $3 $4 $(($2 + $3 + $4)) $(python -c "print(round(float($2 + $4) / ($2 + $3 + $4) * 100, 2))")
}

print_found_by_prefix() {
    local dir="$1"
    local prefix=$2
    local text=$(cat $dir/$prefix*)
    local sat=$(echo "$text" | grep "WAS FOUND" | wc -l)
    local unsat=$(echo "$text" | grep "UNSAT" | wc -l)
    local unknown=$(echo "$text" | grep "\\(TIME LIMIT EXCEEDED\\|UNKNOWN\\|OutOfMemoryError\\)" | wc -l)
    if [[ "$prefix" == "${prefix/-fa/}" ]]; then
        print_sat $prefix $sat $unknown $unsat
    else
        print_unsat $prefix $unsat $unknown $sat
    fi
}

for compdir in "complete" "incomplete"; do
    echo ">>> $compdir"
    for instance_type in tr fa; do
        for prefix in HYBR*-$instance_type EXP*-$instance_type ITER*-$instance_type QSAT*-$instance_type BACK*-$instance_type; do
            echo_str=
            for ((s = 3; s <= 10; s++)); do
                ls evaluation/$compdir/$prefix*-$s-*.done 1>/dev/null 2>/dev/null
                if [[ $? != 0 ]]; then
                    continue
                fi
                print_found_by_prefix evaluation/$compdir "$prefix*-$s-"
                echo_str='\n'
            done
            echo -en "$echo_str"
        done
    done
done

echo not verified $(cat $(ls evaluation/*/*) | grep "NOT VERIFIED" | wc -l), not complies to scenarios $(cat $(ls evaluation/*/*) | grep "NOT COMPLIES" | wc -l), severe $(cat $(ls evaluation/*/*) | grep "SEVERE" | wc -l), out of memory $(cat $(ls evaluation/*/*) | grep "OutOfMemoryError" | wc -l)
