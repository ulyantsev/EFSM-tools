#!/bin/bash
java -jar ../jars/qbf-automaton-generator.jar "testing-daniil/50n/nstates=5/10/plain-scenarios" --size 5 --eventNumber 2 --eventNames A,B --actionNumber 2 --actionNames z0,z1 --varNumber 2 --timeout 100000 --result generated-fsm.gv --strategy BACKTRACKING --backtrackingErrorNumber 2
