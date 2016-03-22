#!/bin/bash
states=9
cd .. && ant fast-automaton-generator-jar && cd evaluation && java -Xmx4G -jar ../jars/fast-automaton-generator.jar case-instances/cash-dispenser.sc --ltl case-instances/cash-dispenser.ltl --size $states --eventNumber 14 --eventNames IC,EC,A,AE,AS,C,CR,CP,M,MR,MS,ME,MP,CNL --actionNumber 13 --actionNames z1,z2,z3,z4,z5,z6,z7,z8,z9,z10,z11,z12,z13 --varNumber 0 --timeout 100000 --result generated-fsm.gv --bfsConstraints --globalTree
