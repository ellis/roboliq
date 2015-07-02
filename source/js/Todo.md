- [x] test.js: fillStateItems(): add more handling for objects in ourlab.js
- [x] ourlab.js: remove autogenerate logic
- [x] roboliq.js: create file for command handlers and plan converters
- [x] put protocol JSON into separate JSON files
- [x] refactor names of movePlate commands (use "transporter." prefix)
- [x] create test protocol with sealing, where plate needs to be moved to the sealer first (add a 'movePlate' command, which will be expanded in turn)
- [x] test.js: handle errors from commandHandler call
- [x] test.js: accumulate warnings from commandHandler call
- [ ] generate instructions, pass them to Evoware compiler, checkout final script
- [ ] how to handle pipetting commands?
- [ ] how to track the state of wells?
- [ ] test.js: pass JSON and JavaScript files to load from the command line
- [ ] commandHandler args: should probably create a single object to pass in to ease adaptation of call parameters
- [ ] commandHandler: also allow for returning alternative parameter values, either for individual parameters or groups of parameters
- [ ] ourlab.js: add 'sites' and 'sealing' namespaces
- [ ] consider using uppercase for special JSON fields, like TYPE, ROBOLIQ, DEFAULTS, COMMAND

## Before publication, but not yet

- [ ] llpl.js: need to add a function to create a database object, so that we can use multiple instances simultaneously
- [ ] add warning/error handling to objectToLogicConverters
- [ ] change commandHandlers to return an object with both descriptive and 'handle' functions; the descriptive function should contain a description and information about the parameters
- [ ] test the creation of custom functions in a user's protocol
- [ ] test the usage of a separate protocol as part of a new protocol (test re-use); particularly trick will be object merging and gathering all required step parameters into a single parameters map
- [ ] commands/sealer.js: figure out how to let the biologist handle commands that aren't setup for the lab yet
- [ ] version handling for protocols and commands

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
* Use as many tips as possible at once
* Break groups if they are not contiguous, possibly washing or starting back at tip 1 or whatever


