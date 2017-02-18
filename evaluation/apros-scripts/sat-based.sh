#!/bin/bash

/usr/bin/time -v java -Xmx4G -jar ../../jars/apros-builder.jar --type explicit-state --satBased --config ../apros-configurations/s1.conf --dataset dataset_.bin --traceIncludeEach 5
#/usr/bin/time -v java -Xmx4G -jar ../../jars/apros-builder.jar --type explicit-state --satBased --config ../apros-configurations/s2.conf --dataset dataset_.bin --traceIncludeEach 25
#/usr/bin/time -v java -Xmx4G -jar ../../jars/apros-builder.jar --type explicit-state --satBased --config ../apros-configurations/s4.conf --dataset dataset_.bin --traceIncludeEach 50
#/usr/bin/time -v java -Xmx4G -jar ../../jars/apros-builder.jar --type explicit-state --satBased --config ../apros-configurations/s7.conf --dataset dataset_.bin --traceIncludeEach 5
#/usr/bin/time -v java -Xmx4G -jar ../../jars/apros-builder.jar --type explicit-state --satBased --config ../apros-configurations/s8.conf --dataset dataset_.bin --traceIncludeEach 50
#/usr/bin/time -v java -Xmx4G -jar ../../jars/apros-builder.jar --type explicit-state --satBased --config ../apros-configurations/s3.conf --dataset dataset_.bin --traceIncludeEach 25
