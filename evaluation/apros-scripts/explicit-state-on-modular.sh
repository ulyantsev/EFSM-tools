#!/bin/bash
/usr/bin/time -f "\t%U user,\t%S system,\t%e elapsed,\t%Mk maxresident" java -Xmx4G -jar ../../jars/apros-builder.jar --type explicit-state --config ../apros-configurations/modular/combined.conf --dataset dataset_for_etfa_.bin
