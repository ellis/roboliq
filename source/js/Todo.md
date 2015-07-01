- [x] test.js: fillStateItems(): add more handling for objects in ourlab.js
- [x] ourlab.js: remove autogenerate logic
- [x] roboliq.js: create file for command handlers and plan converters
- [x] put protocol JSON into separate JSON files
- [ ] create test protocol with sealing, where plate needs to be moved to the sealer first (add a 'movePlate' command, which will be expanded in turn)
- [ ] refactor names of movePlate commands (use "transporter." prefix)
- [ ] pass JSON and JavaScript files to load from the command line
- [ ] generate instructions, pass them to Evoware compiler, checkout final script
- [ ] add warning/error handling to objectToLogicConverters
- [ ] test the creation of custom functions in a user's protocol
- [ ] test the usage of a separate protocol as part of a new protocol (test re-use)

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
