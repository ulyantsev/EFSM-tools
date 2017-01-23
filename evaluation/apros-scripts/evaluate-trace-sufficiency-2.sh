#!/bin/bash
tracemax=$(ls ../plant-synthesis/traces-plant/*.txt | wc -l)
default_folds=5

for ((i = 1; i <= 8; i++)); do
    for ((j = 1; j >= 0; j--)); do
        tracemin=$((tracemax - 100*j))
        fraction=$(python -c "print(float($tracemin)/$tracemax)")
        echo -n "*** S$i : $tracemin traces ***"
        s="0."
        t="0."
        if [[ $j == 0 ]]; then
            folds=1
        else
            folds=$default_folds
        fi
        for ((k = 0; k < $folds; k++)); do
            java -Xmx4G -jar ../../jars/apros-builder.jar --type trace-evaluation --config ../apros-configurations/s"$i".conf --dataset dataset_.bin --traceFraction $fraction 2>/dev/null > .tmp.log
            s="$s + "$(grep "Number of states:" .tmp.log | sed -e 's/^.*: //')
            t="$t + "$(grep "Number of supported transitions:" .tmp.log | sed -e 's/^.*: //')
        done
        echo ": s="$(python -c "print(($s)/$folds)")", t="$(python -c "print(($t)/$folds)")
    done
done
