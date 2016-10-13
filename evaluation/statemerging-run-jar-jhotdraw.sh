#!/bin/bash
states=7
java -Xmx4G -jar ../jars/qbf-automaton-generator.jar ../examples/jhotdraw.sc --ltl ../examples/jhotdraw.ltl --size $states --eventNumber 6 --eventNames figure,text,setpos,edit,setdim,finalise --actionNumber 0 --varNumber 0 --result generated-fsm.gv --strategy STATE_MERGING
