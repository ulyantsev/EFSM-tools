#!/bin/bash

mkdir -p iteration-plots
rm -f iteration-plots/*.plot

for compdir in "complete" "incomplete"; do
    for prefix in FAST*; do
        for ((s = 3; s <= 12; s++)); do
            echo ">>> $compdir; s = $s"
            for ((i = 0; i < 50; i++)); do
                sat_size=$(cat "eval"/$compdir/$prefix-$s-$i/size);
                iter=$(grep "ITERATIONS" "eval"/$compdir/$prefix-$s-$i/$sat_size.log | sed 's/.* //g')
                time=$(grep "execution time" "eval"/$compdir/$prefix-$s-$i/$sat_size.log | sed 's/.* //g')
                echo $time $iter >> iteration-plots/$compdir-$s.plot
            done
        done
    done
done
