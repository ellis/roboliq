for col in 1 2 3 4 5; do
  for gfp in 1 2 3 4 5; do
    node tania15_renaturation.js buffer1 $col $gfp >! tania15_renaturation_$col$gfp.json
    echo tania15_renaturation_$col$gfp.json
  done
done
