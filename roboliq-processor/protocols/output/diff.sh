#!/usr/bin/env bash
for file in *.cmp.json; do diff --brief $file $(basename $file .cmp.json).out.json; done
