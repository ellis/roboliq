for file in *.cmp.json; do echo $file; diff $file $(basename $file .cmp.json).out.json; done
