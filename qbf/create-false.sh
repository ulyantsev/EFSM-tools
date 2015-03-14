#!/bin/bash

for ((size = 10; size >= 3; size--)); do
    for compdir in "complete" "incomplete"; do
        for ((instance = 0; instance < 50; instance++)); do
            name="testing/$compdir/fsm-$size-$instance"
            pattern='1s/^\(.*\)$/!(\1)/g'
            cat $name-true.ltl | sed -e "$pattern" > $name-false.ltl
            echo $name : done
        done
    done
done
