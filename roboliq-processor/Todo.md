# Todos

# paper2 Todos

* [ ] process 'output.units' in TecanInfinite2.js
* [x] scripts for weight experiments
* [ ] run weight experiment A (ran it, but need to make changes!)
* [?] run weight experiment B
* [x] run weight experiment C
* [ ] qc02-absorbance
	* [ ] qc02-absorbance-A
		* [x] write script
		* [ ] write analysis
		* [x] run script
		* [ ] run analysis
	* [ ] qc02-absorbance-B
		* [x] write script
		* [x] write analysis
		* [x] run script
		* [ ] run analysis
	* [ ] qc02-absorbance-C
		* [x] write script
		* [.] write analysis
		* [3] run script
		* [ ] run analysis
	* [ ] write overall analysis
	* [ ] run overall analysis
* [ ] unintended dilution
	* [x] write script
	* [1] write analysis
	* [.] run script
	* [ ] run analysis
* [ ] accuracy and precision via absorbance
	* [ ] write script
	* [ ] write analysis
	* [ ] run script
	* [ ] run analysis
* [ ] script for evaporation
	* [ ] write script
	* [ ] write analysis
	* [ ] run script
	* [ ] run analysis
* [ ] script for z-level
	* [2] write script
	* [ ] write analysis
	* [5] run script
	* [ ] run analysis
* [ ] figure out how to associate and copy R files to the '-P' directory
* [ ] run analysis along-side execution
* [ ] display analysis during execution (complication: a single Rmd file for the whole experiment often won't work, because we don't have all measurements until the end)
* [ ] save designs if the '-P' option is given (as json,md,csv?)?

maybe use metalsmith or something simpler (handlerbars with partials, assembler) to generate the html page,
such that it can be built up incrementally as the experiment progresses.
It should also have also have an auto-reloader when the files update, or maybe better would be to trigger Atom IDE to reload it
so that we don't need an extra server running, which could get complicated when executing multiple scripts one after the other.
Steps might be:

* run R script to output CSVs, jpgs, maybe even text
* run a static site generator to create the output HTML
* trigger Atom to load/reload the HTML?

# Todos for Charlotte

* [ ] create Roboliq package for Atom
	* [ ] compile script using config data
	* [ ] display errors
	* [ ] display warnings
	* [ ] display designs
	* [ ] display compilation progress
	* [ ] display various infos about the final script (info that can be formatted as Markdown reasonably well)
	* [ ] possibly try to handle real-time log while the script is executing
* [ ] troubleshoot protocol processing using bablified code
* [ ] use bablified code to run roboliq-runtime-cli, so it executes faster
* [ ] improve error handling/display
* [ ] at beginning of script, display notice to user about what should be on the bench, wait for user to confirm
	* [x] generate HTML file like qc02-abosrbance-B.html
	* [ ] improve HTML
		* [ ] name
		* [ ] date
		* [ ] proper table styling
		* [ ] sort order of stuff
		* [ ] proper table for well contents
		* [ ] tables for designs
		* [ ] script?
	* [ ] open HTML at beginning of script
	* [ ] wait for user to confirm
* [ ] change from '#data' to 'data()'
* [ ] request VB code from the automated liquid class optimization people

# Documentations TODOS

* [ ] generated: separate pages for Commands and Types
* [ ] generated: sort in alphabetical order, but put low-level things later
* [ ] distribution documentation
	* [ ] src/roboliq/README.md
		* [ ] Getting Started
			* [ ] a protocol that doesn't require any configuration or backend
			* [ ] a protocol with a minimal configuration
			* [ ] a protocol with a backend
			* [ ] a protocol compiled for Evoware
			* [ ] running the server and getting the data during execution of an Evoware script
- [ ] user documentation (see <http://usejsdoc.org/about-tutorials.html>)
	- [x] Commands.md: Add general documentation to each command namespace
	- [ ] Commands.md: add examples for each command
	- [ ] generatedTypes: every type should completely define type, description, example
	- [ ] document the properties of the types (i.e. add 'description' field)
	- [ ] WritingADesign.md
	- [ ] Developing_Roboliq.md
	- [ ] Object_Types.md: Add general documentation to top of page
	- [ ] Using_Roboliq.md
		- [ ] add reference to WritingAProtocol.md
	- [ ] WritingAProtocol.md
	- [ ] Cookbook.md: explaining how to solve specific problems
	- [ ] Configuring a lab (e.g. `config/ourlab.js`)
	- [ ] for all commands, include documentation about required logic (e.g. transporter, equipment, pipetter)
	- [ ] convention for syringes being in 'syringe' property of pipetter
	- [ ] pipetter.cleanTips: document the various allowed parameter combinations of equipment, items, syringes, intensity
	- [ ] document the directives, such as "#createPipetteMixtureList"
- [ ] configuration documentation
	- [ ] models
	- [ ] agents
	- [ ] ...
- [ ] code documentation
	- [x] roboliq.js
	- [x] WellContents.js
	- [x] generateSchemaDocs.js
	- [x] commandHelper.js
	- [ ] figure out how to reference an anchor from a separate file (e.g. commands/centrifuge.js should reference 'centrifuge' in Commands.md)
			- continue with centrifuge.js documentaiton header (currently have experiments in there)
	- [ ] generateSchemaDocs.js: set anchors for command modules, so they can be referenced from the source code
	- [ ] generateSchemaDocs.js: why aren't descriptions generated for "Object Types" properties?
	- [ ] for parameter types, can they be links to the Type definition?
	- [ ] check generated jsdoc, and make appropriate improvements (e.g. setting modules and indicating which methods are exported)
	- [ ] generateSchemaDocs.js shouldn't be listed on the Home page of the jsdocs (probably need to remote `@file`)
	- [ ] expectCore.js
	- [ ] expect.js
	- [ ] main.js
	- [ ] misc.js
	- [ ] commands...
	- [ ] document roboliq's extensions to JSON Schema (types, 'module')
- [ ] sort through `notes`, `doc`, and `old` directories

# Bugs

* [ ] in qc02-absorbance-B.yaml, step 4, if we don't include the extra '1' substep, `Volume {{$totalVolume}}` throws and error
* [ ] replace usage of mustache with handlebars
