#!/bin/bash
states=4
cd .. && ant fast-automaton-generator-jar && cd evaluation && java -Xmx4G -jar ../jars/fast-automaton-generator.jar walkinshaw/editor.sc --ltl walkinshaw/editor.ltl --negsc walkinshaw/editor.negsc --size $states --eventNumber 5 --eventNames load,save,close,exit,edit --actionNumber 0 --varNumber 0 --timeout 1000000 --result generated-fsm.gv --bfsConstraints --globalTree
