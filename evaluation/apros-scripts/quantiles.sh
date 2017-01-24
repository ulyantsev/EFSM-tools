#!/bin/bash
conf="$1" # s1, ..., s8
java -Xmx4G -jar ../../jars/apros-builder.jar --type quantiles --config ../apros-configurations/"$conf".conf --dataset dataset_.bin
