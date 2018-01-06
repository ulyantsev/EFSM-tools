#!/bin/bash
each=300
/usr/bin/time -f "\t%U user,\t%S system,\t%e elapsed,\t%Mk maxresident" java -Xmx4G -jar ../../jars/continuous-trace-builder.jar --config ../apros-configurations/modular/combined.conf --type explicit-state --dataset dataset_recorded_.bin --satBased --traceIncludeEach $each
