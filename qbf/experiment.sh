#!/bin/bash

tl=300
./evaluate.sh EXP_SAT $tl 3 3 true true
./evaluate.sh EXP_SAT $tl 3 3 true false
#./evaluate.sh ITERATIVE_SAT $tl 3 3 true
#./evaluate.sh BACKTRACKING $tl 3 3 true
#./evaluate.sh QSAT $tl 3 3 true