#!/bin/bash
/usr/bin/time -v java -Xmx4G -jar ../../jars/apros-builder.jar --type explicit-state --config ../apros-configurations/s8.conf --dataset dataset_.bin --traceFraction 1
