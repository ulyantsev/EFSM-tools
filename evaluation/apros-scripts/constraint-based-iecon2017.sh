#!/bin/bash
conf="$1" # s1, ..., s8
/usr/bin/time -f "\t%U user,\t%S system,\t%e elapsed,\t%Mk maxresident" java -jar ../../jars/apros-builder.jar --type constraint-based --config ../apros-configurations/"$conf".conf --dataset dataset_for_etfa_.bin --constraintBasedDisableOIO_CONSTRAINTS --constraintBasedDisableMONOTONIC_FAIRNESS_CONSTRAINTS --constraintBasedDisableCOMPLEX_FAIRNESS_CONSTRAINTS
