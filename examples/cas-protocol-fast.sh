#!/bin/bash
states=6
java -Xmx4G -jar ../jars/fast-automaton-generator.jar cas-protocol.sc --ltl cas-protocol.ltl --size $states --eventNames IncomingCall,AnsweringCall,RejectingCall,CallReleased,CallConnected,Disconnected --result generated-fsm.gv --bfsConstraints --globalTree
