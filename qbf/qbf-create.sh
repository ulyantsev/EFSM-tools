#!/bin/bash

echo "Generating FSMs, scenarios and LTL formulae..."
mkdir -p testing
#rm -f testing/*
for ((size = 2; size <= 10; size++)); do
    for ((events = 2; events <= 5; events++)); do
        for ((actions = 2; actions <= 5; actions++)); do
            name="testing/fsm_${size}s${events}e${actions}a"
            if [ -f  $name.done ]; then
                continue;
            fi
            echo $name : creating FSM
            java -jar ../jars/automaton-generator.jar -ac $actions -ec $events -maxa $actions -mina 1 -o $name.dot -s $size -p 100 -vc 0
            echo $name : creating formulae
            cmd="TREE_SIZE=10 ./make-formulas -m $name.dot -n $size -f 1 -e $events -i 1 -a $actions -s $actions -v 0 -h 50"
            rm -f $name-1-true.ltl $name-1-false.ltl
            for ((formula_num = 1; formula_num <= 5; formula_num++)); do
                rm -f formulae
                echo $cmd
                while [ ! -f formulae ]; do
                    eval $cmd
                done
                if (( formula_num > 1 )); then
                    last_num=$(( formula_num - 1 ))
                    cp $name-$last_num-true.ltl $name-$formula_num-true.ltl
                fi
                cat formulae >> $name-$formula_num-true.ltl
            done
            echo $name : creating scenarios
            for cnt in 5 10 20; do
                for l in 20 30 40 60 80 120 160 240; do
                    java -jar ../jars/scenarios-generator.jar -a $name.dot -cnt $cnt -minl 1 -suml $l -o ${name}_${cnt}_$l.sc
                done
            done
            echo $name : done
            touch $name.done
        done
    done
done
./false-create.sh
