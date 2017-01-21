#!/bin/bash
conf="$1" # s1, ..., s8
/usr/bin/time -v java -Xmx4G -jar ../../jars/apros-builder.jar --type explicit-state --config ../apros-configurations/"$conf".conf --dataset dataset_.bin
