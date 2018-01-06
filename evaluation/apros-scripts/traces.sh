#!/bin/bash
conf="$1" # s1, ..., s8
java -jar ../../jars/continuous-trace-builder.jar --type traces --config ../apros-configurations/"$conf".conf --dataset dataset_correct_.bin
