#!/bin/bash

print_sat() {
    printf "%4s sat %4d, unknown %4d, unsat %2d, total %4d, solved fraction %5s%%\n" $1 $2 $3 $4 $(($2 + $3 + $4)) $(python -c "print(round(float($2) / ($2 + $3 + $4) * 100, 2))")
}

print_found_by_prefix() {
    local prefix=$1
    local text=$(cat evaluation-daniil/$prefix*)
    local sat=$(echo "$text" | grep "WAS FOUND" | wc -l)
    local unsat=$(echo "$text" | grep "UNSAT" | wc -l)
    local unknown=$(echo "$text" | grep "\\(TIME LIMIT EXCEEDED\\|UNKNOWN\\)" | wc -l)
    print_sat $prefix $sat $unknown $unsat
}

for ((s = 5; s <= 10; s++)); do
    ls evaluation-daniil/$s-* 1>/dev/null 2>/dev/null
    if [[ $? != 0 ]]; then
        continue
    fi
    print_found_by_prefix "$s-*"
done
for ((s = 5; s <= 10; s++)); do
    ls evaluation-daniil/$s-* 1>/dev/null 2>/dev/null
    if [[ $? != 0 ]]; then
        continue
    fi
    str=$(echo $(grep -nr "execution time" evaluation-daniil/$s-* | sed -e "s/^.*time: //g" | sort -n))
    #echo $str
    IFS=' ' read -a arr <<< "$str"
    printf "quartiles 0..4 for s=%d: %8s %8s %8s %8s %8s\n" $s ${arr[0]} ${arr[index=$((${#arr[@]} / 4))]} ${arr[index=$((${#arr[@]} / 2))]} ${arr[index=$((${#arr[@]} * 3 / 4))]} ${arr[-1]}
done

cat $(ls -rt evaluation-daniil/*) | grep "execution" | sed -e 's/^.*: //g;' > times.tmp
cat $(ls -rt evaluation-daniil/*) | grep "FOUND" | sed -e 's/INFO: Automaton with [0-9]\+//g; s/states //g; s/FOUND!//g; s/ WAS/1/g; s/ NOT/0/g' > succ.tmp
paste succ.tmp times.tmp > stats-daniil.csv
rm times.tmp succ.tmp
echo

echo not verified $(cat $(ls -rt evaluation-daniil/*) | grep "NOT VERIFIED" | wc -l), not complies to scenarios $(cat $(ls -rt evaluation-daniil/*) | grep "NOT COMPLIES" | wc -l), severe $(cat $(ls -rt evaluation-daniil/*) | grep "SEVERE" | wc -l)
