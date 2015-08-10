#!/bin/bash

tl=300
./evaluate.sh COUNTEREXAMPLE $tl 3 10 false
./evaluate.sh COUNTEREXAMPLE $tl 3 10 true
./evaluate.sh BACKTRACKING $tl 3 10 false
./evaluate.sh BACKTRACKING $tl 3 10 true
./evaluate.sh EXP_SAT $tl 3 10 false
./evaluate.sh EXP_SAT $tl 3 10 true

