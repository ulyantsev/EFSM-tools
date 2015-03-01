#!/bin/bash

tl=300
./evaluate.sh QSAT $tl 3 3 true true
./evaluate.sh QSAT $tl 3 3 true false
