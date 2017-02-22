#!/bin/bash
dir="../apros-configurations/modular"
/usr/bin/time -v java -Xmx4G -jar ../../jars/apros-builder.jar $dir/pressurizer.conf $dir/reactor.conf $dir/upper_plenum.conf $dir/misc.conf --type modular --dataset dataset_for_etfa_.bin
