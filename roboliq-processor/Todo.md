# Todos

Running newest QC scripts 2017-06-18:

Highest level:

* [ ] create a controlled error - bad liquid class
* [ ] detect errors
	* [ ] this can be done with qc31-absorbance
	* [ ] it would be nicer to do it with qc21-general
* [ ] correct errors

High-level:

* [ ] one weighing experiment to get average dispense volume of 150ul, and perhaps other volumes too
* [ ] one absorbance experiment to get bias, variance, and dilution of smaller volumes
* [ ] a general checking protocol, such as qc21-general?

Todos:

* [x] qc31-absorbance: run it on Wet 3-15ul
* [x] create bad liquid class: duplicate Roboliq_Water_Wet_1000, adjust for not enough air gap, negative bias towards 15ul
	* Roboliq_Water_Wet_1000_BADTEST
	* changes to 3-15ul subclass:
		* system trailing airgap: 10ul => 1ul
		* leading air gap: 10ul => 0ul
		* calibration offset: 0.2ul => 0.8ul
		* calibration factor: 1.045 => 0.7
* [ ] make qc31-absorbance-bad script to test bad liquid class
	* [ ] make modified Rmd file to use the new class
	* [ ] make modified yaml to load the new data tables
	* [ ] run it
	* [ ] analyze it
* [ ] qc31-absorbance: analyze Wet 3-15ul

Re-running QC scripts (outdated):

* [ ] qc21-general
	* [x] support shaking in reader commands
	* [x] re-run the measurement steps, this time with shaking in the reader
	* [x] push data to git repo
	* [ ] analyze results
* [ ] qc23-dilution
	* [x] create wellData
	* [x] create simData
	* [x] create script for AW
	* [x] test analysis of simulated output (AW)
	* [x] create script for D
	* [x] test analysis of simulated output (D)
	* [ ] run script
		* [x] run from line 567, step 1.4.1.3.4.1 "Aspirate and re-dispense 30 times (step 4 of 6)"
		* [x] run from 1.4.3.3.3.1, first dispense done, and first wash done, from line 1932
		* [ ] run from 6437
	* [ ] re-write analysis to account for the constant volume drop + dilution;
		in the case of no dilution, absorbance would drop approximately linearly (aside from beam effects)
	* [ ] analyze results
	* [ ] extend Stan model for analysis
	* [ ] add testing of dilution for volumes >150ul?
* [ ] qc22-accuracy
	* [?] create scale command
	* [ ] bsse-mario: scale equipment
	* [ ] bsse-mario: scale site
	* [ ] bsse-mario: user transporter
	* [ ] bsse-mario: user transporter definitions, to move plate from bench sites to scale site
	* [ ] make it possible for user to move labware to scale
	* [ ] create a user implementation for the scale command
	* [ ] create an evoware VisualBasic script for getting measurement from user, or use the evoware user dialog
	* [ ] take the user's measurement and save it to a measurement file
* [ ] test: write a script to dispense low dye volumes and get a desired amount in the well, correcting for volume bias and dilution
	* [ ] measure empty wells
	* [ ] need to know/determine/verify alpha_k of the dye so that we know how much absorbance to expect from 3, 5, 7, 10, 15ul
	* [ ] dispense 2 x 150ul dye into several wells, and use dilution and accuracy parameters from previous experiments to estimate alpha_k
	* [ ] for each $d$ level, pick how many times to dispense it - 10 x 3ul, 6 x 5ul, 4 x 7ul, 3 x 10ul, 2 x 15 ul, so ~30ul of dye will be in each well
	* [ ] select several wells to just contain water
	* [ ] the remaining wells are all assigned a $d$ value, with nested factors for uncorrected volume and corrected volume
	* [ ] dispense 270ul water to all empty wells
	* [ ] measure plate
	* [ ] dispense dye volumes as assigned
	* [ ] measure wells

Speed Optimizations and Niceties:

* [ ] For calls to `mathjs.eval`, find the identifiers in the expression first and only add those variables to the scope (like done in `commandHelpers.js:calculateWithMathjs`)
* [ ] refactor to pass `context` object around instead of `data`, `DATA`, and `SCOPE`, etc.
* [ ] only update object predicates when objects change
* [ ] only call `getCommonValues()` when DATA changes
* [ ] refactor to just use nodejs without babel (to let us avoid the compile step)
* [ ] think about freezing objects, so that we don't need to clone them
* [ ] try http://github.com/agershun/alasql for sql queries on data, especially in order to support table joins

# Pain points:

* doesn't have the toolbox to solve certain problems alone.
* getting started guide that walks through all the tools and covers about 80% of what you need to know
* maybe an exercise or two
* designs: how do they work, when do you use them, what are the limitations
* many confusing things:
	* different functions available in the designs than in the commands
* interactive design table thing
	* perhaps like a python interpreter, and it shows you what it would do

# Electron GUI

* [x] handle separate protocolBase, protocol, protocolComplete
	* [x] load Oskari's YAML file as `protocol` and example_protocol_output.json as `protocolComplete`
	* [x] Protocol.jsx: only display objects in `protocol`
	* [x] Protocol.jsx: only display steps in `protocol`
	* [x] fix schema in step 35 so that 'groupBy' is not displayed in red
	* [x] remove everything up to last "step" in path before displaying step ID
* [x] load Lena's script and make sure it displays OK
	* [x] display protocol description
	* [x] output YAML instead of JSON for unknown values
	* [x] display parameters
* [x] add [Cancel] button to JsonEditor
* [?] make JsonEditor a YamlEditor
* [x] main.js: command line for setting the protocols to be loaded
* [x] add menus:
	* Menu.setApplicationMenu() https://electron.atom.io/docs/api/menu/
	* https://www.christianengvall.se/electron-menu/
* [ ] add menu option to switch between main and output protocols (see mainmenu.js)
* [ ] send edits back to main.js to save
* [ ] main.js: load config protocols using roboliq lib
* [ ] main.js: compile protocols and load `protocolComplete`
* [ ] let user add an object
	* [ ] dropdown list for user to select a type
* [ ] let user add a step
* [ ] add "Find" functionality
* [ ] load Lena's script and make sure it displays OK
	* [ ] design1
	* [ ] give param column a sufficient fixed width and let its values take the rest of the width
* [ ] EK01-test.yaml: look into why light washing of small tips doesn't work (it's probably not configured)
* [ ] EK01-test.yaml: make a `pipetter.discard` or `pipetter.aspirate` command

# Things needed for Oskari's yeast script, part 1 on mario

* [x] lid handling
	* [x] `transporter._moveLidFromContainerToSite` roboliq code
	* [x] `transporter.moveLidFromContainerToSite` roboliq code
	* [x] `transporter._moveLidFromSiteToContainer` roboliq code
	* [x] `transporter.moveLidFromSiteToContainer` roboliq code
	* [x] `transporter._moveLidFromContainerToSite` evoware code
	* [x] `transporter._moveLidFromSiteToContainer` evoware code
* [x] stacking plates via lid handling
* [x] make sure all tests succeed
* [x] better error reporting for Lena's example of missing plate reader info
* [x] better error reporting for Oskari's scripts that are currently failing
	* [x] `npm start -- --progress -P ../compiled --evoware ../testdata/bsse-mario/Carrier.cfg,../testdata/bsse-mario/OV_TranformationTestNew_20170111.ewt,ourlab.mario.evoware ../config/bsse-mario.js ../protocols/yeast-transformation-cells.yaml`
* [x] improve the script documentation format
	* [x] mark required parameters
* [x] look at Oskari's requests in Email from 2017-03-09 (e.g. documentation)
* [x] move 'api-generator' output to `docs/protocol`
* [x] remove the extra 'container' or 'origin' parameters from the high level moveLid commands, since the lid location already tells us that information
* [-] figure out why 'npm i' sometimes has EPERM rename permission errors on Windows
* [x] output warning for unrecognized command parameters
* [x] setup warning suppression
	* [x] `object1 and object2 are of different labware models; this may be problematic for centrifugation`
* [ ] get rid of `params` argument to command handlers, because its redundant
* [ ] `shaker.start` or `equipment.start`: implement to allow for other operations to happen during shaking
* [ ] PCR machine
* [ ] consider adding display information to the sites, and generate an SVG of the initial bench setup
* [ ] allow for dynamic bench configuration: let user specify that P4 is a PCR site
* [ ] in `yeast-transformation-complete`, why is the labware information not generated for the centrifuge site (we had to manually put the labware onto the centrifuge in the table file)?

Would be nice:

* [ ] tubes
* [ ] troughs
* [ ] print a table of the labware models and sites in `bsse-mario.js`, for reference while writing a script
* [ ] `data` accepts a `jmespath` argument for running jmespath queries http://jmespath.org/

# Next experiment with Charlotte

* [ ] repeat experiment 1, but with correction of Dry=>Air dispense (and maybe also at the end fill wells to 250ul and re-read)
* [ ] need to consider how to measure/estimate alpha_k more reliably
* [ ] higher dye volumes

# For Lena

* [ ] experiment with `~/src/extern/ng2-json-editor` for configuration and script editing
* [ ] use evoware table file for the grid indexes, rather than using the values in the config file
* [ ] when errors are in the protocol, open a browser window at the beginning of the ESC file to show the errors

# Get it working for colleagues

* [x] on Charlotte's computers, `npm test` fails in `WellContents flattenContents`
* [x] make sure `npm i` works on a clean checkout in the root directory
* [ ] look into why the design table randomization seed is ignored when using `npm run design`
* [ ] for update to jsdoc, see https://github.com/jsdoc3/jsdoc/issues/1185#issuecomment-279895756
* [ ] api-generator: pull schemas in automatically to contents/schemas (right now it contains old copies)
* [ ] Protocol tutorial and Writing-a-Design: link to YAML tutorial as a pre-requisite
* [ ] autogenerate the API documentation when `npm i` is run from the root, link to API docs from the README
	* [ ] roboliq-evoware: create a README so that index.html isn't blank
	* [ ] roboliq-processor: rename "Roboliq Source" to "Roboliq Processor"
	* [ ] roboliq-processor: fix link to roboliq-evoware
	* [ ] api: figure out why index.html is basically blank
	* [ ] create top-level README with links to the other modules
* [ ] try to get `npm run processor` to work from the root directory for more complex scripts
* [ ] `npm install` instead of `npm i`
* [ ] describe in general the actions that happen by default (such as rinsing at certain times, movements that require multiple steps or opening devices)
* [ ] Writing-a-Design.md: remove `---` from the yaml files
* [ ] remove design*Test.yaml
* [ ] add repository field to root package.json
* [ ] check out why compiling from the root didn't update `npm run design`
* [ ] BUG: `npm run design` -- ignores random seed
* [ ] Document functions in design.js
* [ ] Document HACK & TRICKS: such as the "Plate" type for tube holders and Troughs, and setting "contents" property for troughs to treat as a single well
* [ ] for pipetter.pipette command, if volumes are unitless, there should be a better error message
* [ ] roboliq.yaml: add descriptions to fields on models
* [ ] api-generator: indicate whether properties are required or not
* [ ] make sure that well parser documentation is somewhere findable
* [ ] pipetter.pipette: better error message when `sources` is mispelled as `source`
* [ ] roboliq.js: validate protocol, check that initial locations are valid, e.g. no A03 on a tube holder
* [ ] check that the initial models are valid?  E.g., we told it that T1 had the 50ml tubes, but T1 can only hold 15ml tubes
* [ ] document the predicates and how the logic works
* [ ] bsse-mario.js: let it be a function that can take configuration options for PCR holders and the P1/DOWNHOLDER

flattenArrayAndIndexes:

[{}] ; [0]

a*: 2
[{}] ; [0] =>
	[{}, {}] ; [0,1] => [{a: 1}, {a: 2}] ; [0,1]
[[{a: 1}, {a: 2}]] ; [0]
[{a: 1}, {a: 2}] ; [0,1]

b*: 2
[{a: 1}, {a: 2}] ; [0,1] =>
	[{a: 1}, {a: 1}] ; [0,1] => [{a: 1, b: 1}, {a: 1, b: 2}] ; [0,1]
	[{a: 2}, {a: 2}] ; [0,1] => [{a: 2, b: 1}, {a: 2, b: 2}] ; [0,1]
[[{a: 1, b: 1}, {a: 1, b: 2}], [{a: 2, b: 1}, {a: 2, b: 2}]] ; [0,1]
[{a: 1, b: 1}, {a: 1, b: 2}, {a: 2, b: 1}, {a: 2, b: 2}] ; [0,1,2,3]

# Lena & Daniel

* [x] make sure that fluorescenceReader works like absorbanceReader
* [x] figure out how to substitute well into fluorescenceReader programFile
* [x] EK 384 greiner flat bottom and EK 96 well Greiner Black
* [x] make sure LK01-test.yaml compiles
* [x] setup P1DOWNHOLDER
* [ ] fix total volume calculation
* [ ] test out with overriding the parameters
* [ ] figure out simulation equation
* [ ] extract time-series measurement data from the fluorescence reader (put it in `measurements.jsonl`)

# paper2 Todos

* [x] process 'output.units' in TecanInfinite2.js
* [ ] qc01-accuracy
	* [ ] have roboliq ask for weight measurements and save them, rather than doing all this stuff manually
	* [ ] qc01-accuracy-003
		* [?] write script
		* [ ] run script
	* [ ] qc01-accuracy-016
		* [x] write script
		* [?] run script
	* [x] qc01-accuracy-501
		* [x] write script
		* [x] run script
	* [x] write overall analysis
	* [x] run analysis
* [ ] qc02-absorbance
	* [x] qc02-absorbance-A
		* [x] write script
		* [x] write analysis
		* [x] run script
		* [x] run analysis
	* [x] qc02-absorbance-B
		* [x] write script
		* [x] write analysis
		* [x] run script
		* [x] run analysis
	* [x] qc02-absorbance-C
		* [x] write script
		* [x] write analysis
		* [x] run script
		* [x] run analysis
	* [x] write overall analysis
	* [x] run overall analysis
	* [ ] re-run overall analysis after repeating qc02-absorbance-A to correct for incorrect factor data
* [ ] qc03-dilution: unintended dilution
	* [x] write script
	* [x] write analysis
	* [x] run script
	* [x] run analysis
	* [ ] fix reader bug (missed absorbance of diluted wells A11 block E12) and re-run
* [ ] qc04-accuracy-dye
	* [x] write script
	* [.] write analysis
	* [x] run script
	* [ ] run analysis
* [x] qc05-zlevel
	* [x] write script
	* [x] write analysis
	* [x] run script
	* [x] run analysis
* [ ] script for evaporation
	* [.] write script
	* [.] write analysis
	* [ ] run script
	* [ ] run analysis
* [ ] figure out how to associate and copy R files to the '-P' directory
* [ ] trigger analysis during execution after measurements
* [ ] display analysis during execution (complication: a single Rmd file for the whole experiment often won't work, because we don't have all measurements until the end)
* [ ] save designs if the '-P' option is given (as json,md,csv?)?
* [ ] should include the 'model' in all of the designs where 'm' is part of a calculated parameter
	* or maybe it should be generated by the measurements, automatically including such data as site, model, syringe, tipModel, etc.
* [ ] handle `.rblq` files which hold command lines to execute, can generate multiple scripts?  Also include this information in the `.out.json` files

maybe use metalsmith or something simpler (handlerbars with partials, assembler) to generate the html page,
such that it can be built up incrementally as the experiment progresses.
It should also have also have an auto-reloader when the files update, or maybe better would be to trigger Atom IDE to reload it
so that we don't need an extra server running, which could get complicated when executing multiple scripts one after the other.
Steps might be:

* run R script to output CSVs, jpgs, maybe even text
* run a static site generator to create the output HTML
* trigger Atom to load/reload the HTML?

Variable substitution:

* SCOPE `$`
* DATA `$$`
* mathjs `` $`..` ``, or maybe `$(...)` would be better
* javascript `${}`
* handlebars `$|...|` for inline, `{$||: ...}` for object substitution
* javascript string substitution `` $`..` `` (if we don't use this for mathjs)
* Variables in `objects`: `$@`
* protocol parameters `$#`
* command parameters: `$^` for this command, `$^^` for the parent command, etc?
* directives: `$#` e.g. `$#length(data)` or `$#length:`

See misc.js:renderTemplateString, since another convention is used there

# Todos for Charlotte

* [ ] create Roboliq package for Atom
	* [ ] compile script using config data
	* [ ] display errors
	* [ ] display warnings
	* [ ] display designs
	* [ ] display compilation progress
	* [ ] display various infos about the final script (info that can be formatted as Markdown reasonably well)
	* [ ] possibly try to handle real-time log while the script is executing
* [4] troubleshoot protocol processing using bablified code
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
	* [x] open HTML at beginning of script
	* [x] wait for user to confirm
* [?] change from '#data' to 'data()'
* [ ] request VB code from the automated liquid class optimization people

# Documentations TODOS

## Version 1

* [x] generate Evoware API documentation
* [x] generated: handle `$ref`s in `EvowareconfigSpec.yaml`
* [x] generated: handle `oneOf`s in `EvowareconfigSpec.yaml`
* [x] book: figure out how to add appropriate links to the API documentation
* [x] draft chapter: configuration
	* [x] objectToPredicateConverters
	* [x] planHandlers
* [x] draft chapter: simple protocols
	* [x] split chapter into simple & advanced protocols
	* [x] objects
	* [x] steps
* [x] draft chapter: advanced protocols
	* [x] parameters
	* [x] objects
	* ----- new day
	* [x] data
		* [x] Data type
		* [x] data property
		* [x] data commands
		* [x] data() directive
	* [x] scope
	* [x] substitution
	* [x] $-substitution
	* [x] template substitution
	* [x] directives
* [x] replace mustache with handlebars
* [x] make sure the jsdoc is generated for roboliq-processor and roboliq-evoware
* [x] generate a top-level index.html somehow that links to manual/index.html, protocol/index.html, roboliq-processor/index.html, roboliq-evoware/index.html
* [x] book: exclude unfinished chapters for now
* [x] draft chapter: design tables
	* [x] plain factors, `*`-factors, array factors
	* [x] hidden factors
	* [x] actions/functions
* [x] create schema files for Evoware commands, so that they are in the Evoware API documentation
* [x] rename `Design` type to `Data`
* [x] rename `experiment` module to `data`
* [x] rename `conditions` to `design`

## Secondary

* [x] improve SCOPE, write tests
	* SCOPE: should contain parameters, variables, data rows
		* [x] `__objects`: access the raw objects
		* [x] `__data`: access the current raw data table
		* [x] `__parameters`: access raw protocol parameters
		* [x] `__step`: access parameters of the current step
		* [x] `__column()`: function to return a column from the current data table
		* [x] `*_ONE`: when a column has a single common value, it's added to the scope with the `_ONE`-suffix.
		* [x] entire DATA column by name, getting rid of \$\$
* [x] run `rsync -rv docs ellisw@intra.csb.ethz.ch:/local0/www/html/roboliq/`
* [x] change parsing of singular values when they are passed an array so that if each entry has the same value, use that value -- this way we can avoid the `$*_ONE` suffix in commands.
* [x] update documentation for the new SCOPE
* [x] SCOPE for `${}` javascript substitution
	* [x] `_`: lodash module
	* [x] `math`: mathjs module
* [ ] make sure templates work like I documented them (see also in the Design Tables chapter the last example with templates and nested steps, do I need the \$-prefix?  I think I saw it used somewhere, probably in unit tests.)
* [ ] add examples to all commands
* [ ] Design Tables:
	* [ ] `order`: add section, document shuffle, reshuffle, reverse, and repeat
	* [ ] other common action arguments: decimals, units
	* [ ] other top-level parameters for designs, such as `orderBy`
* [ ] create some schema for the `data` property somehow, then generate the documentation, or write it manually in the appropriate place
* [ ] document the predicates required by the various command handlers (e.g. transporer.movePlate requires a bunch of transport-related predicates)
* [ ] predicates: explain the difference between state predicates and tasks/actions
* [ ] generated: sort in alphabetical order, but put low-level things later
* [ ] document the directives, using JSON Schema
* [ ] document the format for specifying wells, e.g. `plate1(A01 down D02)`
* [ ] document `protocol.config.suppressWarnings`
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

# Important internal refactoring

* [ ] roboliq.js: improve the whole context/data thing in the calls to `expand*` functions
	* [ ] remove PARAMETERS, SCOPE and DATA from objects, put them in the context instead
	* [ ] pass context to `expand*` functions, stop passing all those other variables
	* [ ] create a commandHelpers function to create a new context for new step based on the old context and the new step
	* [ ] remove all these extra references in the context to things that are already in the protocol
* [ ] Make a complete and validatable schema for Protocol

# Bugs

* [x] qc05-zlevel: need to automatically include variables for DETECTED_VOLUME_? in the evoware script
* [ ] absorbanceReader: 'output.units' not handled in TecanInfinite2.js
* [ ] absorbanceReader: For a full plate minus F11 down H11, A11 cross E12 get skipped in the reader!
* [ ] in qc02-absorbance-B.yaml, step 4, if we don't include the extra '1' substep, `Volume {{$totalVolume}}` throws and error
* [ ] BUG: Compiling qc01-accuracy-tubes-003.yaml with dist crashes, whereas running with babel-node works
* [ ] BUG: transporter.doThenRestoreLocation: can't use `$scopeVariable` in `objects` array
* [?] BUG: transporter.doThenRestoreLocation: `equipment` wasn't used when transferring plate back to original position
* [ ] BUG: qc02-absorbance-A: in the last 'absorbanceReader.measurePlate' command, if the command isn't made into a *numbered* substep, compilation crashes when processing 'simulated'.

> ERROR:
>
>	Running this with babel-node from the roboliq-processor directory works:
>
>	`npm start -- -T --progress src/config/ourlab.js ../protocols/qc01-accuracy-tubes.yaml --print-designs -P 'C:\ProgramData\Tecan\EVOware\database\scripts\Ellis\EvowareScripts' --evoware ../testdata/bsse-mario/Carrier.cfg,../testdata/bss e-mario/NewLayout_Feb2015.ewt,ourlab.mario.evoware`
>
>	But from the root directory, this doesn't work:
>
>	`npm run processor -- -T --progress roboliq-processor/dist/config/ourlab.js protocols/qc01-accuracy-tubes.yaml --print-designs -P 'C:\ProgramData\Tecan\EVOware\database\scripts\Ellis\EvowareScripts' --evoware testdata/bsse-mario/Carrie r.cfg,testdata/bsse-mario/NewLayout_Feb2015.ewt,ourlab.mario.evoware`

# Todos

* [ ] designTest: unit test for calculateRow
* [ ] SCOPE
	* [ ] `__stepStack[]`: access parameters from any step in the current step stack (0 = current step)
* [ ] Variable references:
	* Future referencing?
		* `$*`: dereference twice
		* `${${@myvar}.somevalue}`
		* `${@myvar.somevalue}`
		* template substitution with ticks
* [ ] allow for setting command defaults:
	* `commandDefaults: [{command: "transporter.*", equipment: "ourlab.mario.roma2"}]`
