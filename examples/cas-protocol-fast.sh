#!/bin/bash
states=6
java -Xmx4G -jar ../jars/fast-automaton-generator.jar cas-protocol.sc --ltl cas-protocol.ltl --size $states --eventNumber 6 --eventNames IncomingCall,AnsweringCall,RejectingCall,CallReleased,CallConnected,Disconnected --actionNumber 0 --varNumber 0 --result generated-fsm.gv --bfsConstraints --globalTree
