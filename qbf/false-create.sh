#!/bin/bash

for name in testing/*-true.ltl; do
    lines=$(cat $name | wc -l)
    newname=${name/true/false}
    pattern="${lines}"'s/^\(.*\)$/!(\1)/g'
    cat $name | sed -e "$pattern" > "$newname"
done
