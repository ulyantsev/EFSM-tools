#!/bin/bash
each=300
/usr/bin/time -v java -Xmx4G -jar ../../jars/apros-builder.jar --config ../apros-configurations/modular/combined.conf --type explicit-state --dataset dataset_for_etfa_.bin --satBased --traceIncludeEach $each
