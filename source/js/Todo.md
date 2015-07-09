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
- [x] EvowareCompiler: pipetter.instruction.dispense
- [x] EvowareCompiler: parse volumes (e.g. with units such as 'ul')
- [x] pipetter.instruction.pipette: create instruction, test in protocol5.json
- [x] commands/pipetter.js: pipetter.action.pipette: method 1
- [x] pipetter.instruction.cleanTips: create instruction, test in protocol8.json
- [x] commands/pipetter.js: pipetter.action.pipette: refresh tips (simple)
- [x] commandHandler args: should probably create a single object to pass in to ease adaptation of call parameters
- [x] commands/pipetter.js: pipetter.action.pipette: choose the default liquid class intelligently
- [x] commands/pipetter.js: pipetter.instruction.pipette: output effects array for changes in well contents
- [x] commands/pipetter.js: pipetter.action.pipette: method 2
- [x] commands/pipetter.js: pipetter.action.pipette: clean 4 tips at a time
- [x] commands/pipetter.js: pipetter.action.pipette: method 3
- [x] commands/pipetter.js: pipetter.action.pipette: handle multiple possible source wells for a single item
- [x] commands/pipetter.js: pipetter.action.pipette: for cleaning, sort out "begin, end, before, after" parameters
- [x] commandHandler: also allow for returning alternative parameter values, either for individual parameters or groups of parameters
- [x] ourlab.js: add 'sites' and 'washProgram' namespaces

## Before publication, but not yet

- [x] test.js: '-O' parameter for output directory
- [x] test.js: allow for loading YAML protocols
- [x] ourlab.js: setup labware models
- [x] start creating unit tests
- [x] setup module for processing protocols as a function
- [x] create unit test for a pipetting protocol
- [x] roboliq.js: generate table for labware
- [ ] roboliq.js: generate table for sources (which stock goes where in what volume)
- [ ] roboliq.js: generate table for final well contents
- [ ] roboliq.js: generate table of pipetting actions for well contents by step
- [ ] protocols/protocol8.json: add all cleaning intensities for all tips
- [ ] write program to generate part of ourlab.mario from the evoware config files
- [ ] reader command
- [ ] mix command
- [ ] support creation of well groups, including random ones, and then implement 'free' verb in wellsParser
- [ ] support creation of well groups by commands, so that the same wells can be edited future commands; pass in a well group name to the generating command, and have it populate that well group as an effect
- [ ] create more systematic tests of commands using protocols, adapt old test protocols
- [ ] commands/pipetter.js: pipetter.action.pipette: method 4
- [ ] figure out how to implement variable expansion/evaluation
- [ ] figure out how to implement for-comprehensions for both commands and list
- [ ] figure out how to randomize for-comprehensions and plain lists
- [ ] figure out how to generate partial factorial design lists
- [ ] figure out how to split large factorial designs over multiple plates/batches
- [ ] consider allowing for mathematical expressions (see http://mathjs.org/)
- [ ] consider saving the commandline arguments in the protocol output; also adding program versions or something?
- [ ] for 'ourlab' configuration, handle table selection, for different table configurations
- [ ] consider whether to do typechecking of all parameters automatically -- in this case, the "program" parameter to movePlate, for example, would either need to be an object reference or an explicit string
- [ ] llpl.js: need to add a function to create a database object, so that we can use multiple instances simultaneously
- [ ] add warning/error handling to objectToLogicConverters
- [ ] change commandHandlers to return an object with both descriptive and 'handle' functions; the descriptive function should contain a description and information about the parameters
- [ ] test the creation of custom functions in a user's protocol
- [ ] test the usage of a separate protocol as part of a new protocol (test re-use); particularly trick will be object merging and gathering all required step parameters into a single parameters map
- [ ] commands/sealer.js: figure out how to let the biologist handle commands that aren't setup for the lab yet
- [ ] version handling for protocols and commands
- [ ] centrifuge command
- [ ] commands/pipetter.js: pipetter.instruction.aspirate: output effects array for changes in well and tip contents
- [ ] commands/pipetter.js: pipetter.instruction.dispense: output effects array for changes in well and tip contents
- [ ] commands/pipetter.js: pipetter.action.pipette: refresh tips (advanced)
- [ ] JSON schema
- [ ] how to handle trough volume, which has many virtual wells, but they all share the same common liquid: set the labware's contents, rather than the contents of the individual wells, then the effects function should handle that case appropriately.

## After publication

- [ ] commandHandler: allow for returning of property constraints, for example: the sealPlate command could return a list of possible plate models for the given plate
- [ ] commandHandler args: also pass paramDefaults
- [ ] commands/sealer.js: figure out more sophisticated way to deal with agents for the pre/post steps; consider 'agentPreferred' parameter
- [ ] handle lids on plates and tracking their position
- [ ] add a default storage site for plates?  How to handle when plates are shared between robots?
- [ ] commands/pipetter.js: handle case of dispensing then later aspirating from a single well in a single pipetting command

# Notes

When loading JSON/JavaScript files, we expect the following structure:

    {
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

# Variables

Need to reference objects, variables, step parameters, parameters of current command, parameters of parent command

* "${object}" -- `object` parameter for current command
* "${^.object}" -- `object` parameter for parent command
* "${1.2.object}" -- `object` parameter for step 1.2
* "@{volume}" -- the value of `objects.volume.value`
* "@{${object}.location}" -- the location of the object referenced by the `object` parameter for the current command

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


# Encoding content

Several ideas for how to encode content.  Currently I prefer `plate1(C01)` below, using the arrays.

        "contents": {
            "plate1(A01)": "(water=25ul+asdf=5ul)@?-10ul",
            "plate1(B01)": {
                "contents": [
                    {"contents": "water", "AMOUNT":"25ul"},
                    {"contents": "asdf", "AMOUNT":"5ul"}
                ],
                "amount": "?-10ul"
            },
            "plate1(C01)": ["?ul-10ul", ["25ul", "water"], ["5ul", "asdf"]]
        },


# Combinatorial stuff

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
        "command": "pipetter.action.pipette",
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

    for:
    - [source, volume] in [[water, 10ul], [reagent1, 20ul]]
    - destination in plate1(A02 down 8)
    template:
      source: {{source}}
      destination: {{destination}}
      volume: {{volume}}

      - {source: water, destination: plate1(A02), volume: 10ul}
      - {source: water, destination: plate1(B02), volume: 10ul}

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

    reports:
        wells:
            plate1(A01):
                isSource: true
                contentsInitial:
                    water: 0ul
                volumeAdded: XXX
                volumeRemoved: 60ul
                contentsFinal:
                    water: -60ul
        sources:
            water:
        sourceWells:
            water:
            - well: plate1(A01)
              volume: 0ul
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
