#!/bin/bash
states=18
java -Xmx4G -jar ../jars/fast-automaton-generator.jar cvs.sc --ltl cvs.ltl --size $states --eventNames setfiletype,initialise,connect,login,changedir,listfiles,logout,disconnect,makedir,delete,appendfile,retrievefile,listnames,rename,storefile,rmdir --result generated-fsm.gv --bfsConstraints --globalTree 
