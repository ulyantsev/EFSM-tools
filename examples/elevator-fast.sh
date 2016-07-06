#!/bin/bash
states=5
java -Xmx4G -jar ../jars/fast-automaton-generator.jar elevator.sc --ltl elevator.ltl --size $states --eventNumber 5 --eventNames e11,e12,e2,e3,e4 --actionNumber 3 --actionNames z1,z2,z3 --varNumber 0 --result generated-fsm.gv --bfsConstraints --globalTree
