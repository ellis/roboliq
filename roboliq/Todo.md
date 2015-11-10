## Before submission

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
	- [x] tani13_ph-ourlab-diff
	- [x] tania12_denaturation
- [x] if an object has both ! and non ! properties, the ! property should take precedence
- [x] `npm test`: fix problem with variable that references a source
	- [x] node_modules/.bin/mocha --compilers js:babel/register tests/wellsParserTest.js
	- [x] node_modules/.bin/mocha --compilers js:babel/register tests/pipetterTest.js
- [x] `diff tania12_denaturation.out.json tania12_denaturation-new.out.json > x`
- [x] pipetterTest: re-enable all the tests
- [x] command documentation:
	- [x] create commandSpecs for all commands (JSON/YAML structured documentation)
	- [x] move all commandSpecs to YAML files
	- [x] generate documentation from commandSpecs
		- [x] write code to do the generation
		- [x] create an npm command for generating docs
		- [x] install git version of jsdoc and try to get it to work with our ES6 code
		- [x] put the command docs somewhere where jsdoc will pick it up
	- [x] generateCommandSpecDocs.js: load yaml files instead of command handler files
	- [x] change all commandSpecs to use roboliq types, rather than just JSON Schema types (continue with sealer.yaml)
	- [x] use commandSpecs to parse the params before passing them to the commandHandler
	- [x] pipetterUtils.getEffects_pipette: line 38 (items are parsed)
	- [x] checkout fluorescenceReaderTest, because the relevant ourlab.js command handlers haven't been updated yet to use schemas
	- [x] refactor all commands to accept parsed parameters, using the commandSpecs
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
- [ ] refactor commandHelper parseValue functions to return `{values, objects, inputs}`
- [ ] refactor: rename commandHelper type `name` to `Symbol`
- [ ] generate documentation for object schemas
- [ ] create commandHelper.parseParams test for misspelled `sources` specifier (e.g. removing `balanceWater` from tania13)
- [ ] commandSpec: pipetter.pipetteMixtures.order: should be an enum
- [ ] fixup pipetter.js to not hardcode our `syringesAvailable` and `tipModelToSyringes`
- [ ] rename commandSpec to schema everywhere
- [ ] user documentation (see <http://usejsdoc.org/about-tutorials.html>)
- [ ] code documentation
	- [x] roboliq.js
	- [x] WellContents.js
	- [ ] commandHelper.js
	- [ ] expect.js
	- [ ] expectCore.js
	- [ ] generateCommandSpecDocs.js
	- [ ] main.js
	- [ ] misc.js
	- [ ] commands...
- [ ] evoware:
	- [ ] add run-option to only process certain steps
	- [ ] when user specified steps to process but no output name, name the output file according to the steps processed
	- [ ] multiline comments (like in tania12) probably won't work -- test this in Tecan Evoware.
	- [ ] try loading the `tania*` protocols in Tecan Evoware
- [ ] rewrite evoware compiler in javascript
- [ ] find a better term for "Site", maybe "Position" or "Location" or "BenchPos" or something...
- [ ] upload to a public repository
- [ ] test all pipetter commands
- [ ] testing scripts on second Tecan robot

## After submission

- [ ] improve error reporting (e.g. use try/catch blocks, and prepend context to the Error properties and rethrow)
- [ ] refactor roboliq.js: `_run`: move as many of the functions as possible out of the `_run` function
- [ ] refactor roboliq.js: `_run`: rather than return `{protocol, output}`, add tables to protocol and return protocol
- [ ] write back-end for Hamilton
- [ ] UI to display protocol
- [ ] UI to interactively customize protocol
- [ ] UI to create protocol
- [ ] refactor commandHelper processValue functions, organize them in a more principled way to avoid duplication of various type checks.
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
