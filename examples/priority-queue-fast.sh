#!/bin/bash
states=7
java -Xmx4G -jar ../jars/fast-automaton-generator.jar priority-queue.sc --ltl priority-queue.ltl --size $states --eventNumber 2 --eventNames OFFER,POLL --actionNumber 8 --actionNames z1,z2,z3,z4,z5,z6,z7,z8 --varNumber 4 --result generated-fsm.gv --bfsConstraints --globalTree
