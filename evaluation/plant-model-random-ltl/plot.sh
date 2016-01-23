#!/bin/bash

dir=evaluation

for size in 10 15 20 25 30; do
    echo "***size: $size;"
    str=$(echo $(grep -nr "execution time" $dir/$size-*/*.full.log | sed -e "s/^.*time: //g" | sort -n))
    echo $str
    str=$(echo $(grep -nr "ITERATIONS:" $dir/$size-*/*.full.log | sed -e "s/^.*ITERATIONS: //g" | sort -n))
    echo $str
done
