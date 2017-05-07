# Todos

Pain points:

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
* [ ] figure out why 'npm i' sometimes has EPERM rename permission errors on Windows
* [ ] output warning for unrecognized command parameters
* [ ] documentation: check whether '$' and '$$' are explained
* [ ] get rid of `params` argument to command handlers, because its redundant
* [ ] `shaker.start` or `equipment.start`: implement to allow for other operations to happen during shaking
* [ ] PCR machine
* [ ] generate documentation for evoware commands
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
* [ ] create schema files for Evoware commands, so that they are in the Evoware API documentation

* [ ] rename `Design` to `Data`
* [ ] rename `experiment` to `data`
* [ ] rename `conditions` to `design`

## Secondary

* [ ] improve SCOPE, write tests
	* SCOPE: should contain parameters, variables, data rows
		* `__objects`: access the raw objects
		* `__data`: access the current raw data table
		* `__parameters`: access raw protocol parameters
		* `__step`: access parameters of the current step
		* `__stepStack[]`: access parameters from any step in the current step stack (0 = current step)
		* `__column()`: function to return a column from the current data table
		* `_`: lodash module
		* `math`: mathjs module
		* `*_common`: when a column has a single common value, it's added to the scope with the `_common`-suffix.
* [ ] document predicates required by the various command handlers
* [ ] Design Tables:
	* [ ] `order`: add section, document shuffle, reshuffle, reverse, and repeat
	* [ ] other common action arguments: decimals, units
	* [ ] other top-level parameters for designs, such as `orderBy`
* [ ] make sure templates work like I documented them
* [ ] create some schema for the `data` property somehow, then generate the documentation, or write it manually in the appropriate place
* [ ] predicates: explain the difference between state predicates and tasks/actions
* [ ] generated: sort in alphabetical order, but put low-level things later
* [ ] advanced protocols: make the `data` property documentation more complete
* [ ] document the directives
* [ ] document the format for specifying wells, e.g. `plate1(A01 down D02)`
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
* [ ] Variable references:
	* Future referencing?
		* `$*`: dereference twice
		* `${${@myvar}.somevalue}`
		* `${@myvar.somevalue}`
		* template substitution with ticks
* [ ] allow for setting command defaults:
	* `commandDefaults: [{command: "transporter.*", equipment: "ourlab.mario.roma2"}]`
