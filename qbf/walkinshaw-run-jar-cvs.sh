#!/bin/bash
states=3
cd .. && ant qbf-automaton-generator-jar && cd qbf && java -ea -Xms2G -Xmx4G -jar ../jars/qbf-automaton-generator.jar walkinshaw/cvs.sc --ltl walkinshaw/cvs.ltl --size $states --eventNumber 15 --eventNames setfiletype,initialise,connect,login,changedirectory,listfiles,logout,disconnect,makedir,delete,appendfile,retrievefile,listfiles,rename,storefile --actionNumber 0 --varNumber 0 --timeout 10000 --result generated-fsm.gv --strategy NEWHYBRID --hybridSecToGenerateFormula 15 --hybridSecToSolve 15 --completenessType NO_DEAD_ENDS --satSolver LINGELING
