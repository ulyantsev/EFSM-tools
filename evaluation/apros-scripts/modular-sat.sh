#!/bin/bash
dir="../apros-configurations/modular"
each=60
#each=1500
/usr/bin/time -f "\t%U user,\t%S system,\t%e elapsed,\t%Mk maxresident" java -Xmx4G -jar ../../jars/continuous-trace-builder.jar $dir/pressurizer.conf $dir/reactor.conf $dir/upper_plenum.conf $dir/misc.conf --type modular --dataset dataset_recorded_.bin --satBased --traceIncludeEach $each
