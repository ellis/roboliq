# Roboliq

Roboliq compiles biological protocols specifications down to low-level commands
for robotic lab automation.

# Setup

```{sh}
npm install
```

# Usage

To run a protocol:

```{sh}
npm start -- --help
npm start -- [options] ${PROTOCOL}
```

To compile a protocol for evoware, use `npm run evoware`.  Here's an example:

```{sh}
npm run evoware -- ../testdata/bsse-luigi/Carrier.cfg ../testdata/bsse-luigi/DM_WorkTable_June2015.ewt protocols/output/protocol3.cmp.json ourlab.mario.evoware
```

# Development

To run the tests:

```{sh}
npm test
```

To update the Markdown tutorials for Commands and Types:

```{sh}
npm run generateSchemaDocs
```

To update the parser for well locations:

```{sh}
npm run pegjs
```

To generate the HTML documentation:

```{sh}
npm run jsdoc
```
