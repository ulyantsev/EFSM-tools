#!/bin/bash
/usr/bin/time -f "\t%U user,\t%S system,\t%e elapsed,\t%Mk maxresident" java -Xmx2G -jar ../../jars/continuous-trace-builder.jar --type explicit-state --config ../apros-configurations/modular/combined.conf --dataset dataset_recorded_.bin
