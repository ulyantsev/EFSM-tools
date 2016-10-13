#!/bin/bash
states=17
java -Xmx4G -jar ../jars/qbf-automaton-generator.jar ../examples/cvs.sc --ltl ../examples/cvs.ltl --size $states --eventNumber 16 --eventNames setfiletype,initialise,connect,login,changedir,listfiles,logout,disconnect,makedir,delete,appendfile,retrievefile,listnames,rename,storefile,rmdir --actionNumber 0 --varNumber 0 --result generated-fsm.gv --strategy STATE_MERGING
