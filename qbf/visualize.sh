#!/bin/bash
cat generated-fsm.gv | sed -e 's/\[1\]//g; s/()//g' > tmp.gv
dot -Tsvg tmp.gv -o out.svg
rm tmp.gv
eog out.svg &
