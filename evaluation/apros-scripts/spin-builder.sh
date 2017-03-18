#!/bin/bash
conf="$1" # s1, ..., s8
/usr/bin/time -v java -Xmx5G -jar ../../jars/apros-builder.jar --type explicit-state-completion-with-loops --output "promela,nusmv" --config ../apros-configurations/"$conf".conf --dataset dataset_for_etfa_.bin
