# Todos for optimization of medium

* [ ] email Fabian to ask about the volumes/concentrations for all the components
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
* mixture of:
	* Nitrogen
	* Phosphate
	* Sulfur
* Vitamin mix
* Trace elements

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
