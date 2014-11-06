#!/usr/bin/env bash

FILE=tania15_renaturation_merge.esc
RPG='--{ RPG }--'
rm -f $FILE
sed "/$RPG/ q" ~/src/roboliq/source/temp/tania15_renaturation_11/tania15_renaturation_11_mario.esc > $FILE

for file in ~/src/roboliq/source/temp/tania15_renaturation_*/*.esc; do
  echo "Comment(\"`basename $file .esc`\")" >> $FILE
  sed -n "/$RPG/,\$p" $file | grep -v -e "$RPG" >> $FILE
done
