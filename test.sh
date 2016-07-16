#!/bin/bash

# plant model builder
cd evaluation/plant-synthesis
./cylinder.sh 2>&1
./water-level.sh 2>&1
cd ../..

# Moore builder
cd evaluation/moore-machine-synthesis
./cylinder.sh 2>&1
./water-level.sh 2>&1
cd ../..

# fast Mealy builder
cd examples
./clock-fast.sh 2>&1
./elevator-fast.sh 2>&1
./cash-dispenser-fast.sh 2>&1
./editor-fast.sh 2>&1
./jhotdraw-fast.sh 2>&1
./cvs-fast.sh 2>&1
cd ..

# slow Mealy builder
cd examples
./clock.sh 2>&1
./elevator.sh 2>&1
./editor.sh 2>&1
cd ..

