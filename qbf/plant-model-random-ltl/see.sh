#!/bin/bash

dir=evaluation

for size in 10 15 20 25 30 35; do
    echo "***size: $size;" "Found: "$(cat $dir/$size-*/*.full.log | grep "WAS FOUND" | wc -l)";" "Not Found: "$(cat $dir/$size-*/*.full.log | grep "NOT FOUND" | wc -l)";" "Exception: "$(cat $dir/$size-*/*.full.log | grep "Exception" | wc -l)";" "Severe: "$(cat $dir/$size-*/*.full.log | grep "SEVERE" | wc -l)
    str=$(echo $(grep -nr "execution time" $dir/$size-*/*.full.log | sed -e "s/^.*time: //g" | sort -n))
    IFS=' ' read -a arr <<< "$str"
    len=${#arr[@]}
    if (( len == 0 )); then
        q2=0
    elif (( len % 2 == 0 )); then
        left=${arr[$(($len / 2 - 1))]}
        right=${arr[$(($len / 2))]}
        q2=$(python -c "print($left / 2 + $right / 2)")
    else
        q2=${arr[$(($len / 2))]}
    fi
    #echo "Median time: $q2; Mean time: "$(python -c "print((${str// /+})/$len)")
    echo "Median time: $q2; Q3: ${arr[$(($len * 3 / 4))]}"
    str=$(echo $(grep -nr "ITERATIONS:" $dir/$size-*/*.full.log | sed -e "s/^.*ITERATIONS: //g" | sort -n))
    IFS=' ' read -a arr <<< "$str"
    len=${#arr[@]}
    if (( len == 0 )); then
        q2=0
    elif (( len % 2 == 0 )); then
        left=${arr[$(($len / 2 - 1))]}
        right=${arr[$(($len / 2))]}
        q2=$(python -c "print($left / 2 + $right / 2)")
    else
        q2=${arr[$(($len / 2))]}
    fi
    #echo "Median iterations: $q2; Mean iterations: "$(python -c "print((${str// /+})/$len)")
    echo "Median iterations: $q2; Q3: ${arr[$(($len * 3 / 4))]}"
done
