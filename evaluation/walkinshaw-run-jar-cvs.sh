#!/bin/bash
states=17
cd .. && ant qbf-automaton-generator-jar && cd evaluation && java -ea -Xms2G -Xmx4G -jar ../jars/qbf-automaton-generator.jar walkinshaw/cvs.sc --ltl walkinshaw/cvs.ltl --size $states --eventNumber 16 --eventNames setfiletype,initialise,connect,login,changedir,listfiles,logout,disconnect,makedir,delete,appendfile,retrievefile,listnames,rename,storefile,rmdir --actionNumber 0 --varNumber 0 --timeout 1000000 --result generated-fsm.gv --strategy COUNTEREXAMPLE --completenessType NO_DEAD_ENDS --satSolver LINGELING
