for col in 1 3 5; do
  for gfp in 1 2 3 4 5; do
    node tania14_renaturation.js buffer1 $col $gfp >! tania14_renaturation_1$col$gfp.json
    echo tania14_renaturation_1$col$gfp.json
  done
done

for col in 2 4 6; do
  for gfp in 1 2 3 4 5; do
    node tania14_renaturation.js buffer2 $col $gfp >! tania14_renaturation_2$col$gfp.json
    echo tania14_renaturation_2$col$gfp.json
  done
done
