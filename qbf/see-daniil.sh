#!/bin/bash

dir=evaluation-daniil

print_sat() {
    printf "%12s  sat %2d, unknown %2d, total %2d, solved fraction %5s%%\n" $1 $2 $3 $(($2 + $3)) $(python -c "print(round(float($2) / ($2 + $3) * 100, 2))")
}

print_found_by_regexp() {
    local prefix=$1
    local text=$(cat $dir/$prefix)
    local sat=$(echo "$text" | grep "WAS FOUND" | wc -l)
    local unsat=$(echo "$text" | grep "UNSAT" | wc -l)
    if [[ "$unsat" != 0 ]]; then
        echo "UNSAT!"
    fi
    local unknown=$(echo "$text" | grep "\\(TIME LIMIT EXCEEDED\\|UNKNOWN\\|OutOfMemoryError\\)" | wc -l)
    print_sat $prefix $sat $unknown
}

for l in 50 100 200; do
    echo "*** l = $l"
    for ((s = 5; s <= 10; s++)); do
        ls $dir/$s-*-$l.done 1>/dev/null 2>/dev/null
        if [[ $? != 0 ]]; then
            continue
        fi
        print_found_by_regexp "$s-*-$l.log"
    done
    for ((s = 5; s <= 10; s++)); do
        ls $dir/$s-*-$l.done 1>/dev/null 2>/dev/null
        if [[ $? != 0 ]]; then
            continue
        fi
        str=$(echo $(grep -nr "execution time" $dir/$s-*-$l.log | sed -e "s/^.*time: //g" | sort -n))
        IFS=' ' read -a arr <<< "$str"
        len=${#arr[@]}
        q0=${arr[0]}
        q1=${arr[$(($len / 4))]}
        if (( len % 2 == 0 )); then
            left=${arr[$(($len / 2 - 1))]}
            right=${arr[$(($len / 2))]}
            q2=$(python -c "print($left / 2 + $right / 2)")
        else
            q2=${arr[$(($len / 2))]}
        fi
        q3=${arr[$(($len * 3 / 4))]}
        q4=${arr[-1]}
        printf "quartiles 0..4 for s=%2d:  %5.1f  %5.1f  [%5.1f]  %5.1f  %5.1f\n" $s $q0 $q1 $q2 $q3 $q4   
    done
    echo
done

cat $(ls -rt $dir/*) | grep "execution" | sed -e 's/^.*: //g;' > times.tmp
cat $(ls -rt $dir/*) | grep "FOUND" | sed -e 's/INFO: Automaton with [0-9]\+//g; s/states //g; s/FOUND!//g; s/ WAS/1/g; s/ NOT/0/g' > succ.tmp
paste succ.tmp times.tmp > stats-daniil.csv
rm times.tmp succ.tmp

echo not verified $(cat $(ls $dir/*) | grep "NOT VERIFIED" | wc -l), not complies to scenarios $(cat $(ls $dir/*) | grep "NOT COMPLIES" | wc -l), severe $(cat $(ls $dir/*) | grep "SEVERE" | wc -l), out of memory $(cat $(ls $dir/*) | grep "OutOfMemoryError" | wc -l)
