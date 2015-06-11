#!/bin/bash

events=10
p=25
formula_num=5
size=5
cnt=$size
sc_factor=50
l=$((sc_factor * size))

mkdir -p testing-walkinshaw

for ((instance = 0; instance < 10; instance++)); do
    name="testing-walkinshaw/fsm-$instance"
    echo $name : creating FSM
    java -jar ../jars/automaton-generator.jar -ac 0 -ec $events -maxa 0 -mina 0 -o "$name.dot" -s $size -p $p -vc 0
    echo $name : creating scenarios
    java -jar ../jars/scenarios-generator.jar -a "$name.dot" -cnt $cnt -suml $l -o "$name.sc"
    echo $name : creating formulae
    java -jar ../jars/safety-ltl-generator.jar "$name.dot" > "$name.ltl"
    echo $name : done
done
