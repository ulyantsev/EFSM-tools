#!/bin/bash
events=4
actions=4
cd .. && ant qbf-automaton-generator-jar && cd qbf
for compl in "complete" "incomplete"; do
    if [[ $compl == "incomplete" ]]; then
        ctype=NO_DEAD_ENDS
    else
        ctype=NORMAL
    fi
    echo "*** $compl ***"
    for ((states = 10; states <= 10; states++)); do
        for ((inst = 38; inst < 50; inst++)); do
            for ((sub = 1; sub <= states; sub++)); do
                if [[ $sub == $states ]]; then
                    res="NOT FOUND"
                else 
                    res=$(java -jar -ea ../jars/qbf-automaton-generator.jar testing/$compl/fsm-$states-$inst.sc --ltl testing/$compl/fsm-$states-$inst-true.ltl --size $(($states - $sub)) --eventNumber $events --actionNumber $actions --timeout 30000 --result generated-fsm.gv --strategy COUNTEREXAMPLE --completenessType $ctype --satSolver LINGELING 2>&1 | grep FOUND)
                fi
                if [[ "$res" =~ "NOT FOUND" ]]; then
                    echo states=$states instance=$inst realstates=$(($states - $sub + 1))
                    break
                fi
            done
        done
    done
done
