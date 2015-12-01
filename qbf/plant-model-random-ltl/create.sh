#!/bin/bash

res=cfsm.gv
events=5
actions=5

echo "Generating plants, scenarios and LTL formulae..."
mkdir -p plants
for ((size = 5; size <= 20; size++)); do
    for ((instance = 0; instance < 50; instance++)); do
        name="plants/plant-$size-$instance"
        ltl="$name.ltl"
        #echo $name : creating FSM
        #java -jar ../../jars/plant-generator.jar -an $actions -ap 25 -en $events -ip 25 -o "$name.dot" -s $size -tp 25
        #echo $name : creating scenarios
        #java -jar ../../jars/plant-scenario-generator.jar -a "$name.dot" -cnt $((size * 2)) -minl $size -maxl $((size * 2)) -o "$name.sc"

        echo $name : creating formulae
        rm -f "$ltl"
        for ((i = 0; i < $size; i++)); do
            TREE_SIZE=7 ./prepare-formulas -a "$name.dot" -n $size -f $size > formula
            # FIXME smth is wrong during the transformation from Spin... sometimes the generated formulae are invalid
            #cat formula
            #java -jar ../../jars/verifier.jar -an $actions -en $events --ltl formula -au "$name.dot" -pm
            out=$(java -jar ../../jars/verifier.jar -an $actions -en $events --ltl formula -au "$name.dot" -pm 2>/dev/null) 
            #echo $out
            if [[ "$out" == "PASS" ]]; then
                cat formula >> "$ltl"
            else 
                (( i-- ))
            fi
        done
        rm formula
        echo $name : done
    done
done
