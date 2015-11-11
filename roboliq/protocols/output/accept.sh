#!/usr/bin/env bash
for file in *.out.json; do 'cp' -f $file $(basename $file .out.json).cmp.json; done
