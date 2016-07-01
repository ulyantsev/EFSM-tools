#!/bin/bash

#java -Xmx4G -jar ../../jars/apros-builder.jar --type sat-based --config ../apros-configurations/s1.conf --dataset dataset_.bin --traceIncludeEach 5
#java -Xmx4G -jar ../../jars/apros-builder.jar --type sat-based --config ../apros-configurations/s2.conf --dataset dataset_.bin --traceIncludeEach 25
#java -Xmx4G -jar ../../jars/apros-builder.jar --type sat-based --config ../apros-configurations/s4.conf --dataset dataset_.bin --traceIncludeEach 50
#java -Xmx4G -jar ../../jars/apros-builder.jar --type sat-based --config ../apros-configurations/s7.conf --dataset dataset_.bin --traceIncludeEach 5
#java -Xmx4G -jar ../../jars/apros-builder.jar --type sat-based --config ../apros-configurations/s8.conf --dataset dataset_.bin --traceIncludeEach 50
java -Xmx4G -jar ../../jars/apros-builder.jar --type sat-based --config ../apros-configurations/s3.conf --dataset dataset_.bin --traceIncludeEach 25
