#!/bin/bash

res=cfsm.gv
events=5
cnt=10
sc_factor=50
formula_num=5
size=15
compl=false
p=50
l=$((sc_factor * size))

mkdir -p testing-walkinshaw
#LTLPRIORITIES=
LTLPRIORITIES='xor=0, implies=0, equiv=0, not=0, F=0, R=0, U=0, G=1, X=1, and=1, or=1'

for ((instance = 0; instance < 2; instance++)); do
    name="testing-walkinshaw/fsm-$instance"
    ltl="$name-true.ltl"
    echo $name : creating FSM
    java -jar ../jars/automaton-generator.jar -ac 0 -ec $events -maxa 0 -mina 0 -o "$name.dot" -s $size -p $p -vc 0
    echo $name : creating scenarios
    java -jar ../jars/scenarios-generator.jar -a "$name.dot" -cnt $cnt -suml $l -o "$name.sc"
    echo $name : creating formulae
    rm -f "$ltl"
    for ((f = 0; f < formula_num; f++)); do
        rm -f formulae
        while [ ! -f formulae ]; do
            N_CHECK_REPEATS=1 TREE_SIZE=7 ./make-formulas -m $name.dot -n $size -f 1 -e $events -i 0 -a 0 -s 0 -v 0 -p $p -h 100 -u "$LTLPRIORITIES"
        done
        cat formulae >> "$ltl"
    done
    echo $name : done
done
