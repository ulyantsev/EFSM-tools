#!/bin/bash

events=5
actions=5
minstates=15
maxstates=15
instances=50

echo "Generating plants and scenarios..."
mkdir -p plants
for ((size = $minstates; size <= $maxstates; size++)); do
    for ((instance = 0; instance < $instances; instance++)); do
        name="plants/plant-$size-$instance"
        echo $name : creating FSM
        java -jar ../../jars/plant-generator.jar -an $actions -mina 1 -maxa 1 -en $events -ip 25 -o "$name.dot" -s $size -mint 1 -maxt 2
        echo $name : creating scenarios
        java -jar ../../jars/plant-scenario-generator.jar -a "$name.dot" -cnt $size -minl $size -maxl $size -o "$name.sc"
    done
done

#exit

echo "Generating LTL properties..."
for ((size = $minstates; size <= $maxstates; size++)); do
    for ((instance = 0; instance < $instances; instance++)); do
        name="plants/plant-$size-$instance"
        ltl="$name.ltl"
        echo $name : creating formulae
        rm -f "$ltl"
        for ((i = 0; i < 50; i++)); do
            TREE_SIZE=7 ./prepare-formulas -a "$name.dot" -n $size -f $size > formula
            # FIXME smth is wrong during the transformation from Spin... sometimes the generated formulae are invalid
            #patchedf="!G(event(${events[(($RANDOM % 5))]})) || ("$(cat formula)")"
            #echo "$patchedf" > formula
            #echo "$patchedf"
            out=$(java -jar ../../jars/verifier.jar -an $actions -en $events --ltl formula -au "$name.dot" -pm 2>/dev/null)
            if [[ "$out" == PASS ]]; then
                passed=0
                for ((j = 0; j < 1; j++)); do
                    othername="plants/plant-$size-$(($RANDOM % $instances))"
                    if [[ "$othername" == "$name" ]]; then
                        (( j-- ))
                        continue
                    fi
                    otherout=$(java -jar ../../jars/verifier.jar -an $actions -en $events --ltl formula -au "$othername.dot" -pm 2>/dev/null)
                    if [[ "$otherout" == PASS ]]; then
                        (( passed++ ))
                    fi
                done
                echo $passed
                if (( passed <= 0 )); then
                    cat formula >> "$ltl"
                    echo generated
                else
                    (( i-- ))
                fi
            else 
                (( i-- ))
            fi
        done
        rm formula
        echo $name : done
    done
done
