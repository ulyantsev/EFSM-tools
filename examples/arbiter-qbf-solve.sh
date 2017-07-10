#!/bin/bash
states=2
java -Xmx4G -jar ../jars/qbf-automaton-generator.jar arbiter.sc --ltl arbiter.ltl --size $states --eventNumber 1 --actionNames G0,G1 --varNames R0,R1 --result generated-fsm.gv --strategy QSAT --completenessType NORMAL
