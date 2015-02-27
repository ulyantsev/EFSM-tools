#!/bin/bash

compdir="complete"
p=100
events=5
cnt=10
sc_factor=50

#for ((size = 10; size >= 3; size--)); do
for size in 10; do
    l=$((sc_factor * size))
    
    for ((instance = 0; instance < 50; instance++)); do
        name="testing/$compdir/fsm-$size-$instance"
        echo $name : creating scenarios
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
    done
done
