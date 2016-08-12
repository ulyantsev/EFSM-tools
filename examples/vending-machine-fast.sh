#!/bin/bash
states=4
java -Xmx4G -jar ../jars/fast-automaton-generator.jar vending-machine.sc --ltl vending-machine.ltl --size $states --eventNames START,TOFFEE,CHOC,COIN --actionNames OK,NO,TOFFEE,CHOC --varNames x1 --result generated-fsm.gv --bfsConstraints --globalTree
