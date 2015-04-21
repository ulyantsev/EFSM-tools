#!/bin/bash

print_sat() {
    if (($2 > 25)); then
        local med="%5.1f"
    else
        local med="[%3.0f]"
    fi
    if (($2 > 37)); then
        local q3="%5.1f"
    else
        local q3="[%3.0f]"
    fi
    printf "%13s sat   %2d, unknown %2d, unsat %2d, total %2d, solved fraction %5s%%, q2=$med, q3=$q3\n" $1 $2 $3 $4 $(($2 + $3 + $4)) $(python -c "print(round(float($2) / ($2 + $3 + $4) * 100, 2))") $5 $6
}

print_unsat() {
    if (($2 > 25)); then
        local med="%5.1f"
    else
        local med="[%3.0f]"
    fi
    if (($2 > 37)); then
        local q3="%5.1f"
    else
        local q3="[%3.0f]"
    fi
    printf "%13s unsat %2d, unknown %2d, sat   %2d, total %2d, solved fraction %5s%%, q2=$med, q3=$q3\n" $1 $2 $3 $4 $(($2 + $3 + $4)) $(python -c "print(round(float($2) / ($2 + $3 + $4) * 100, 2))") $5 $6
}

print_found_by_prefix() {
    local dir="$1"
    local prefix=$2
    local text=$(cat $dir/$prefix*.log)
    local sat=$(echo "$text" | grep "WAS FOUND" | wc -l)
    local unsat=$(echo "$text" | grep "UNSAT" | wc -l)
    local unknown=$(echo "$text" | grep "\\(TIME LIMIT EXCEEDED\\|UNKNOWN\\|OutOfMemoryError\\)" | wc -l)
    
    # find median time
    local str=$(echo $(echo "$text" | grep "execution time" | sed -e "s/^.*time: //g" | sort -n))
    local arr=()
    IFS=' ' read -a arr <<< "$str"
    local len=${#arr[@]}
    if (( len % 2 == 0 )); then
        left=${arr[$(($len / 2 - 1))]}
        right=${arr[$(($len / 2))]}
        q2=$(python -c "print($left / 2 + $right / 2)")
    else
        q2=${arr[$(($len / 2))]}
    fi
    q3=${arr[$(($len * 3 / 4))]}

    if [[ "$prefix" == "${prefix/-fa/}" ]]; then
        print_sat $prefix $sat $unknown $unsat $q2 $q3
    else
        print_unsat $prefix $unsat $unknown $sat $q2 $q3
    fi
}

for compdir in "complete" "incomplete"; do
    echo ">>> $compdir"
    for instance_type in tr fa; do
        for prefix in COUNT*-$instance_type; do
        #for prefix in HYBR*-$instance_type EXP*-$instance_type ITER*-$instance_type QSAT*-$instance_type BACK*-$instance_type; do
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
