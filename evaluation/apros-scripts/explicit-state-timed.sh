#!/bin/bash
conf="$1" # s1, ..., s8
/usr/bin/time -f "\t%U user,\t%S system,\t%e elapsed,\t%Mk maxresident" java -Xmx5G -jar ../../jars/apros-builder.jar --type explicit-state --config ../apros-configurations/"$conf".conf --dataset dataset_.bin --timedConstraints
