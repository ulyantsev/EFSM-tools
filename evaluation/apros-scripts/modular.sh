#!/bin/bash
each=75
#each=4000
/usr/bin/time -v java -Xmx4G -jar ../../jars/apros-builder.jar ../apros-configurations/modular/pressurizer.conf ../apros-configurations/modular/reactor.conf ../apros-configurations/modular/upper_plenum.conf ../apros-configurations/misc.conf --type modular --dataset dataset_.bin --satBased --traceIncludeEach $each
