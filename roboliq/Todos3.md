# Todos for optimization of medium

* [x] handle proportions of different nitrogen sources
* [ ] plug the design table into the script
* [ ] Fabian: volume of buffer?
* [ ] Fabian: what to use for decontamination wash?  Bleach?
* [ ] Fabian: open the student lab to return scale?
* [ ] Fabian: can we just flush between strain dispenses (then decontam at end), or do we need to decontaminate between with dispense?
* [ ] Fabian: centers3 values?
* [ ] Fabian: rather than 'x' concentrations, we could also use values like `8 mg / L`
* [ ] Fabian: we don't need to shake the plate on the shaker before putting it in the reader, do we?
* [ ] Fabian: reader doesn't like plate with lid, says bad height
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
