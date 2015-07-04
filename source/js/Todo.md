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
- [ ] commands/pipetter.js: pipetter.action.pipette: method 1 plus refresh tips
- [ ] how to track the state of wells?
- [ ] commands/pipetter.js: pipetter.action.pipette: method 2
- [ ] commands/pipetter.js: pipetter.action.pipette: method 3
- [ ] commands/pipetter.js: pipetter.action.pipette: method 4
- [ ] commandHandler args: should probably create a single object to pass in to ease adaptation of call parameters
- [ ] commandHandler: also allow for returning alternative parameter values, either for individual parameters or groups of parameters
- [ ] ourlab.js: add 'sites' and 'sealing' namespaces
- [ ] consider using uppercase for special JSON fields, like TYPE, ROBOLIQ, DEFAULTS, COMMAND

## Before publication, but not yet

- [ ] reader command
- [ ] mix command
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
- [ ] test.js: allow for loading YAML protocols
- [ ] centrifuge command

## After publication

- [ ] commandHandler: allow for returning of property constraints, for example: the sealPlate command could return a list of possible plate models for the given plate
- [ ] commandHandler args: also pass paramDefaults
- [ ] commands/sealer.js: figure out more sophisticated way to deal with agents for the pre/post steps; consider 'agentPreferred' parameter
- [ ] handle lids on plates and tracking their position
- [ ] add a default storage site for plates?  How to handle when plates are shared between robots?


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
"#expr#wells(plate1, A01, 4)"
plate1(A01)
plate1(A01, B01, C01, D01)
plate1(A01, 4)
plate1(A01, 4 columns)
plate1(4)
plate1(4 empty)
plate1(4 from A01)
plate1(4 from A01 column-wise)
plate1(4 from A01 row-wise)
plate1(A01 to D01 column-wise)
plate1(A01 to B02 block-wise)

Simplest algorithms:

* Only use one tip
* Use each tip, rotating through them till they need to be washed
* Group as many tips at once as possible
* Group as many tips at once as possible, but if group splits over columns, see if we can get a longer contiguous group by restarting the grouping in the new column rather than splitting the group from the previous column

# Combinatorial stuff

somehow distinguish between control factors, nuisance factors, blocking factors, measurable factors, unknown factors

- factor: [protein, volume]
  values: [[gfp, 10], [gfp, 20], [gfp, 30], [yfp, 5]]
- factor: something else
  values: [1, 2, 3, 4]
- "#tabfile#factor3.tab"
- "#csvdata#protein,volume\ngfp,10\ngfp,20\ngfp,30\nyfp,5"
