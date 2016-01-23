#!/bin/bash

problem=$1
strategy=$2

if [[ $problem == "clock" ]]; then
    echo CLOCK
    minstates=3
    maxstates=3
    eventnum=4
    events=A,T,H,M
    actionnum=7
    actions=z1,z2,z3,z4,z5,z6,z7
    varnum=2
    compl=NORMAL
elif [[ $problem == "elevator" ]]; then
    echo ELEVATOR
    minstates=1
    maxstates=5
    eventnum=5
    events=e11,e12,e2,e3,e4
    actionnum=3
    actions=z1,z2,z3
    varnum=0
    compl=NO_DEAD_ENDS
elif [[ $problem == "cash-dispenser" ]]; then
    echo CASH DISPENSER
    minstates=1
    maxstates=9
    eventnum=14
    events=IC,EC,A,AE,AS,C,CR,CP,M,MR,MS,ME,MP,CNL
    actionnum=13
    actions=z1,z2,z3,z4,z5,z6,z7,z8,z9,z10,z11,z12,z13 
    varnum=0
    compl=NO_DEAD_ENDS
else
    echo ERROR
    exit
fi

cd .. && ant qbf-automaton-generator-jar && cd qbf 
rm iterate-states.log

for ((i = $minstates; i <= $maxstates; i++)); do
    echo $i states...
    java -Xms2G -Xmx4G -jar ../jars/qbf-automaton-generator.jar case-instances/$problem.sc --ltl case-instances/$problem.ltl --size $i --eventNumber $eventnum --eventNames $events --actionNumber $actionnum --actionNames $actions --varNumber $varnum --timeout 100000000 --result generated-fsm.gv --strategy $strategy --completenessType $compl --satSolver LINGELING --qbfSolver DEPQBF 2>> iterate-states.log
done
