# Todos

# Top

- check ROBOSEAL on mario
- work on setting up luigi
- incubator command and methods
- finish mario's QC scripts for testing ROMAs
- design for Box examples
- Figure out how to automatically convert reader data to measurement JSON data
- UI: monitor
- UI: load and compile
- UI: design

# Todos for Paper 1

- [x] add 'roboliq' version to output
- [x] get running of protocols from command line to work again, and document it
- [x] main.js: show help if nothing is passed on the command line
- [x] main.js: fix `--ourlab` help description
- [x] make sure tests run again
- [x] make sure that the scala program runs again
- [x] BUG: track down bad construction of WellContents
- [x] babel-ify for ES6 features
- [x] write test for construction of well contents during mixing
- [x] create WellContents.js with functions from pipetterUtils.js
    - [x] create a function for adding contents, take code from pipetterUtils/getEffects_pipette
    - [x] refactor pipetterUtils/getEffects_pipette using WellContents.js
- [x] change WellContents.unknownVolume to infinity, and retest
- [x] implement `tania12` scripts for denaturation
    - [x] create single protocol for all of the steps that used to be separated
    - [x] create `#pipetteMixtures` directive
    - [x] create `centrifuge.insertPlates2` command
    - [x] try to compile for evoware
- [x] implement `tania15` scripts for renaturation
    - [x] node protocols/tania15_renaturation.js, use `__dirname`
    - [x] errors aren't indicating the command they came from (e.g. movePlate & site/destination, something with programFile)
    - [x] merge protocols together rather than appending them!
- [x] for object of type `Variable`, process a `calculate` property as an alternative to setting the value to a directive.
- [x] change directive handling to accommodate merging of lab-specific protocol
    - [x] object variables should have directives parsed before parsing steps, not immediately after loading
    - [x] directives can have an 'override' property, whose value gets merged into the result of the directive
    - [x] directives in steps are processed as they are encountered
    - [x] directives in command parameters are processed before being passed to the command handler
- [x] handle JSON Patch files (as opposed to merge-able files)
- [x] handle '?'-suffix and '!'-suffix properties in objects and steps
- [x] split `tania*` protocols into portable vs lab data
    - [x] tania13_ph: make combined protocol, at first lab-specific
    - [x] `./node_modules/.bin/babel-node -- src/main.js -p --progress -r protocols/tania13_ph-spec.yaml`
    - [x] tania13_ph: split combined protocol into portable and lab-specific parts
    - [x] tania13_ph-ourlab-diff
    - [x] tania12_denaturation
- [x] if an object has both ! and non ! properties, the ! property should take precedence
- [x] `npm test`: fix problem with variable that references a source
    - [x] node_modules/.bin/mocha --compilers js:babel/register tests/wellsParserTest.js
    - [x] node_modules/.bin/mocha --compilers js:babel/register tests/pipetterTest.js
- [x] `diff tania12_denaturation.out.json tania12_denaturation-new.out.json > x`
- [x] pipetterTest: re-enable all the tests
- [x] command documentation:
    - [x] create schemas for all commands (JSON/YAML structured documentation)
    - [x] move all schemas to YAML files
    - [x] generate documentation from schemas
        - [x] write code to do the generation
        - [x] create an npm command for generating docs
        - [x] install git version of jsdoc and try to get it to work with our ES6 code
        - [x] put the command docs somewhere where jsdoc will pick it up
    - [x] generateCommandSpecDocs.js: load yaml files instead of command handler files
    - [x] change all schemas to use roboliq types, rather than just JSON Schema types (continue with sealer.yaml)
    - [x] use schemas to parse the params before passing them to the commandHandler
    - [x] pipetterUtils.getEffects_pipette: line 38 (items are parsed)
    - [x] checkout fluorescenceReaderTest, because the relevant ourlab.js command handlers haven't been updated yet to use schemas
    - [x] refactor all commands to accept parsed parameters, using the schemas
    - [x] remove all uses of commandHelper.parseParams()
    - [x] rename commandHelper.parseParams2()
- [x] commandSpec: pipetter.cleanTips: define intensity (intensity is a predefined enum)
- [x] protocols/protocol4.json: pipetter.AspirateItem should maybe have 'well' property instead of 'source'...
- [x] move around protocols/tania* files so that ./runall.sh works
- [x] commandSpec: pipetter: need to declare some more pipetter typedefs, such as pipetter.AspirateItem
- [x] schemas/objects.yaml: add schemas for object types
- [x] validate `protocol.objects` using object schemas
- [x] enforce all objects in protocol.objects to have a type (see roboliq.js:validateProtocol1())
- [x] commandHelperTest.js: test array of variables of integers
- [x] generate documentation for object schemas
- [x] rename commandSpec to schema everywhere
- [x] refactor: can I remove parsers/sourceParser?
- [x] create commandHelper.parseParams test for misspelled `sources` specifier (e.g. removing `balanceWater` from tania13)
- [x] schemas: pipetter.pipetteMixtures.order: should be an enum
- [x] change `parseParams` function to return a {objectNames, values}
    - [x] figure out problem with processOneOrArray as currently testing in commandHelperTest
    - [x] pipetterPipetteTest.js
    - [x] centrifugeTest.js
- [x] can `name` parameter be removed from commandHelper functions such as `processValue0AsEnum`?
- [x] refactor usage of commandHelper.getParsedValue
    - [x] figure out why we needed to `_.clone(value1)` -- what is mutating the value?
    - [x] run `npm test` on branch lookupValue2
    - [x] remove commandHelper.lookupValue (currently fixing processParamsBySchema())
    - [x] finish documenting commandHelper.getParsedValue
- [x] look at `~/src/roboliq/roboliq/protocols/output/diff.sh`
    - [x] handle `destinationAfter1: null` for `centrifuge.centrifuge2`: make new type of "SiteOrStay"
    - [x] fix error in protocols/protocol1_ph.yaml
- [x] fixup pipetter.js to not hardcode our `syringesAvailable` and `tipModelToSyringes`
    - [x] create `pipetter.cleanTips|ourlab.mario.evoware|ourlab.mario.liha` command handler
    - [x] change `pipetter._cleanTips` to `pipetter._washTips`
    - [x] `./node_modules/.bin/mocha --compilers js:babel/register tests/pipetterTest.js`
    - [x] `pipetter.cleanTips` should perform logical query to figure out which sub-command to call (e.g. `pipetter.cleanTips|ourlab.mario.evoware|ourlab.mario.liha`)
    - [x] test for clean all tips
    - [x] delete protocol8.json, because the tests are in pipetterTest.js
    - [x] can probably get rid of `pipetter.cleanTips.canAgentEquipmentProgramModelIntensity` logic and program it directly in `pipetter.cleanTips|ourlab.mario.evoware|ourlab.mario.liha`
    - [x] add tips 5-8 to ourlab.js
- [x] copy fixed mixtures specification from protocols/protocol1_ph.yaml to paper1 supporting information
- [x] `pipetter._washTips` `_aspirate` `_dispense` should update syringe state
    - [x] `_pipette`: append contaminants from source well to syringe
    - [x] `_washTips`: used 'parsed' instead of 'params' for the syringe names
    - [x] `_aspirate`: add contents, append contaminants in source well, remove `cleaned`
    - [x] `_pipette`: clear syringe.cleaned
    - [x] `_washTips`: remove contents, remove contaminants, set `cleaned`
    - [x] `_washTips`: should not set cleaned state on syringe if there was no change (see e.g. pipetterPipetteTest, first test)
    - [x] `_dispense`: remove contents, append contaminants in dest well if syringe touches
        - [x] test contents=null when dispensing all syringe contents
        - [x] getEffects_dispense: need to track syringe content state through iterations, in case of multi-dispense
        - [x] test contamination upon wet contact
    - [x] merge pipetterUtils.getEffects... functions by having condition branches for 'destination' and 'source' properties
- [x] centrifuge: temperature parameters should require unit (e.g. 'degC')
- [x] upgrading lodash and babel
- [x] test EvowareTableFile.toStrings_internalLabware
- [x] test EvowareTableFile.toString
- [x] Evoware: Carrier.cfg: "0100ï¿½300000000000000000000000000000000000", how to interpret those strange characters? I think that's some quirk representing 7 bits, because I found a carrier with 9 sites on it, and 7 bits plus 2 bits = 9 sites...
- [x] evoware compiler: initial functionality
    - [x] figure out way to unit test the compiler
    - [x] why isn't table properly updated with labware after `transporter._movePlate`?
    - [x] for evoware site labels, don't use the full path name (only the last part)
    - [x] why is labware on RoboSeal missing for protocol3.cmp.json?
- [x] `experiment.run`: implement
- [x] consider how to use `experiment.run` in paper1_protocol1_ph
- [x] `lookupPath` should handle an object name as the first path element
- [x] get EvowareCompiler working
- [x] troubleshoot sealer.sealPlate
- [x] remove excess debug output from pipetting
- [x] add `--quiet` flag to suppress printing errors
- [x] let roboliq load .js protocols from the command line
- [x] dm00_test2.js: create output we want from the `experiment.run` command for a sample experiment
- [x] `timer.wait`
- [x] `system.if`
- [x] pipetteMixtures: test mixture items where 'destination' and 'syringe' are specified
- [x] test `system.if`
- [x] `shaker.shakePlate`
- [x] explicitly add TipModels to ourlab, along with their characteristics
- [x] pipetter.pipette: woops! we still have mario-specific code for `findTipModel()` and `assignProgram()`!
- [x] replace `pipetter.findTipModel*` with a more generic function
- [x] groupingMethod3: allow `layer` to take partial priority over item order
- [x] groupingMethod3: force a group break when previous destination is used as a source
- [x] pipetter.DilutionItem yaml
- [x] `pipetter.pipetteDilutionSeries2x`
- [x] pipetteMixtures: handle new mixture list
- [x] need to add liquid and labware for the "media" and "culture" sources
- [x] fix dilution series destinations (putting into A and B rows in the same item)
- [x] fix evoware Wash output
- [x] why no `timer._sleep` output from evoware?
- [x] create `Design` type in roboliq.yaml (needs more work)
- [x] roboliq.js: handle `data` property
- [x] commandHelper.js: handle `$$`-prefix for returning column of current data
- [x] roboliqDirectiveHandlers.js: handle '#data' directive
- [x] transporter.movePlate: should handle specifications of `equipment` and `program`
- [x] experiment.js: replace makeSubstitutions with commandHelper.substituteDeep
- [x] commandHelper.substituteDeep:
    - [x] should accept SCOPE and DATA instead of data, because we data.objects may change as steps are processed
    - [x] skip objects with 'data' property
    - [x] figure out dm00_test4m.yaml equivalent to dm00_test3m.js
- [x] tania13_ph.yaml: re-write using Design
- [ ] create incubator command for tania13_ph, and accommodate "methods" for expanding commands
- [ ] centrifuge.startIncubation: create
- [ ] create portable reader.measureFluorescence command that uses parameters instead of a file
- [ ] `experiment.run`: automatically find timers if none supplied, using the current agent value if supplied; may need to implement feature to reserve objects (such as to reserve a duration timer that isn't started at the beginning)
- [ ] change `#directive` to `directive()`, and if the value is an array, convert it to an object with key `items`.
- [ ] paper1_protocol3_unfolding.yaml: make sure it compiles
- [ ] consider having the fluorescenceReader command generate a report with the current plate contents
- [ ] handle mixture randomization well, and use it for the paper1 protocols (pH and unfolding)
- [ ] how can we automatically analyze the results of the fluorescence readout?
- [ ] use schemas for directives too
- [ ] evoware:
    - [ ] multiline comments (like in tania12) probably won't work -- test this in Tecan Evoware; maybe it will work to replace '\n' with '^L^G'
    - [ ] try loading the `tania*` protocols in Tecan Evoware
- [ ] find a better term for "Site", maybe "Position" or "Location" or "BenchPos" or something...
- [ ] review FIXMEs
- [ ] upload to a public repository
- [ ] test all pipetter commands
- [ ] test scripts on second Tecan robot
- [ ] rename '#createPipetteMixtureList' to '#mixtureList' and '#gradient' to '#gradientLinear', so it matches figure in paper
- [ ] add pipetter commands for dropping tips and getting tips
- [ ] drop the `params` parameter from commandHandler calls, but then rename `parsed` to `params` in commandHandler functions
- [ ] change `predicates` field to be a map of arrays instead of an array
- [ ] commandHelper.substituteDeep: only substitute template strings whose variables are '$' variables
- [ ] commandHelper.substituteDeep: support '$$' variables in template strings
- [?] implement `system.description`
- [ ] system.repeat: handle properties stepDuration, stepWaitBefore, stepWaitAfter, noStepDurationEnd (lookup after/end/last terminology in pipetter commands)

# Todos for luigi

- [x] configure sites and cliques for transporter.movePlate:
    - [x] camera
    - [x] P1-P3
    - [x] lightbox
    - [x] shaker
    - [x] sealer
    - [x] mario exchange hotel
    - [x] hotels
    - [x] regrip
        - [x] the regrip site is represented by two evoware sites, depending on the orientation we want for the romas
    - [x] reader
    - [x] culturebox
    - [x] add possibility to configure options in 'ourlab'
- [x] perform initial pipetting test
- [ ] pipetter.cleanTips: check whether luigi_protocol3 runs as expected
- [ ] evoware: after pipetting is done, retract all the tips somehow
- [ ] test pipetting on troughs
- [ ] double-check pipetting on P1-P6
- [ ] sealer operation
- [ ] reader operation
- [ ] culturebox operation
- [ ] test with three labware types: 96 nunc, 96 DWP, 6-well culture
- [ ] need to change evoware's labware model on DWP once its sealed
- [ ] suboptimal plate transport: P1 -> REGRIP_BELOW (catches on side)
- [ ] decide on wash programs with Daniel and Fabian

- [ ] shaker operation
- [ ] incubator sites:
    - [ ] add the extra logic+code for opening a black incubator site
- [ ] incubators operation
- [ ] camera operation

# Todos for ROMA qc

- [x] qc_mario_vectors_96nunc: randomize order of sites
- [x] qc_mario_vectors_96nunc: allow for setting a random seed
- [x] qc_mario_vectors_96nunc: output `description` values as evoware comments
- [x] qc_mario_vectors_96nunc: Create evoware Groups for steps with `description` properties
- [x] qc_mario_vectors_96nunc sites:
    - [x] RoboPeel
    - [x] hotels in back
    - [x] P1-P3,P6-P8 with ROMA2
    - [x] make sure ROMA1 is used for putting plate in reader when `equipment: roma1` is specified
    - [x] transfer hotels
- [x] qc_mario_vectors_96nunc: ERROR: wrongly placed on SEELER
- [x] qc_mario_vectors_96nunc: ERROR: misplaced from hotel32 site 19 to P2
- [ ] qc_mario_vectors_96nunc: HOTEL4_2 to ROBOSEAL didn't work great (caught briefly on the ROBOSEAL pins)
- [ ] qc_mario_vectors_96nunc: for ROBOSEAL and ROBOPEEL, should actually seal in order to check positions afterwards
- [ ] qc_mario_vectors_96nunc: C4 -> P6 was a bit off on P6
- [ ] qc_mario_vectors_96nunc: configure to omit timing commands
- [ ] create script for DWP
- [ ] create script for PCR plates

# Todos for paper 2/3

- [x] implement lookupPath
- [x] REFACTOR: pass same set of arguments to roboliq and evoware command handlers, so that evoware commands also receive parsed params
- [x] evoware compiler: add more commands
    - [x] `evoware._facts`
    - [x] `timer._start`
    - [x] `timer._wait`
    - [x] `pipetter._aspirate`
    - [x] `pipetter._dispense`
    - [x] `pipetter._pipette`
    - [x] `pipetter._washTips`
    - [x] delete scala files
- [x] implement lookupPaths
- [x] evoware: rename `_cleanTips` instruction to `_washTips`
- [x] test transporter.doThenRestoreLocation
- [x] commandHelper.getStepKeys: return array of step keys in order
- [x] commandHelper.stepArrayToObject: take an array of steps and return an object of steps
- [x] move scala project in ~/src/roboliq/evoware to ~/src/roboliq/old
- [x] have a good command for expanding steps and parameters based on data (see 'data' property handling in roboliq.js and '#data' in roboliq directives)
- [x] program a server that accepts data from the runtime client
- [x] use `express` to serve up an HTML page from serverUi, automatically display changes in `state.timing`.
- [x] program a prototype UI client that displays live data from the server as it's updated by the runtime client
- [x] program a prototype little "runtime client" that sends data to a server when called by Evoware
- [x] Add time logging to all evoware instructions
- [x] Test runtime-client/runtime-server/roboliq-runtime-cli
<<<<<<< HEAD
- [ ] see about using VB script to avoid the console popup when logging time of commands

    Set objShell = CreateObject("WScript.Shell")
    ' Set objExec = objShell.Exec("node C:\\Users\\localadmin\\Desktop\\Ellis\\roboliq\\runtime-server\\roboliq-runtime-cli.js begin 100")
    Return = objShell.Run("node C:\\Users\\localadmin\\Desktop\\Ellis\\roboliq\\runtime-server\\roboliq-runtime-cli.js begin 100", 0, true)

    Set args = Wscript.Arguments

    For Each arg In args
      Wscript.Echo arg
    Next

    CSCRIPT MyScript.vbs

    WScript.Arguments.Item(0)
    WScript.Arguments.Item(1)

    test from command line with wscript test.vbs

- [ ] qc_mario_vectors_96nunc:
    - [x] randomize order of sites
    - [x] allow for setting a random seed
    - [x] output `description` values as evoware comments
    - [x] Create evoware Groups for steps with `description` properties
    - [ ] sites:
        - [ ] RoboPeel
        - [ ] hotels in back
        - [ ] transfer hotels
        - [ ] P1-P3,P6-P8 with ROMA2
        - [ ] make sure ROMA1 is used for putting plate in reader when `equipment: roma1` is specified
    - [ ] ERROR: wrongly placed on SEELER
    - [ ] ERROR: misplaced from hotel32 site 19 to P2
- [ ] runtime-server: take as input a `.out.json` file
- [ ] runtime-client: create a redux version, start with `fullstack-voting-client`
- [ ] Figure out how to automatically convert reader data to measurement JSON data
=======
- [x] EvowareCompiler: make addition of run-time instructions an option
- [x] roboliq-runtime-client: create a redux version, start with `fullstack-voting-client`
- [x] see about using VB script to avoid the console popup when logging time of commands
- [x] runtime-client/Log: duplicate from Runtime
- [x] runtime-client/Log: nicer table format with padding between columns
- [x] runtime-client/Log: separate bold row for changes in date
- [x] runtime-client/Log: only display the time in time column instead of whole ISO date
- [x] runtime-server: accept as input a `.out.json` file
- [x] runtime-client/Runtime: display protocol
- [x] runtime-client/Runtime: better display of from-till/duration time for each command
- [ ] Figure out how to automatically convert reader data to measurement JSON data
	- [ ] roboliq-runtime-cli: should send XML to runtime-server
	- [ ] roboliq-runtime-cli: should rename XML file to include end-time suffix
	- [ ] runtime-server: setInfiniteMeasurements: accepts `{step, xml}`
	- [ ] roboliq-runtime-client: should display measurements using vega
	- [ ] roboliq-runtime-client: should display measurements using a table
- [ ] runtime-client: when we get updated state from server, calculate new Runtime report in order to remove the logic from Runtime.jsx
- [ ] runtime-client: for the Runtime report, calculate accumulated durations for all steps, and calculate a flag when a step is done
- [ ] runtime-client/Runtime: display checkmark next to commands which are complete
- [ ] roboliq-runtime-client: rename folder to runtime-client
- [ ] runtime-server: use REST instead of socket for communication with runtime-client
- [ ] figure out how to run the runtime-client without webpack-dev-server
- [ ] consider starting the runtime-server from Evoware, if its not already running
- [ ] consider opening a browser window for the runtime-client from Evoware
- [ ] make the runtime programs' ports configurable
>>>>>>> 4877f16d0564fed243f509fce6fce98448d9b1ee
- [ ] test pipetter.pipetteDilutionSeries2x
- [ ] design.js: when assigning a column array, produce an error if there are fewer array elements than rows
- [ ] for JSON editor in web UI, take a look at http://arqex.com/991/json-editor-react-immutable-data
- [ ] dm00_test3m.js:
    - [ ] BUG: why are media and strain sources taken from different syringes?
    - [ ] BUG: why is water dispensed for dilution using tips 3+4, and why does it wash between?
    - currently: after pipetting culture, 1m8s left to wait
    - currently: after second culture preparation (and wait of 1m8s), probably more than 2 minutes left to wait (look at it around 1m18s)
    - PROBLEM: dilution series failed because it didn't detect enough liquid (but 188ul!)
    - PROBLEM: then there were only 11s left before next dilution (I manually told evoware to ignore the pipetting errors)
    - 46s after measurement 1, second dilution
    - 12s after measurement 2, second dilution
    - [ ] `absorbanceReader.measurePlate`
- [ ] dm00_test3.js: create a protocol we can run on EITHER mario or luigi (just has to work, not be pretty -- I can prettify it later)
    - something with absorbance OrangeG
    - two "culture" plates
    - two dilution plates
    - should skip sealing so that we don't need to deal with the complications on Sunday
    - [ ] mario: can't use tips 1-4 to puncture a seal
    - [ ] luigi: need to change the evoware labware for a deep-well plate once its sealed
- [ ] transporter test errors:
    - [ ] transfers from P2, twice it didn't grip
    - [ ] why was roma2 used for transfer from REGRIP to READER when we were in the roma1 group?
    - [ ] roma2, wide grip, from REGRIP to READER: doesn't narrow the grip again, so it pushes aside the transfer hotel
    - [ ] should automatically move ROMAs to "home position" before a different roma is used.
- [ ] compile the experiment for mario
- [ ] HACK: remove HACK for 'air' dispense of diluent in pipetter.pipetteDilutionSeries2x
- [ ] ourlab.js: DWP cann't go on sealer or peeler
SUNDAY:
- [ ] compile the experiment for luigi
- [ ] run the experiment on mario
- [ ] run the experiment on luigi
- [ ] try to extend EvowareCompile to save timing data while executing commands
- [ ] make video of same experiment running on both robots
    - [ ] demonstrate interleaved timing by showing the robot's picture-in-picture starting the same procedures at the same time
    - [ ] show the data displayed in R/html as soon as the measurement is done
- [ ] proper randomization
LATER:
- [ ] generate a pretty HTML/SVG protocol for the experiment
- [ ] web UI just for showing interactive experiment design
- [ ] cli ui?
    - [ ] `load` command
        - [ ] `--config` for config protocols
        - [ ] `--protocol` for the main protocol
        - [ ] `--user` for the user settings
    - [ ] `evoware` command
        - [ ] set evoware robot, load carrier and table, flag that evoware compiler is being used
    - [ ] `compile`
        - [ ] compile the protocol
        - [ ] only compile the protocol if there have been relevant changes
        - [ ] compile evoware if configured
        - [ ] only compile evoware if there have been relevant changes
    - [ ] `show`
        - [ ] show a step
        - [ ] show an object
        - [ ] show a table
    - [ ] `dump`
        - [ ] dump entire state
        - [ ] dump part of state
- [ ] web ui?
- [ ] reduce number of levels of steps generated by commands like `timer.doAndWait`


## Todos for complex protocol and feedback

<<<<<<< HEAD
- [x] implement lookupPath
- [x] REFACTOR: pass same set of arguments to roboliq and evoware command handlers, so that evoware commands also receive parsed params
- [x] evoware compiler: add more commands
    - [x] `evoware._facts`
    - [x] `timer._start`
    - [x] `timer._wait`
    - [x] `pipetter._aspirate`
    - [x] `pipetter._dispense`
    - [x] `pipetter._pipette`
    - [x] `pipetter._washTips`
    - [x] delete scala files
- [x] implement lookupPaths
- [x] evoware: rename `_cleanTips` instruction to `_washTips`
- [x] test transporter.doThenRestoreLocation
- [x] commandHelper.getStepKeys: return array of step keys in order
- [x] commandHelper.stepArrayToObject: take an array of steps and return an object of steps
- [ ] figure out why this Design doesn't have the correct table column order in firefox:
=======
- [ ] figure out why this Design doesn't have the correct table column order in firefox (try OrderedMap instead of Map for immutablejs):
>>>>>>> 4877f16d0564fed243f509fce6fce98448d9b1ee
    ```
    conditions:
      waterSource: saltwater
      waterVolume: 40ul
      proteinSource*: [sfGFP, Q204H_N149Y, tdGFP, N149Y, Q204H]
      proteinVolume: 5ul
      bufferSystem*:
        acetate:
          acidSource: acetate_375
          baseSource: acetate_575
          acidPH: 3.75
          basePH: 5.75
    ```
<<<<<<< HEAD
- [ ] Experiment Conditions Data:
- [ ] check whether Evoware external n2 is display order
- [ ] compile some simple protocols and try to run them on mario and luigi
=======
- [ ] check whether Evoware external n2 represents display order
>>>>>>> 4877f16d0564fed243f509fce6fce98448d9b1ee
- [ ] evoware compiler: add comments to beginning of script regarding how the script was generated
- [ ] evoware compiler: allow for loading Carrier.json instead of Carrier.cfg
- [ ] evoware compiler: allow for loading JSON table instead of .ewt or .esc
- [ ] evoware compiler: add script line for every protocol command to log the start and end times of that command
- [ ] evoware compiler: add code to process and display measured data after measurement commands
- [ ] evoware: test `pipetter._cleanTips`
- [ ] evoware: write more extensive tests for `pipetter._aspirate`, `pipetter._dispense`, and `pipetter._pipette`
- [ ] move scala project in ~/src/roboliq/evoware to ~/src/roboliq/old
- [ ] maybe move evoware folder up one level (e.g. to ~/src/roboliq/evoware)
- [ ] test `evoware._facts`
- [ ] test `EvowareCompiler.compile`
- [ ] evoware: add command-line option to only process certain steps
- [ ] evoware: when user specifies steps to process but no output name, name the output file according to the steps processed
- [ ] for ANOVA visualization, consider http://rpsychologist.com/d3-one-way-anova

## On-going todos with lots of sub-steps

- [ ] complex protocol
    - [ ] write script based on DM_Growthcurves
    - [ ] allow "#calculate" to accept a string to be evaluated by mathjs, and somehow handle scope too so that other numeric variables can be used in the expression
- [ ] user documentation (see <http://usejsdoc.org/about-tutorials.html>)
    - [x] Commands.md: Add general documentation to each command namespace
    - [ ] Commands.md: add examples for each command
    - [ ] document the properties of the types (i.e. add 'description' field)
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
- [ ] implement equivalents for BioCoder commands
    - [ ] optional_step
    - [ ] parallel_step
    - [ ] to_do
    - [ ] store_until
    - [ ] use_or_store
    - [ ] time_constraint
    - [?] set_value
    - [?] assign
    - [ ] add
    - [ ] divide
    - [ ] subtract
    - [ ] multiply
    - [ ] discard
    - [ ] drain
    - [ ] new_solid
    - [ ] new_container
    - [ ] new_slide
    - [ ] new_column
    - [ ] measure_solid
    - [ ] measure_prop
    - [ ] add_to_column
    - [ ] add_to_slide
    - [ ] collect_tissue
    - [ ] plate_out
    - [?] transfer
    - [ ] combine
    - [ ] combine_and_mix
    - [ ] dissolve
    - [ ] invert
    - [?] pipet
    - [ ] resuspend
    - [ ] tap
    - [ ] vortex
    - [ ] vortex_column
    - [ ] incubate_and_mix
    - [ ] mixing_table
    - [ ] mixing_table_pcr
    - [ ] immerse_slide
    - [ ] remove_slide
    - [ ] wash_slide
    - [ ] homogenize_tissue
    - [ ] wash_tissue
    - [ ] incubate
    - [ ] store_for
    - [ ] set_temp
    - [ ] store_plate
    - [ ] thermocycler
    - [ ] thermocycler_anneal
    - [ ] pcr_init_denat
    - [ ] pcr_final_ext
    - [ ] inoculation
    - [ ] incubate_plate
    - [ ] invert_dry
    - [ ] dry_pellet
    - [ ] dry_slide
    - [x] centrifuge
    - [ ] centrifuge_pellet
    - [ ] centrifuge_phases_top
    - [ ] centrifuge_phases_bottom
    - [ ] centrifuge_column
    - [ ] centrifuge_flow_through()
    - [ ] ce_detect
    - [ ] electrophoresis
    - [ ] facs
    - [ ] measure_fluorescence
    - [ ] mount_observe_slide
    - [ ] sequencing
    - [ ] electroporate
    - [ ] weigh
    - [ ] cell_culture
    - [ ] transfection
- [ ] implement equivalents for PR-PR commands

## After submission

- [ ] roboliq.js: need to handle a complication with final instructions: if their parameters contain references to DATA, SCOPE, or string substitutions, we need to either substitute the values into the command or create a sub-command with the concrete values.
- [ ] test equipment.run|ourlab.mario.evoware|ourlab.mario.shaker
- [ ] replace tests/schema.json with a JS file that loads the files in schemas/?
- [ ] pipetter.js: figure out a better way to order items using the 'layer' property, maybe have an 'order' parameter of 'index', 'layer', or 'semilayer', where 'semilayer' would do what `groupingMethod3()` currently does, 'index' would ignore 'layer', and 'layer' would sort all items by their layer value.
- [ ] pipetter.js: disallow pipetting on a plate with a lid (may need to create a LabwareLid type)
- [ ] create schema for pipetter.TipModel and use it (see pipetter.pipette:findTipModel, where min and max volumes are checked)
- [ ] pipetter.js: findTipModel(): check whether the labware is sealed
- [ ] pipetter.js: findTipModel(): check whether the well has cells
- [ ] pipetter.js: Try to find tipModel for each layer (see calls to `setTipModel()`)
- [ ] stringify out.json in better order (first properties in schema order, then alphabetic properties in natural order, then numeric properties in natural order) (for related code, see https://www.npmjs.com/package/canonical-json)
- [ ] possibly mark steps which are generated by expansion with `_generated: true` property
- [ ] BUG: HTN: in transporter.js, if we use all transporterLogic predicates at once for planning, then in sealerTest a two-step route for plate movement is accepted instead of a one-step route -- that's why I had to split up transporterLogic into groups (null, one, two) and iterate through them.
- [ ] `commandHelper.parseParams`: test that a parameter of type `name` does not expand when the value happens to be a path to an object
- [ ] `commandHelper.parseParams`: return a list of extra properties in the parameters that aren't in the schema (e.g. `parsed.unknown`)
- [ ] `commandHelper.parseParams`: test `parsed.unknown` list
- [ ] `commandHelper.lookupPath()` should accept ParsedParams instead of plain object params (create a ParsedParams class for this)
- [ ] `commandHelper.lookupPath()` should use `g()` to update `data.accesses` (only with the last element in each path or subpath)
- [ ] improve error reporting (e.g. use try/catch blocks, and prepend context to the Error properties and rethrow)
- [ ] `pipetter.cleanTips` should call `pipetter.cleanTips|$agent|$equipment` to get the low-level cleaning commands -- but if there are any errors, then return the sub-command instead.
- [ ] `pipetter.pipette` should call `pipetter.cleanTips|$agent|$equipment` to get the cleaning commands, and then update its syringeClean variables based on the effects of the resulting commands
- [ ] refactor: rename commandHelper type `name` to `Symbol`
- [ ] refactor roboliq.js: `_run`: move as many of the functions as possible out of the `_run` function
- [ ] refactor roboliq.js: `_run`: rather than return `{protocol, output}`, add tables to protocol and return protocol
- [ ] remove expectCore.paramsRequired
- [ ] write back-end for Hamilton
- [ ] write back-end for PR-PR?
- [ ] UI to display protocol
- [ ] UI to interactively customize protocol
    - see http://arqex.com/991/json-editor-react-immutable-data
    - see https://github.com/jdorn/json-editor
- [ ] UI to create protocol
- [ ] consider adding constraints to properties using 'property@' properties, e.g. `model@: {rows:8, columns:16}`
- [ ] refactor commandHelper processValue functions, organize them in a more principled way to avoid duplication of various type checks.
- [ ] handle multi-aspirate, and prevent use of `pipetter._pipette` if there are already contents in a syringe
- [ ] use a generic incubate command in tania13_ph_3_measure, instead of centrifuge.run
- [ ] write program to generate part of ourlab.mario from the evoware config files ('siteModel', 'stackable')
- [ ] support creation of well groups by commands, so that the same wells can be used by later commands; pass in a well group name to the generating command, and have it populate that well group as an effect
- [ ] add program version info to the protocol output
- [ ] add commandline arguments to the protocol output
- [ ] add a command line command to split a protocol into its specification/realization parts for when a protocol needs to be shared
- [ ] preProcess_Marks: preserve property order
- [ ] test the usage of a separate protocol as part of a new protocol (test re-use); particularly tricky will be object merging and gathering all required step parameters into a single parameters map
- [ ] figure out how to pass commands without handlers onto the biologist for execution
- [ ] change imports to require a version number?
- [ ] change commandHandlers to return an object with both descriptive and 'handle' functions; the descriptive function should contain a description and information about the parameters
- [ ] improvements to specification/realization splitting
    - [ ] consider adding `extend` and `patch` keywords for `loadProtocol()`
    - [ ] `patch` items can be handled by `jiff` or `_.set`, depending on their content
- [ ] augment protocol design:
     - [ ] specify factors
    - [ ] choose combinations of factor levels (e.g. full-factorial)
    - [ ] possibly partition blocks of the combinations
    - [ ] construct mixtures, if relevant
    - [ ] assign mixtures to wells (probably with randomization)
    - [ ] specify pipetting details, such as order of sources, and parameters by source
    - [ ] allow factor values to alter program flow (i.e., heating vs not heating)
- [ ] refactor misc.js, expect.js, and commandHelper.js to remove duplication
- [ ] protocols/protocol8.json: add all cleaning intensities for all tips
- [ ] for 'ourlab' configuration, handle table selection, for different table configurations; consider setting a table filename that can be used by the evoware translator, rather than passing the filename on the command line
- [ ] for unit tests, create a simplified variant of ourlab.js that won't need to be changed when there are changes on our robot
- [ ] commands/pipetter.js: pipetter.pipette: method 4
- [ ] commands/pipetter.js: `pipetter.pipette`: refresh tips (advanced)
- [ ] change imports to allow for passing parameters, such as table name?
- [ ] create test for the usage of custom command handlers in a user's protocol
- [ ] commands/pipetter.js: `pipetter._aspirate`: output effects array for changes in tip contents
- [ ] commands/pipetter.js: `pipetter._dispense`: output effects array for changes in tip contents
- [ ] mix command
- [ ] roboliq.js: generate table of pipetting actions for well contents by step
- [ ] timer.doAndWait: should stop the timer once the loops are over
- [ ] consider whether to do typechecking of all parameters automatically -- in this case, the "program" parameter to movePlate, for example, would either need to be an object reference or an explicit string, but not a name which doesn't resolve to an object, as is the case currently
- [ ] add warning/error handling to objectToLogicConverters
- [ ] llpl.js: need to add a function to create a database object, so that we can use multiple instances simultaneously
- [ ] commands/pipetter.js: pipetter.pipetteMixtures: allow for ordering items by source and volume
- [ ] consider allowing for mathematical expressions (see http://mathjs.org/)
- [ ] wellsParser: implement 'free' verb for empty and unreserved wells
- [ ] figure out how to generate partial factorial design lists
- [ ] figure out how to split large factorial designs over multiple plates/batches
- [ ] roboliq.js: allow for specifying the step range to compile for
- [ ] commandHandler: allow for returning of property constraints, for example: the sealPlate command could return a list of possible plate models for the given plate
- [ ] commandHandler args: also pass paramDefaults
- [ ] commands/sealer.js: figure out more sophisticated way to deal with agents for the pre/post steps; consider 'agentPreferred' parameter
- [ ] handle lids on plates and tracking their position
- [ ] add a default storage site for plates?  How to handle when plates are shared between robots?
- [ ] commands/pipetter.js: handle case of dispensing then later aspirating from a single well in a single pipetting command
- [ ] consider allowing for scopes to commands, rather than just globals objects and command params; may need to make data.objects an array.
- [ ] Use Immutablejs to protocol structure: should speed up handling of large protocols by avoiding the `_.cloneDeep` calls and unnecessary recalculations of logic for each step.
- [ ] `pipetter.cleanTips` should call `pipetter.cleanTips|$agent|$equipment` to get the low-level cleaning commands to return -- but if there are any errors, then return the sub-command instead.
- [ ] `pipetter.pipette` should call `pipetter.cleanTips|$agent|$equipment` to get the cleaning commands, and then update its syringeClean variables based on the effects of the resulting commands

# Notes

When loading JSON/JavaScript files, we expect the following structure:

    {
      roboliq: string,
      objects: {},
      steps: {},
      effects: {},
      predicates: [],
      taskPredicates: [],
      objectToPredicateConverters: {function},
      commandHandlers: {function},
      planHandlers: {function}
    }

# Misc

- SHOP code from http://danm.ucsc.edu/~wsack/SoftwareArts/Code/Plan/

# Loading process

* Get a list of filenames as protocols, an optional protocol on the command line, and optionally a list of filename/filedata pairs.
* Add the optional filename/filedata pairs to the file cache
* Load the content of each filename and add to file cache
* Construct a protocol list consisting of the default files to always load, protocol files passed in args, and protocol passed in arg
* For each protocol:
    * load its imports individually, merge the protocol into its imports
    * pre-process directives and urls
    * merge into previous protocol

# Separation of protocol specification and realization

The protocol specification and realization can be combined in a single for for convenience while developing the protocol.  In this case, properties can be suffixed with a '?' or '!' to allow for automatically separating the realization aspects from the specification later.

A '!'-suffixed property indicates that the value is lab-specific and is not portable.  When compiled, such properties will automatically have the '!' removed from their name.  When Roboliq is told to split the file into specification+realization, it will remove such properties from the specification and add them to the realization file.

A '?'-suffixed property indicates that the value need to be supplied by the realization.  Such a property should be a map.  It may contain a `description` field and a `value!` field.  If the `value!` field is present, a property without the '?'-suffix will be added to the containing object with the given value.

# Variable references

Need to reference objects, variables, step parameters, parameters of current command, parameters of parent command

* `${plate1}` -- an entire object
* `${plate1.location}` -- the value of an object's property
* `${volume}` -- the value of variable, e.g. `objects.volume.value`

Still need figure out how to reference parameter values, here are some ideas:

* "^object" -- `object` parameter for current command
* "^^object" -- `object` parameter for parent command
* "^^{2.object}" -- `object` parameter for second sub-step of the parent command
* "${^object.location}" -- the location of the object referenced by the `object` parameter for the current command
* "${^{object}.location}" -- the location of the object referenced by the `object` parameter for the current command

# Pipetting optimization

For fixed tips:

* Which tips to use?  Large or small?
* Washing when necessary
* Which program ("liquid class") to use?
* How to group tip tips and wells?
* Washing is deterministic

Assigning tips to sources:

* Try to assign one tip model to each source.  If that doesn't work, assign the optimal tip model for each pipetting step from that source.

Tuple: (source, destination, volume, flags, tipModel, tip, program, pre-commands, post-commands)

"#wells#plate1(A01 down 4)"

    A01 down [take] 4
    A01 down [to] D01
    A01 down block [to] B02
    A01 right [take] 4
    A01 right [to] A04
    A01 right block [to] B02
    all random(seed)
    all random(seed) take 1
    all free take 4
    all free random(seed) take 4

Simplest methods/algorithms:

1. Only use one tip
2. Use each tip, rotating through them till they need to be washed
3. Group as many tips at once as possible
4. Group as many tips at once as possible, but if group splits over columns, see if we can get a longer contiguous group by restarting the grouping in the new column rather than splitting the group from the previous column

Modularize the methods more:

- break the items into groups that should be handled simultaneously, possible methods include:
    - each item is its own group
    - groups are built until no more syringes would be available based on the item's tipModel (but syringe doesn't need to be assigned yet)
    - groups are built (with limited look-ahead) where alternatives are investigated when a group splits over two columns
    - have a fixed assignment between wells and syringes (i.e. row n = tip (n % 4)) for the sake of managing differences between tips
- assign syringes by group for items without an assigned syringe
- assign source well by group for items without assigned source wells; if multiple syringes need to access the same source, and that source has multiple wells, then possible methods include:
    - pick first one
    - rotate through source wells in order
    - rotate through source wells in order of max volume
    - try a simple geometrical assignment considering whether there are more tips or wells; if that fails, use previous method
    - same as above, but if wells > tips, try starting at first (wells - tips) wells and see which one produces the greatest minimum final volume
- add cleaning actions between each group, at the beginning, and at the end

A completely different method that would sometimes useful to manage tip differences:

## Cleaning tips

cleanBegin: intensity of first cleaning at beginning of pipetting, before first aspiration.
Priority: item.cleanBefore || params.cleanBegin || params.clean || source.cleanBefore || "thorough"

cleanBetween: intensity of cleaning between groups.
Priority: max(previousCleanAfter, (item.cleanBefore || params.cleanBetween || params.clean || source.cleanBefore || "thorough"))

previousCleanAfter = item.cleanAfter || if (!params.cleanBetween) source.cleanAfter

cleanEnd: intensity of cleaning after pipetting is done.
Priority: max(previousCleanAfter, params.cleanEnd || params.clean || "thorough")


# Combinatorial stuff

* tableRows: property names given as row of column names, values as remaining rows, defaults as objects
* tableCols: property names as property names, values in array
* for: for comprehension with keys: factors (array), replicates (integer), order (function), output (value), flatten (boolean or 'deep')
* factorialCols: property names as property names, values in array
* factorialArrays: combinatorial combination of an element from each input array
* merge: merge an array of objects
* factorialMerge: factorial merging of array of arrays of objects (like ``factorialArrays >> map merge``)
* replicate

```
    #factorialTemplate:
        variables:
        - a: [1, 2, 3]
        - b: [3, 4, 5]
        - c: {name: "bob"}
        template:
            hello: "'{{a}}'",
            n: {{b}}
            c: ${c}
```

somehow distinguish between control factors, nuisance factors, blocking factors, measurable factors, unknown factors

- factor: [protein, volume]
  values: [[gfp, 10], [gfp, 20], [gfp, 30], [yfp, 5]]
- factor: something else
  values: [1, 2, 3, 4]
- "#tabfile#factor3.tab"
- "#csvdata#protein,volume\ngfp,10\ngfp,20\ngfp,30\nyfp,5"

For comprehensions

type: Eval.List
type: Eval.Wells

    "1": {
        "command": "pipetter.pipette",
        "items": {
            "type": "Eval.List",
            "variables": [{
                "name": ["source", "volume"],
                "values": [["water", "10ul"], ["reagent1", "20ul"]]
            }, {
                "name": "destination",
                "values": "#wells#plate1(A02 down 8)"
            }],
            "order": ["-volume"],
            "template": {
                "source": "{{source}}",
                "destination": "{{destination}}",
                "volume": "{{volume}}"
            }
        }
    }

    name: [hepes_850, hepes_650]
    values: [[30, 0], [22.5, 7.5], [15ul, 15ul], [7.5ul, 22.5ul], [0ul, 30ul]]
    gradient: [hepes_850, hepes_650]
    count: 5

    [{source: sfGFP}, {source: tdGFP}]
    [{volume: 5ul}, {volume: 10ul}]
    vs
    [{source: sfGFP, volume: 5ul}, {source; tdGFP, volume: 10ul}]

    list = {}
    for (i = 0; i < variables.length; i++) {
        for (j = 0; j < variables[i].elements.length; j++) {
            variables[i].elements[j]
        }
    }


    {water: 40ul, hepes_850: 30ul, hepes_650: 0ul, sfGFP: 5ul}
    {water: 40ul, hepes_850: 30ul, hepes_650: 0ul, Q204H_N149Y: 5ul}
    {water: 40ul, hepes_850: 22.5ul, hepes_650: 7.5ul, sfGFP: 5ul}

    for:
    - [source, volume] in [[water, 10ul], [reagent1, 20ul]]
    - destination in plate1(A02 down 8)
    template:
      source: {{source}}
      destination: {{destination}}
      volume: {{volume}}

      - {source: water, destination: plate1(A02), volume: 10ul}
      - {source: water, destination: plate1(B02), volume: 10ul}

# Equipment command conversion

    command: equipment.open
    agent: ourlab.mario.evoware
    equipment: ourlab.mario.reader

    command: equipment.open
    agent: ourlab.mario.evoware
    equipment: ourlab.mario.reader
    handler: function(params, data)

    command: equipment.open|ourlab.mario.evoware|ourlab.mario.reader
    handler: function(params, data)

# Reports

## Labware

    reports:
        labware:
        - labware: plate1
          type: Plate
          model: ...
          locationInitial: P3
          locationFinal: P3

## Wells

    objects:
        __WELLS__:
            plate1(A01):
                isSource: true
                contentsInitial:
                    water: 0ul
                volumeAdded: XXX
                volumeRemoved: 60ul
                contentsFinal:
                    water: -60ul

    reports:
        sources:
        - source: water
        sourceWells:
        - source: water
          well: plate1(A01)
          volumeInitial: 0ul
          volumeFinal: -60ul
          volumeRemoved: 60ul
        wellContentsInitial:
        - well: plate1(A01)
          volume: 0ul
          water: 0ul
        wellContentsFinal:
        - well: plate1(A01)
          volume: -60ul
          water: -60ul
        - well: plate1(A02)
          volume: 60ul
          water: 60ul

## Command Reports

An example of a command report/table would be for the reader command.
The reader will read a plate and produce an output file.  To analyze it,
we need to know what the contents of the plate were.
So the command should create a report of the well contents before readout.

    reports:
        step-2:
            {}

# Error reporting

    1.1: parameter `sources`: value `systemLiquid`: undefined
    1.2: parameters `items`, `sources`, `destinations`: must have equal lengths
    1.3: value `plate1.model`: undefined, please set a value

# Factorial designs

Normally, a design is represented by an array of objects.
The object's properties are factor names, and the values are the factor values.

Here's an experiment description for the sample pH experiment:

```
phConditions:
  type: Conditions
  description: |
    my description...
  factors:
    saltwaterVolume: 40ul
    gfpVolume: 5ul
    gfpSource: sfGFP
    bufferSystem:
      acetate:
        acidPH: 3.75
        basePH: 5.75
        acidSource: acetate_375
        baseSource: acetate_575
        count: 8
      mes:
        acidPH: 5.10
        basePH: 7.10
        acidSource: mes_510
        baseSource: mes_710
        count: 7
      pipes:
        acidPH: 5.75
        basePH: 7.75
        acidSource: pipes_575
        baseSource: pipes_775
        count: 5
      hepes:
        acidPH: 6.50
        basePH: 8.50
        acidSource: hepes_650
        baseSource: hepes_850
        count: 5
    acidVolume: {range(): {start: 30ul, end: 0ul, count: count, decimals: 1}}
    baseVolume: {calculate(): "30 - acidVolume"}
    pH: {calculate(): "(acidPH * acidVolume + basePH * baseVolume) / 30ul)"}
  replicates: 3
  assignWells:
    well: mixPlate(all)
  assign:
    syringe:
      items: [1,2,3,4]
      sequence: order
  randomSeed: 123
```

It should produce output similar to this:

```{yaml}
description: my description...
items:
- index: 1
  order: 7
  saltwaterVolume: 40ul
  gfpVolume: 5ul
  gfpSource: sfGFP
  bufferSystem: acetate
  acidPH: 3.75
  basePH: 5.75
  acidSource: acetate_375
  baseSource: acetate_575
  count: 8
  acidVolume: 30ul
  baseVolume: 0ul
  pH: 3.75
  well: mixPlate(H01)
  syringe: 3
- index: 2
  ...
```

The protocol steps to run the experiment could be:

CONTINUE
```
- {destination: $well, source: saltwater, volume: $saltwaterVolume}
- {destination: $well, source: $acidSource, volume: $acidVolume}
- {destination: $well, source: $baseSource, volume: $baseVolume}
- {destination: $well, source: $gfpSource, volume: $gfpVolume}
command: experiment.pipetteMixtures
experiment: experiment1
mixtures:
  -
destinations: mixtureWells
clean: flush
cleanBegin: thorough
cleanBetweenSameSource: none
cleanEnd: thorough
```



This has the following structure:
* for each GFP variant:
    * denature a sample of GFP
    * wait for 7 minutes
    * extract three samples
    * measure those three samples sequentially (with injected dilution)

The multi-level experiment description could look like this:

```{yaml}
- index: 1
  gfpSource: sfGFP
  denaturantVolume: 85.5ul
  gfpVolume: 4.5ul
  unfoldingTime: 7 minutes
  sampleVolume: 7ul
  mixWell: mixPlate(A01)
  sampleWells: mixPlate(B01,C01,D01)
```

The code to create that experiment description might look like this:

```{yaml}
objects:
  refoldingConditions:
    type: Conditions
    factors:
      gfpSource: [sfGFP, ...]
      denaturantVolume: 85.5ul
      gfpVolume: 4.5ul
      unfoldingTime: 7 minutes
      sampleVolume: 7ul
    assignWells:
      mixWell: mixPlate(all)
      sampleWells:
        wells: mixPlate(all)
        count: 3
    randomSeed: 123
```

The steps to perform the experiment might look like this:

```{yaml}
command: experiment.run
conditions: refoldingConditions
steps:
  1:
    command: pipetter.pipette
    items:
    - {source: denaturant, volume: $denaturantVolume}
    - {source: $gfpSource, volume: $gfpVolume}
    destinations: $mixWell
  2:
    command: timer.sleep
    duration: $unfoldingTime
  3:
    command: pipetter.pipette
    sources: $mixWell
    volumes: $sampleVolume
    destinations: $sampleWells
  4:
    command: fluorescenceReader.measurePlate
    object: mixPlate
    program:
      wells: $sampleWells
    programTemplate: ./refolding.mdfx.template
    #outputFile: 'C:\\Users\\localadmin\\Desktop\\Ellis\\tania15_renaturation--<YYYMMDD_HHmmss>.xml'
```

Sometimes we need multi-level designs, such as when first one set of master
mixtures is prepared, and then experiments are done drawing samples from
each of them. CONTINUE.

# Data

```
# a data field with a 'forEach' leads to a step being repeated with the appropriate scope
data: {
    source: myExperiment,
    distinctBy: "cultureWell",
    forEach: "row"
}

data(): {
    distinctBy: "cultureWell",
    actions: [{
        action: "math",
        value: "cultureVolume / 2"
    }],
    value: "$cultureWell",
    join: ",",
}
```
