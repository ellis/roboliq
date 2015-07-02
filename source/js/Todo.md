- [x] test.js: fillStateItems(): add more handling for objects in ourlab.js
- [x] ourlab.js: remove autogenerate logic
- [x] roboliq.js: create file for command handlers and plan converters
- [x] put protocol JSON into separate JSON files
- [x] refactor names of movePlate commands (use "transporter." prefix)
- [ ] create test protocol with sealing, where plate needs to be moved to the sealer first (add a 'movePlate' command, which will be expanded in turn)
- [ ] test.js: handle errors from commandHandler call
- [ ] test.js: accumulate warnings from commandHandler call
- [ ] commandHandler args: should probably create a single object to pass in to ease adaptation of call parameters
- [ ] commandHandler args: also pass paramDefaults
- [ ] commandHandler: also allow for returning alternative parameter values, either for individual parameters or groups of parameters
- [ ] commandHandler: allow for returning of property constraints, for example: the sealPlate command could return a list of possible plate models for the given plate
- [ ] pass JSON and JavaScript files to load from the command line
- [ ] consider using uppercase for special JSON fields, like TYPE, ROBOLIQ, DEFAULTS, COMMAND
- [ ] generate instructions, pass them to Evoware compiler, checkout final script
- [ ] add warning/error handling to objectToLogicConverters
- [ ] change commandHandlers to return an object with both descriptive and 'handle' functions
- [ ] ourlab.js: add 'sites' and 'sealing' namespaces
- [ ] llpl.js: need to add a function to create an database object, so that we can use multiple instances simultaneously
- [ ] test the creation of custom functions in a user's protocol
- [ ] test the usage of a separate protocol as part of a new protocol (test re-use)
- [ ] handle lids on plates and tracking their position
- [ ] add a default storage site for plates?  How to handle when plates are shared between robots?
- [ ] commands/sealer.js: figure out more sophisticated way to deal with agents for the pre/post steps; consider 'agentPreferred' parameter
- [ ] commands/sealer.js: figure out how to let the user handle commands that aren't setup for the lab yet
- [ ] version handling for protocols and commands
- [ ] how to handle pipetting commands?
- [ ] how to track the state of wells?

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
