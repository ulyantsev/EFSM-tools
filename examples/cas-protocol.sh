#!/bin/bash
states=6
java -Xmx4G -jar ../jars/qbf-automaton-generator.jar cas-protocol.sc --ltl cas-protocol.ltl --size $states --eventNames IncomingCall,AnsweringCall,RejectingCall,CallReleased,CallConnected,Disconnected --result generated-fsm.gv --strategy COUNTEREXAMPLE --completenessType NO_DEAD_ENDS --satSolver LINGELING
