#!/bin/bash
states=7
cd .. && ant fast-automaton-generator-jar && cd evaluation && java -Xmx4G -jar ../jars/fast-automaton-generator.jar walkinshaw/jhotdraw.sc --ltl walkinshaw/jhotdraw.ltl --size $states --eventNumber 6 --eventNames figure,text,setpos,edit,setdim,finalise --actionNumber 0 --varNumber 0 --timeout 1000000 --result generated-fsm.gv --bfsConstraints --globalTree
