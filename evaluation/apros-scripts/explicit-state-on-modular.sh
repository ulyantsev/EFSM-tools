#!/bin/bash
/usr/bin/time -v java -Xmx4G -jar ../../jars/apros-builder.jar --type explicit-state --config ../apros-configurations/modular/combined.conf --dataset dataset_for_etfa_.bin
