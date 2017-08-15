#!/bin/bash
tracemax=$(ls ../plant-synthesis/traces-plant/ | grep "\\.txt$" | wc -l)
default_folds=10

conf="$1" # s1, ..., s8

if [[ "$conf" == "" ]]; then
    echo "Configuration not specified!"
    exit
fi

avgs=(0 0)
avgt=(0 0)
avgf=(0 0)
diff=$((tracemax / default_folds))
tmp=".tmp.log"
for ((j = 9; j >= 0; j--)); do
    tracemin=$((tracemax - diff*j))
    fraction=$(python -c "print(float($tracemin)/$tracemax)")
    echo -n "*** $conf : $tracemin traces ***"
    s="0."
    t="0."
    f="0."
    if [[ $j == 0 ]]; then
        folds=1
    else
        folds=$default_folds
    fi
    for ((k = 0; k < $folds; k++)); do
        java -Xmx2G -jar ../../jars/apros-builder.jar --type trace-evaluation --config ../apros-configurations/"$conf".conf --dataset dataset_.bin --traceFraction $fraction 2>/dev/null > "$tmp"
        s="$s + "$(grep "Number of states:" "$tmp" | sed -e 's/^.*: //')
        t="$t + "$(grep "Number of supported transitions:" "$tmp" | sed -e 's/^.*: //')
        f="$f + "$(grep "Fraction of supported transitions:" "$tmp" | sed -e 's/^.*: //')
    done
    avgs[$j]=$(python -c "print(($s)/$folds)")
    avgt[$j]=$(python -c "print(($t)/$folds)")
    avgf[$j]=$(python -c "print(($f)/$folds)")
    echo ": s=${avgs[$j]}, t=${avgt[$j]}, f=${avgf[$j]}"
done
echo "  ratio=avg/lst="$(python -c "print( (${avgt[0]} / $tracemax) / ((${avgt[0]} - ${avgt[1]}) / $diff) )")

