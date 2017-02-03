#!/bin/bash
conf="$1" # s1, ..., s8
/usr/bin/time -v java -jar ../../jars/apros-builder.jar --type constraint-based-new --config ../apros-configurations/"$conf".conf --dataset dataset_.bin --disableCurNextOutputs --disableCurNext2D 
