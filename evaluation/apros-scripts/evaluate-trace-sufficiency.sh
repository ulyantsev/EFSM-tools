#!/bin/bash
tracemax=$(ls ../plant-synthesis/traces-plant/*.txt | wc -l)
tracemin=$((tracemax - 100))
fraction=$(python -c "print(float($tracemin)/$tracemax)")

for ((i = 1; i <= 8; i++)); do
    echo "*** S$i : $tracemin traces ***"
    java -Xmx4G -jar ../../jars/apros-builder.jar --type explicit-state --config ../apros-configurations/s"$i".conf --dataset dataset_.bin --traceFraction $fraction 2>/dev/null > .tmp.log
    grep "Input coverage:" .tmp.log
    grep "Output coverage:" .tmp.log
    grep "Number of states:" .tmp.log
    grep "Number of supported transitions:" .tmp.log
    echo "*** S$i : $tracemax traces ***"
    java -Xmx4G -jar ../../jars/apros-builder.jar --type explicit-state --config ../apros-configurations/s"$i".conf --dataset dataset_.bin 2>/dev/null > .tmp.log
    grep "Input coverage:" .tmp.log
    grep "Output coverage:" .tmp.log
    grep "Number of states:" .tmp.log
    grep "Number of supported transitions:" .tmp.log
    echo
done
