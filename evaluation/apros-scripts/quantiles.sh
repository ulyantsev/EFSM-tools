#!/bin/bash
conf="$1" # s1, ..., s8
java -Xmx2G -jar ../../jars/apros-builder.jar --type quantiles --config ../continuous-trace-configurations/"$conf".conf --dataset dataset_.bin
