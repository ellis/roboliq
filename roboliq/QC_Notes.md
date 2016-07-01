# Distribution of $a$ for $d=0$ and various $v$

* $P[a|v;d=0]$, where $v$ is only known approximately
* `qc_mario_absorbance1`: 0, 50, 100, 150, 200, 250, 300 (just water, 3 plates)
* Start with empty wells, and cycle 6 times, each time adding 50ul to all wells and measuring absorbance.

# Relative variance of $a$ for $d=const$ and various $v$

* $Var[a|v;d=const]$, where $v$ is only known approximately
* `qc_mario_absorbance2`: v=50, 100, 150, 200, 250, 300
* Distribute various volumes of water, distribute aliquot of dye to obtain a medium absorbance level, then 6 times: shake and measure absorbance

# Relative variance of $a$ for various $d$ and $v=const$

* `qc_mario_pipetting7`:

# Relative calibration curve for absorbance: map %conc to %a

* `qc_mario_pipetting6b`: this worked very well, using the small tips.  Demonstrated affine linearity between .2 and 2 above base level.
* `qc_mario_pipetting6c`: this worked very well, using the small tips.  Demonstrated affine linearity between .04 and .4 above base level.

# $Var(a;d,tip)$

* `qc_mario_evaporation5`: 150, 300 (large tips, dry dispense)
* `qc_mario_pipetting1`: ?
* `qc_mario_pipetting2`: ?
* `qc_mario_pipetting5b`: 5, 10, 20, 40, 60, 80, 100, 150 (large tips, wet and air dispense)

TODO:

* something like `qc_mario_pipetting5b`, but for small volumes and small tips
* something like `qc_mario_pipetting5b`, but for medium volumes and small tips

Also, but not necessarily the best:

* `qc_mario_pipetting5`: 3, 4, 5, 7, 10, 20, 30, 40 (large and small tips, wet and air dispense); problem is that there's a lot more variation in absorbance due to the different amount of dye attracted by the small vs large tips.

# $E[v;d]$

	* large, dry
		* `qc_mario_absorbance3`: 75, 150
		* `qc_mario_evaporation5`: 150, 300
		* `qc_mario_weight1_tube_5_large`: 5
	* small, wet: 1, 3?, 5?, 10?, 0.5?, 0.2?, 0.1?
		* [.] `qc_mario_pipetting8`: 1
		* `qc_mario_weight1_tube_5_small`: 5

# $Var(v;d,tip)$

We assume that the relative standard deviation of the volume is equal to the
relative standard deviation of absorbance.  This is a conservative estimate,
because the absorbance reader adds addition variance.  As long as the reader
variance is reasonably small, this should not be a problem.

If need be, we could use multiple plates, fill them with water from the trough,
and weigh them, using volume replicates in order to estimate the variance.
But this will only work for volumes that are sufficiently large to be reliably
weighed, and due to the manual weighing step and plate requirements, it
probably shouldn't be done too much.

## C.I. $E[v;d,tip]$
