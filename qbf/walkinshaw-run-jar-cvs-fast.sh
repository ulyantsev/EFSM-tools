#!/bin/bash
states=10
cd .. && ant fast-automaton-generator-jar && cd qbf && java -Xmx4G -jar ../jars/fast-automaton-generator.jar walkinshaw/cvs.sc --ltl walkinshaw/cvs.ltl --size $states --eventNumber 16 --eventNames setfiletype,initialise,connect,login,changedir,listfiles,logout,disconnect,makedir,delete,appendfile,retrievefile,listnames,rename,storefile,rmdir --actionNumber 0 --varNumber 0 --timeout 1000000 --result generated-fsm.gv --bfsConstraints --globalTree
