#!/bin/bash

format="\t%U user,\t%S system,\t%e elapsed,\t%Mk maxresident"

conf="$1"

case "$conf" in
    s1) each=5 ;;
    s2) each=25 ;;
    s3) each=25 ;;
    s4) each=50 ;;
    s5) each=100 ;; # ???
    s6) each=5 ;;
    s7) each=5 ;;
    s8) each=50 ;;
    *) echo "Unsupported configuration!"; exit ;;
esac

/usr/bin/time -f "$format" java -Xmx4G -jar ../../jars/continuous-trace-builder.jar --type explicit-state --satBased --config ../apros-configurations/"$conf".conf --dataset dataset_.bin --traceIncludeEach $each
