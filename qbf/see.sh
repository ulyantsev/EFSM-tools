#!/bin/bash

print_sat() {
    printf "%13s sat %4d, unknown %4d, unsat %2d, total %4d, solved fraction %5s%%\n" $1 $2 $3 $4 $(($2 + $3 + $4)) $(python -c "print(round(float($2) / ($2 + $3 + $4) * 100, 2))")
}

print_unsat() {
    printf "%13s unsat %4d, unknown %4d, sat %2d, total %4d, solved fraction %5s%%\n" $1 $2 $3 $4 $(($2 + $3 + $4)) $(python -c "print(round(float($2 + $4) / ($2 + $3 + $4) * 100, 2))")
}

print_found_by_regexp() {
    local regexp=$1
    local text=$(cat evaluation/$regexp)
    local sat=$(echo "$text" | grep "WAS FOUND" | wc -l)
    local unsat=$(echo "$text" | grep "UNSAT" | wc -l)
    local unknown=$(echo "$text" | grep "\\(TIME LIMIT EXCEEDED\\|UNKNOWN\\)" | wc -l)
    if [[ "$regexp" == "${regexp/-fa/}" ]]; then
        print_sat $prefix $sat $unknown $unsat
    else
        print_unsat $prefix $unsat $unknown $sat
    fi
}

for instance_type in tr fa; do
    for prefix in HYBR*-$instance_type EXP*-$instance_type ITER*-$instance_type QSAT*-$instance_type BACK*-$instance_type; do
        ls evaluation/$prefix* 1>/dev/null 2>/dev/null
        if [[ $? != 0 ]]; then
            continue
        fi
        for l_func in "l=$((10 * size))" "l=$((50 * size))"; do
            for ((s = 3; s <= 10; s++)); do
                eval l_func
                ls evaluation/$prefix-$s* 1>/dev/null 2>/dev/null
                if [[ $? != 0 ]]; then
                    continue
                fi
                print_found_by_regexp "$prefix-$s-*-l=$l"
            done
        done
    done
done

echo not verified $(cat $(ls evaluation/*) | grep "NOT VERIFIED" | wc -l), not complies to scenarios $(cat $(ls evaluation/*) | grep "NOT COMPLIES" | wc -l), severe $(cat $(ls evaluation/*) | grep "SEVERE" | wc -l)
