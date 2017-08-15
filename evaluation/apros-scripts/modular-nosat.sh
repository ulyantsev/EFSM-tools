#!/bin/bash
dir="../apros-configurations/modular"
/usr/bin/time -f "\t%U user,\t%S system,\t%e elapsed,\t%Mk maxresident" java -Xmx4G -jar ../../jars/apros-builder.jar $dir/pressurizer.conf $dir/reactor.conf $dir/upper_plenum.conf $dir/misc.conf --type modular --dataset dataset_recorded_.bin
