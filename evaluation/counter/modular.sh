#!/bin/bash
./serialize-datasets.sh
/usr/bin/time -f "\t%U user,\t%S system,\t%e elapsed,\t%Mk maxresident" java -Xmx5G -jar ../../jars/apros-builder.jar s.conf m.conf h.conf --type modular --dataset dataset_.bin
