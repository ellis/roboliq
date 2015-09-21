#!/usr/bin/env bash

FILE=tania14_renaturation_merge.esc
RPG='--{ RPG }--'
rm -f $FILE
sed "/$RPG/ q" ~/src/roboliq/source/temp/tania14_renaturation_111/tania14_renaturation_111_mario.esc > $FILE

#for file in ~/src/roboliq/source/temp/tania14_renaturation_*/*.esc; do

for file in \
/home/ellisw/src/roboliq/source/temp/tania14_renaturation_111/tania14_renaturation_111_mario.esc \
tania12 \
/home/ellisw/src/roboliq/source/temp/tania14_renaturation_112/tania14_renaturation_112_mario.esc \
/home/ellisw/src/roboliq/source/temp/tania14_renaturation_113/tania14_renaturation_113_mario.esc \
/home/ellisw/src/roboliq/source/temp/tania14_renaturation_114/tania14_renaturation_114_mario.esc \
/home/ellisw/src/roboliq/source/temp/tania14_renaturation_115/tania14_renaturation_115_mario.esc \
tania12 \
/home/ellisw/src/roboliq/source/temp/tania14_renaturation_151/tania14_renaturation_151_mario.esc \
/home/ellisw/src/roboliq/source/temp/tania14_renaturation_152/tania14_renaturation_152_mario.esc \
/home/ellisw/src/roboliq/source/temp/tania14_renaturation_153/tania14_renaturation_153_mario.esc \
/home/ellisw/src/roboliq/source/temp/tania14_renaturation_154/tania14_renaturation_154_mario.esc \
tania12 \
/home/ellisw/src/roboliq/source/temp/tania14_renaturation_155/tania14_renaturation_155_mario.esc \
/home/ellisw/src/roboliq/source/temp/tania14_renaturation_241/tania14_renaturation_241_mario.esc \
/home/ellisw/src/roboliq/source/temp/tania14_renaturation_242/tania14_renaturation_242_mario.esc \
/home/ellisw/src/roboliq/source/temp/tania14_renaturation_243/tania14_renaturation_243_mario.esc \
tania12 \
/home/ellisw/src/roboliq/source/temp/tania14_renaturation_244/tania14_renaturation_244_mario.esc \
/home/ellisw/src/roboliq/source/temp/tania14_renaturation_245/tania14_renaturation_245_mario.esc \
/home/ellisw/src/roboliq/source/temp/tania14_renaturation_261/tania14_renaturation_261_mario.esc \
/home/ellisw/src/roboliq/source/temp/tania14_renaturation_262/tania14_renaturation_262_mario.esc \
tania12 \
/home/ellisw/src/roboliq/source/temp/tania14_renaturation_263/tania14_renaturation_263_mario.esc \
/home/ellisw/src/roboliq/source/temp/tania14_renaturation_264/tania14_renaturation_264_mario.esc \
/home/ellisw/src/roboliq/source/temp/tania14_renaturation_265/tania14_renaturation_265_mario.esc \
; do
  echo "Comment(\"`basename $file .esc`\")" >> $FILE
  if [[ "$file" == "tania12" ]]; then
    echo "Subroutine(\"C:\\ProgramData\\Tecan\\EVOware\\database\\Scripts\\Ellis\\tania12_denaturation_5_once_mario.esc\",0);" >> $FILE
  else
    sed -n "/$RPG/,\$p" $file | grep -v -e "$RPG" >> $FILE
  fi
done
