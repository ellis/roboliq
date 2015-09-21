- [x] test.js: fillStateItems(): add more handling for objects in ourlab.js
- [x] ourlab.js: remove autogenerate logic
- [x] roboliq.js: create file for command handlers and plan converters
- [x] put protocol JSON into separate JSON files
- [x] refactor names of movePlate commands (use "transporter." prefix)
- [x] create test protocol with sealing, where plate needs to be moved to the sealer first (add a 'movePlate' command, which will be expanded in turn)
- [x] test.js: handle errors from commandHandler call
- [x] test.js: accumulate warnings from commandHandler call
- [x] generate instructions, pass them to Evoware compiler, checkout final script
- [x] test.js: pass JSON and JavaScript files to load from the command line
- [x] test.js: let user pass in filename to use for output
- [x] test.js: default to output name having same name as protocol, but with "out.json" extension (e.g. protocol3.out.json)
- [x] save output files in git for testing comparisons when code changes are made
- [x] test protocol4.json
- [x] EvowareCompiler: `pipetter._dispense`
- [x] EvowareCompiler: parse volumes (e.g. with units such as 'ul')
- [x] `pipetter._pipette`: create instruction, test in protocol5.json
- [x] commands/pipetter.js: pipetter.pipette: method 1
- [x] `pipetter._cleanTips`: create instruction, test in protocol8.json
- [x] commands/pipetter.js: pipetter.pipette: refresh tips (simple)
- [x] commandHandler args: should probably create a single object to pass in to ease adaptation of call parameters
- [x] commands/pipetter.js: pipetter.pipette: choose the default liquid class intelligently
- [x] commands/pipetter.js: `pipetter._pipette`: output effects array for changes in well contents
- [x] commands/pipetter.js: pipetter.pipette: method 2
- [x] commands/pipetter.js: pipetter.pipette: clean 4 tips at a time
- [x] commands/pipetter.js: pipetter.pipette: method 3
- [x] commands/pipetter.js: pipetter.pipette: handle multiple possible source wells for a single item
- [x] commands/pipetter.js: pipetter.pipette: for cleaning, sort out "begin, end, before, after" parameters
- [x] commandHandler: also allow for returning alternative parameter values, either for individual parameters or groups of parameters
- [x] ourlab.js: add 'sites' and 'washProgram' namespaces
- [x] test.js: '-O' parameter for output directory
- [x] test.js: allow for loading YAML protocols
- [x] ourlab.js: setup labware models
- [x] start creating unit tests
- [x] setup module for processing protocols as a function
- [x] create unit test for a pipetting protocol
- [x] roboliq.js: generate table for labware
- [x] roboliq.js: generate table for sourceWells (which stock goes where in what volume)
- [x] commands/pipetter/pipetterUtils.js: refactor functions to take 'objects' instead of 'data'
- [x] debug: node main.js -O protocols/output protocols/tania13_ph_1_balancePlate.yaml
- [x] misc.js: getObjectsValue(), change arguments so that name is first, then 'objects' and 'effects'
- [x] initialize system liquid's volume to "Infinity l"
- [x] for uninitialized source volumes, default to "NaN l"
- [x] handle opening and closing of centrifuge during transport
- [x] roboliq.js: handle directives
- [x] misc.js: use `_.get`
- [x] #for: create directive that can handle all the other factorial directives
- [x] commands/pipetter.js: pipetter.pipetteMixtures: allow for sorting items by index, destination (default to index)
- [x] `#mixtureList`
- [x] "#destinationWells#mixPlate(all row-jump(1))"
- [x] "#length#mixtures"
- [x] `#replaceLabware`
- [x] create new script for old tania13_ph_2_pipette
- [x] command: repeat
- [x] command: timer
- [x] command: centrifuge run
- [x] command: centrifuge.centrifuge2
- [x] handle opening and closing of centrifuge during transport (write unit test)

## Before submission

- [x] commands/fluorescenceReader.js: update based on sealer code
- [x] consider renaming `pipetter.instruction.pipette => pipetter._pipette`
- [x] set READER.closed = true
- [x] replace `centrifuge._close` with `equipment._close`
- [x] consider using commands/equipment.js
- [x] change Equipment.open => Equipment.closed, with appropriate modifications
- [x] create new script for old tania13_ph_3_measure
- [x] compile to evoware all instructions for tania13_1 protocol
- [x] compile to evoware all instructions for tania13_2b protocol
- [x] compile to evoware all instructions for tania13_3 protocol
- [x] equipment.run for reader: we probably need to use a different ID rather than the carrierName
- [x] figure out how to implement variable expansion
- [x] support creation of well groups, including random ones
- [x] handle "description"s in steps
- [x] roboliq.js: generate table for final well contents
- [x] implement 'import' keyword for protocols to import other protocols
- [x] roboliq.js: should be able to pass multiple --file-data args
- [x] test the creation of custom functions in a user's protocol using a template object
- [x] commands/roboliq.js: import command handlers
- [x] optimize mergeProtocols by optimizing how directives are handled in objects -- currently all the merging is slowing things down significantly
- [x] commands/ourlab.js: import commands/roboliq.js
- [x] require 'roboliq' field for protocols
- [ ] add 'roboliq' version to output
- [ ] implement `tania*` scripts for denaturation and renaturation
- [ ] UI to display protocol
- [ ] UI to interactively customize protocol
- [ ] UI to create protocol
- [ ] use a generic incubate command in tania13_ph_3_measure, instead of centrifuge.run
- [ ] write program to generate part of ourlab.mario from the evoware config files ('siteModel', 'stackable')
- [ ] move source code to 'src' subdir
- [ ] support creation of well groups by commands, so that the same wells can be used by later commands; pass in a well group name to the generating command, and have it populate that well group as an effect
- [ ] add program version info to the protocol output
- [ ] add commandline arguments to the protocol output
- [ ] test the usage of a separate protocol as part of a new protocol (test re-use); particularly tricky will be object merging and gathering all required step parameters into a single parameters map
- [ ] figure out how to pass commands without handlers onto the biologist
- [ ] JSON schema
- [ ] documentation for the avaiable commands and their parameters
- [ ] code documentation
- [ ] upload to a public repository
- [ ] change imports to require a version number?
- [ ] change commandHandlers to return an object with both descriptive and 'handle' functions; the descriptive function should contain a description and information about the parameters

## After submission

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


# Encoding well content

Well contents are encoded as an array.
The first element always holds the volume in the well.
If the array has exactly one element, the volume should be 0l.
If the array has exactly two elements, the second element is the name of the substance.
If the array has more than two elements, each element after the volume has the same
structure as the top array and they represent the mixture originally dispensed in the well.

        objects:
            plate1:
                contents:
                    A01: ["30ul", ["25ul", "water"], ["5ul", "reagent1"]]


# Combinatorial stuff

* tableRows: property names given as row of column names, values as remaining rows, defaults as objects
* tableCols: property names as property names, values in array
* for: for comprehension with keys: factors (array), replicates (integer), order (function), output (value), flatten (boolean or 'deep')
* factorialCols: property names as property names, values in array
* factorialArrays: combinatorial combination of an element from each input array
* merge: merge an array of objects
* factorialMerge: factorial merging of array of arrays of objects (like ``factorialArrays >> map merge``)
* replicate

    #factorialTemplate:
        variables:
        - a: [1, 2, 3]
        - b: [3, 4, 5]
        - c: {name: "bob"}
        template:
            hello: "'{{a}}'",
            n: {{b}}
            c: ${c}


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