#!/usr/bin/env bash

FILE=tania14_renaturation_merge.esc
RPG='--{ RPG }--'
rm -f $FILE
sed "/$RPG/ q" ~/src/roboliq/source/temp/tania14_renaturation_111/tania14_renaturation_111_mario.esc > $FILE
for file in ~/src/roboliq/source/temp/tania14_renaturation_*/*.esc; do
  sed -n "/$RPG/,\$p" $file | grep -v -e "$RPG" >> $FILE
done
