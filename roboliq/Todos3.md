# Todos for optimization of medium

* [x] handle proportions of different nitrogen sources
* [x] Fabian: what to use for decontamination wash?  Bleach? A: Yes
* [x] trace1 needs to be added second-to-last
* [x] plug the design table into the script
* [x] Fabian: centers3 values?
	lower box:
	glucose: 2%-4% (1x - 2x)
	nitrogen1: 1x-2x
	nitrogen2: 0-3x
	trace1: 1x-2x
	trace2: 0-3x

	higher box:
	glucose: 1.5x - 3x (3% - 6%)
	nitrogen1: 1.5x-4x
	nitrogen2: 1-4x
	trace1: 1.5x-4x
	trace2: 1-4x
* [x] Fabian: reader doesn't like plate with lid, says bad height; Gregor says just run it and indicate lid-less
* [x] fix gc_mario_screening2.mdfx so that it doesn't loop
* [x] reserve robot for the weekend / respond to Gabrielle
* [x] between strain dispenses, no flushing; but decontam before and end
* [x] test absorbanceReader with new `output` property and shaking and stuff
* [x] generate with lots of loops, so that it continues over the weekend
* [x] automatically copy recording data to network drive, so I can look at it over the weekend
* [ ] Fabian: start the cooling system
* [ ] Fabian: the reader program only has one heating setting, not a separate one for the top?

* [ ] write EvowareCompilerTest for aspirating from single well and dispensing in adjacent wells
* [ ] design2.js: add functionality for joining independent designs column-wise
* [ ] design2.js: add functionality for joining independent designs column-wise
* [ ] design2.js: add functionality for selecting partial factorial design
* [ ] design2.js: add functionality for randomly selecting a number of rows
* [ ] design2.js: add functionality for selecting rows in a d-optimal way

# Experiment

*Fixed input factor*:

* Strain (we'll probably only work with a single strain for now?)

*Quantitative input factors*.  The following media components will have varying concentrations.

* Buffer
* Glucose
* Nitrogen
* mixture of:
	* Phosphate
	* Sulfur
	* Trace elements
* Vitamin mix

Components will contain antibiotics to avoid contamination.

*Output responses*:

* OD600
* Cell count


# Various design notes

<http://www.itl.nist.gov/div898/handbook/pri/pri.htm>
<http://www.itl.nist.gov/div898/handbook/pri/section3/pri33.htm>

* Need to screen for problematic syringes, tip models, liquid classes, and sites
	* Fractional factorial or Plackett-Burman, each at the various volumes we need to test
* Need to estimate parameters for our models for d', a, and z
	* regression design
* Need to find the optimal amounts of media compounds
	* response surface: Central composite or Box-Behnken

Reading: Box, ANOVA p138

# Specification format

```{yaml}
factors:
  buffer:
    range: []
* Buffer
* Glucose
* mixture of:
	* Nitrogen
	* Phosphate
	* Sulfur
* Vitamin mix
* Trace elements

design:
bufferVolume*: [5ul,]
glucoseVolume*: [ ]
```
