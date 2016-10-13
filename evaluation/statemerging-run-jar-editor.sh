#!/bin/bash
states=4
java -Xmx4G -jar ../jars/qbf-automaton-generator.jar ../examples/editor.sc --ltl ../examples/editor.ltl --size $states --eventNumber 5 --eventNames load,save,close,exit,edit --actionNumber 0 --varNumber 0 --result generated-fsm.gv --strategy STATE_MERGING
