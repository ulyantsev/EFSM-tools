#!/bin/bash
./evaluate.sh HYBRID 60 10 10 true
./evaluate.sh ITERATIVE_SAT 60 10 10 true
./evaluate.sh HYBRID 60 5 5 false
./evaluate.sh ITERATIVE_SAT 60 5 5 false
