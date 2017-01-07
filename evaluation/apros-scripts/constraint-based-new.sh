#!/bin/bash
/usr/bin/time -v java -jar ../../jars/apros-builder.jar --type constraint-based-new --config ../apros-configurations/s5.conf --dataset dataset_.bin #--disableCur3D --disableCurNext3D --disableCur2D
