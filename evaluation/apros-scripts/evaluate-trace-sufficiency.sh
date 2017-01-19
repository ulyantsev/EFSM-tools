#!/bin/bash
for ((i = 1; i <= 8; i++)); do
    echo "*** S$i 90% ***"
    java -Xmx4G -jar ../../jars/apros-builder.jar --type explicit-state --config ../apros-configurations/s"$i".conf --dataset dataset_.bin --traceFraction 0.9 2>/dev/null > .tmp.log
    grep "Input coverage:" .tmp.log
    grep "Output coverage:" .tmp.log
    grep "Number of states:" .tmp.log
    grep "Number of supported transitions:" .tmp.log
    echo "*** S$i 100% ***"
    java -Xmx4G -jar ../../jars/apros-builder.jar --type explicit-state --config ../apros-configurations/s"$i".conf --dataset dataset_.bin --traceFraction 1.0 2>/dev/null > .tmp.log
    grep "Input coverage:" .tmp.log
    grep "Output coverage:" .tmp.log
    grep "Number of states:" .tmp.log
    grep "Number of supported transitions:" .tmp.log
    echo
done
