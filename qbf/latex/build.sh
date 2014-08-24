#!/bin/bash
latex reduction.tex && pdflatex reduction.tex
rm reduction.log reduction.dvi reduction.aux
