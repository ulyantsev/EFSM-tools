#!/bin/bash

echo "Generating FSMs, scenarios and LTL formulas..."
mkdir -p testing
rm -f testing/*
for ((size = 2; size <= 10; size++)); do
    for ((events = 2; events <= 5; events++)); do
        for ((actions = 2; actions <= 5; actions++)); do
            name="testing/fsm_${size}s${events}e${actions}a"
            echo $name : creating FSM
            java -jar ../jars/automaton-generator.jar -ac $actions -ec $events -maxa $actions -mina 1 -o $name.dot -s $size -p 100 -vc 0
            echo $name : creating formula
            cmd="TREE_SIZE=7 ./make-formulas -m $name.dot -n $size -f 1 -e $events -i 1 -a $actions -s $actions -v 0 -h 50"
            echo $cmd
            rm -f formulae
            while [ ! -f formulae ]; do
                eval $cmd
            done
            echo "!("`cat formulae`")" > $name-false.ltl
            mv formulae $name-true.ltl
            echo $name : creating scenarios
            for l in 20 40 80 160 320; do
                java -jar ../jars/scenarios-generator.jar -a $name.dot -cnt 10 -minl 1 -suml $l -o ${name}_$l.sc
            done
            echo $name : done
        done
    done
done
