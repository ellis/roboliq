for file in protocols/protocol*.json; do
  rm -f protocols/output/$(basename $file .json).out.json
  node src/test.js -O protocols/output $file
done
