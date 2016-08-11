#!/bin/bash
states=4
java -Xmx4G -jar ../jars/fast-automaton-generator.jar vending-machine.sc --ltl vending-machine.ltl --size $states --eventNumber 4 --eventNames START,TOFFEE,CHOC,COIN --actionNumber 4 --actionNames OK,NO,TOFFEE,CHOC --varNumber 1 --result generated-fsm.gv --bfsConstraints --globalTree
