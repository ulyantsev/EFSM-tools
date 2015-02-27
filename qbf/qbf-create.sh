#!/bin/bash

compl=$1
if [[ $compl == "true" ]]; then
    compdir="complete"
    p=100
else
    compdir="incomplete"
    p=50
fi

res=cfsm.gv
events=5
actions=5
formula_num=5
cnt=10

sc_factor=50

echo "Generating FSMs, scenarios and LTL formulae..."
mkdir -p "testing/$compdir"
for ((size = 10; size >= 3; size--)); do
    l=$((sc_factor * size))
    
    for ((instance = 0; instance < 50; instance++)); do
        name="testing/$compdir/fsm-$size-$instance"
        ltl="$name-true.ltl"
        while true; do
            echo $name : creating FSM
            java -jar ../jars/automaton-generator.jar -ac $actions -ec $events -maxa $actions -mina 0 -o "$name.dot" -s $size -p $p -vc 0
            echo $name : creating scenarios
            if [[ $compl == "true" ]]; then
                sed_expr=
                for ((line = 2; line <= $events * $size * 2; line++)); do
                    rnum=$(shuf -i 0-3 -n 1)
                    if (( rnum < 3 )); then
                        sed_expr="$sed_expr${line}p;"
                    fi
                done
                cat "$name.dot" | grep "-" | sed -ne "$sed_expr" > dummy.dot
                java -jar ../jars/scenarios-generator.jar -a dummy.dot -cnt $cnt -suml $l -o "$name.sc"
                rm dummy.dot
            else
                java -jar ../jars/scenarios-generator.jar -a "$name.dot" -cnt $cnt -suml $l -o "$name.sc"
            fi

            echo $name : creating formulae
            rm -f "$ltl"
            cmd="TREE_SIZE=10 ./make-formulas -m $name.dot -n $size -f 1 -e $events -i 0 -a $actions -s $actions -v 0 -p $p -h 50"
            for ((formula_num = 1; formula_num <= 5; formula_num++)); do
                rm -f formulae
                echo $cmd
                while [ ! -f formulae ]; do
                    eval $cmd
                done
                cat formulae >> "$ltl"
            done
            
            if [[ $compl == "true" ]]; then
                break
            else
                echo $name : checking
                java -jar ../jars/sat-builder.jar "$name.sc" --result $res --size $size 2>/dev/null
                verified=$(java -jar verifier.jar $res $size "$ltl" | wc -l)
                formula_num=$(cat "$ltl" | wc -l)
                rm -f $res
                if [[ $verified != $formula_num ]]; then
                    echo ">>>>>>>>>>>>>>>>>>>>>>>>>>" HARD s=$size n=$instance
                    break
                else
                    echo ">>>>>>>>>>>>>>>>>>>>>>>>>>" EASY s=$size n=$instance
                fi
            fi
        done
        lines=$(cat "$ltl" | wc -l)
        pattern="${lines}"'s/^\(.*\)$/!(\1)/g'
        cat $name-true.ltl | sed -e "$pattern" > $name-false.ltl
        echo $name : done
    done
done
