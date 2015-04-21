#!/bin/bash

tl=300
./evaluate.sh COUNTEREXAMPLE $tl 3 7 true false
./evaluate.sh COUNTEREXAMPLE $tl 3 5 false false
./evaluate.sh COUNTEREXAMPLE $tl 3 4 true true

#./evaluate.sh COUNTEREXAMPLE $tl 3 4 false false
#./evaluate.sh COUNTEREXAMPLE $tl 3 4 false true
#./evaluate.sh COUNTEREXAMPLE $tl 3 8 true false
#./evaluate.sh COUNTEREXAMPLE $tl 3 6 true true
#./evaluate.sh ITERATIVE_SAT $tl 3 10 true true
#./evaluate.sh ITERATIVE_SAT $tl 3 10 true false
#./evaluate.sh ITERATIVE_SAT $tl 3 10 false true
#./evaluate.sh ITERATIVE_SAT $tl 3 10 false false
#./evaluate.sh HYBRID $tl 3 10 true true
#./evaluate.sh HYBRID $tl 3 10 true false
#./evaluate.sh HYBRID $tl 3 10 false true
#./evaluate.sh HYBRID $tl 3 10 false false
