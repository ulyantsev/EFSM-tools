#!/bin/bash

strategy=$1
timeout=$2
min_size=$3
max_size=$4
compl=$5

if [[ $compl == "true" ]]; then
    compdir="complete"
    compcmd="NORMAL"
else
    compdir="incomplete"
    compcmd="NO_DEAD_ENDS"
fi

echo "Compiling..."
cd .. && ant qbf-automaton-generator-jar && cd evaluation
echo "Evaluating..."

fsm="generated-fsm.gv"
events=4
actions=4

mkdir -p "eval/$compdir"

for ((size = $min_size; size <= $max_size; size++)); do
    for ((instance = 0; instance < 50; instance++)); do
        ev_name="eval/$compdir/$strategy-$size-$instance"
        if [ -f $ev_name/done ]; then
            echo skipping $ev_name
            continue
        fi
        rm -rf $ev_name
        mkdir -p $ev_name
        name=testing/$compdir/fsm-$size-$instance
        sc_name=$name.sc
        instance_description="s=$size n=$instance"
        ltl_name=$name-true.ltl
        minsize=$(java -jar ../jars/max-clique-finder.jar $sc_name | head -n 1 | sed -e 's/.* //')
        for ((trysize = $minsize; trysize <= $size; trysize++)); do
            echo ">>> $instance_description: $trysize"
            rm -f $fsm
            java -Xmx4G -jar ../jars/qbf-automaton-generator.jar "$sc_name" \
                --ltl "$ltl_name" --size $trysize --eventNumber $events --actionNumber $actions \
                --timeout $timeout --result "$fsm" --strategy $strategy --completenessType $compcmd \
                2>&1 | cat > $ev_name/$trysize.full.log
            grep "\\(INFO\\|WARNING\\|SEVERE\\|Exception\\|OutOfMemoryError\\)" < $ev_name/$trysize.full.log > $ev_name/$trysize.log
            if [[ $(grep "\\(TIME LIMIT EXCEEDED\\|UNKNOWN\\|OutOfMemoryError\\)" < $ev_name/$trysize.log) != "" ]]; then
                # unknown
                echo $trysize > $ev_name/size
                touch $ev_name/unknown $ev_name/done
                break
            elif [ -f $fsm ]; then
                # sat
                echo $trysize > $ev_name/size
                touch $ev_name/found $ev_name/done
                cp $fsm $ev_name/$fsm
                break
            fi  # else continue
        done
    done
done
