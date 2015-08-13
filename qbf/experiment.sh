#!/bin/bash

tl=300
./evaluate.sh COUNTEREXAMPLE $tl 11 12 false
./evaluate.sh COUNTEREXAMPLE $tl 11 12 true
./evaluate.sh BACKTRACKING $tl 11 12 false
./evaluate.sh BACKTRACKING $tl 11 12 true
./evaluate.sh EXP_SAT $tl 11 12 false
./evaluate.sh EXP_SAT $tl 11 12 true

