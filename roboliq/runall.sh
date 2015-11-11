#!/usr/bin/env bash
for file in protocols/protocol*.json protocols/*.yaml; do
  rm -f protocols/output/$(basename $file .json).out.json
  ./node_modules/.bin/babel-node src/main.js -O protocols/output $file
done
cd protocols/output
./diff.sh
