#!/bin/bash
states=4
java -Xmx4G -jar ../jars/fast-automaton-generator.jar editor.sc --ltl editor.ltl --negsc editor.negsc --size $states --eventNames load,save,close,exit,edit --result generated-fsm.gv --bfsConstraints --globalTree
