#!/bin/bash

print_sat() {
    printf "%13s sat %4d, unknown %4d, unsat %2d, total %4d, solved fraction %5s%%\n" $1 $2 $3 $4 $(($2 + $3 + $4)) $(python -c "print(round(float($2) / ($2 + $3 + $4) * 100, 2))")
}

print_unsat() {
    printf "%13s unsat %4d, unknown %4d, sat %2d, total %4d, solved fraction %5s%%\n" $1 $2 $3 $4 $(($2 + $3 + $4)) $(python -c "print(round(float($2 + $4) / ($2 + $3 + $4) * 100, 2))")
}

print_found_by_prefix() {
    local prefix=$1
    local text=$(cat evaluation/$prefix*)
    local sat=$(echo "$text" | grep "WAS FOUND" | wc -l)
    local unsat=$(echo "$text" | grep "UNSAT" | wc -l)
    local unknown=$(echo "$text" | grep "\\(TIME LIMIT EXCEEDED\\|UNKNOWN\\)" | wc -l)
    if [[ "$prefix" == "${prefix/_fa/}" ]]; then
        print_sat $prefix $sat $unknown $unsat
    else
        print_unsat $prefix $unsat $unknown $sat
    fi
}

for instance_type in tr fa; do
    for prefix in EXP*_$instance_type QSAT*_$instance_type ITER*_$instance_type BACK*_$instance_type; do
        ls evaluation/$prefix* 1>/dev/null 2>/dev/null
        if [[ $? != 0 ]]; then
            continue
        fi
        #print_found_by_prefix $prefix
        for ((s = 2; s <= 10; s++)); do
            ls evaluation/$prefix*s=$s* 1>/dev/null 2>/dev/null
            if [[ $? != 0 ]]; then
                continue
            fi
            print_found_by_prefix "$prefix*s=$s"
        done
        
        #echo
        #for ((f = 1; f <= 5; f++)); do
        #    ls evaluation/$prefix*f=$f* 1>/dev/null 2>/dev/null
        #    if [[ $? != 0 ]]; then
        #        continue
        #    fi
        #    print_found_by_prefix "$prefix*f=$f"
        #done
        
        cat $(ls -rt evaluation/$prefix*) | grep "execution" | sed -e 's/^.*: //g;' > times.tmp
        cat $(ls -rt evaluation/$prefix*) | grep "FOUND" | sed -e 's/INFO: Automaton with [0-9]\+//g; s/states //g; s/FOUND!//g; s/ WAS/1/g; s/ NOT/0/g' > succ.tmp
        paste succ.tmp times.tmp > stats-$prefix.csv
        rm times.tmp succ.tmp
        echo
    done
done

echo not verified $(cat $(ls -rt evaluation/*) | grep "NOT VERIFIED" | wc -l), not complies to scenarios $(cat $(ls -rt evaluation/*) | grep "NOT COMPLIES" | wc -l), severe $(cat $(ls -rt evaluation/*) | grep "SEVERE" | wc -l)
