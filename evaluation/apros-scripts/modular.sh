#!/bin/bash
/usr/bin/time -v java -Xmx5G -jar ../../jars/apros-builder.jar ../apros-configurations/pressurizer.conf ../apros-configurations/reactor.conf ../apros-configurations/misc.conf --type modular --dataset dataset_correct_.bin
