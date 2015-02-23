#!/bin/bash

# 5 events, 5 actions, 5 formulae
# 3..10 states, which means 15..50 transitions
# 10 scenarios
# scenario length: 10|S| or 50|S|
# 50 instances for each combination
# expected lime limit per instance: 5 minutes

events=5
actions=5
formula_num=5
cnt=10

echo "Generating FSMs, scenarios and LTL formulae..."
mkdir -p testing
for ((size = 3; size <= 10; size++)); do
    for ((instance = 0; instance < 50; instance++)); do
        name="testing/fsm-$size-$instance"
        if [ -f $name.done ]; then
            continue;
        fi
        
        echo $name : creating FSM
        java -jar ../jars/automaton-generator.jar -ac $actions -ec $events -maxa $actions -mina 0 -o $name.dot -s $size -p 100 -vc 0
        
        echo $name : creating scenarios
        for l in $((10 * size)) $((50 * size)); do
            java -jar ../jars/scenarios-generator.jar -a $name.dot -cnt $cnt -minl 1 -suml $l -o $name-$l.sc
        done
        
        echo $name : creating formulae
        rm -f $name-true.ltl
        cmd="TREE_SIZE=10 ./make-formulas -m $name.dot -n $size -f 1 -e $events -i 0 -a $actions -s $actions -v 0 -h 50"
        for ((formula_num = 1; formula_num <= 5; formula_num++)); do
            rm -f formulae
            echo $cmd
            while [ ! -f formulae ]; do
                eval $cmd
            done
            cat formulae >> $name-true.ltl
        done
        lines=$(cat $name-true.ltl | wc -l)
        pattern="${lines}"'s/^\(.*\)$/!(\1)/g'
        cat $name-true.ltl | sed -e "$pattern" > $name-false.ltl
        
        echo $name : done
        touch $name.done
    done
done
