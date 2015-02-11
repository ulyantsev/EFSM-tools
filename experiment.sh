#!/bin/bash
echo ITER true
./evaluate.sh ITERATIVE_SAT 60 2 10 true
echo EXP true
./evaluate.sh EXP_SAT 60 2 10 true
echo ITER false
./evaluate.sh ITERATIVE_SAT 60 2 10 false
echo EXP false
./evaluate.sh EXP_SAT 60 2 10 false
