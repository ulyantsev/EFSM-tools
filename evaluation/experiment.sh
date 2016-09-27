#!/bin/bash

tl=300
./evaluate.sh QSAT $tl 4 12 true
./evaluate.sh QSAT $tl 3 12 false
#./evaluate.sh EXP_SAT $tl 5 12 false

#./evaluate.sh BACKTRACKING $tl 5 12 false
#./evaluate.sh BACKTRACKING $tl 5 12 true
#./evaluate.sh EXP_SAT $tl 5 12 true
#./evaluate.sh EXP_SAT $tl 5 12 false
#./evaluate-fast.sh $tl 3 12 false
#./evaluate-fast.sh $tl 3 12 true

#./evaluate.sh COUNTEREXAMPLE $tl 11 12 false
#./evaluate.sh COUNTEREXAMPLE $tl 11 12 true
#./evaluate.sh EXP_SAT $tl 11 12 false
#./evaluate.sh EXP_SAT $tl 11 12 true

