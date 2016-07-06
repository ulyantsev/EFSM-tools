#!/bin/bash
states=17
java -Xmx4G -jar ../jars/qbf-automaton-generator.jar cvs.sc --ltl cvs.ltl --size $states --eventNumber 16 --eventNames setfiletype,initialise,connect,login,changedir,listfiles,logout,disconnect,makedir,delete,appendfile,retrievefile,listnames,rename,storefile,rmdir --actionNumber 0 --varNumber 0 --result generated-fsm.gv --strategy COUNTEREXAMPLE --completenessType NO_DEAD_ENDS --satSolver LINGELING
