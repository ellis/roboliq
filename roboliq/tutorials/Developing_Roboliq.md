This document contains information for software developers to help them get
started programming Roboliq's source code.

Roboliq is written in JavaScript and using the [Node.js](https://nodejs.org/) platform.
Before working with the code, please install Node.js, which provides the
Node Pack Manager (`npm`).
You can then install the libraries that Roboliq depends on by changing
to the project's root (where the `package.json` file is) and running:

``npm install``

## Running Roboliq



```{sh}
npm run roboliq -- [options] ${PROTOCOL}
```

## Development

The most commonly used commands are:

* ``npm test`` -- Run Roboliq's unit tests.

* ``npm run jsdoc`` -- Generate the HTML documentation.

Less commonly used commands include:

* ``npm run pegjs`` -- Update the parser for well locations.

* ``npm run generateSchemaDocs`` -- Update the schema-related documentation in `tutorias/Commands.md` and `tutorials/Object_Types.md` after modifications to any of the schema files in `schemas/` or the generating function in `src/generateSchemaDocs.js`.

## Directory structure

Here is a directory listing with the most important files and directories.

```
├── README.md
├── Todo.md
├── beautify.sh
├── jsdoc.json
├── out/
├── package.json
├── protocols/
├── runall.sh
├── src
│   ├── commands
│   │   ├── pipetter
│   │   │   ├── groupingMethods.js
│   │   │   ├── pipetterUtils.js
│   │   │   └── sourceMethods.js
│   │   ├── centrifuge.js
│   │   ├── equipment.js
│   │   ├── fluorescenceReader.js
│   │   ├── pipetter.js
│   │   ├── sealer.js
│   │   ├── system.js
│   │   ├── timer.js
│   │   ├── transporter.js
│   │   └── transporterLogic.json
│   ├── config
│   │   ├── ourlab.js
│   │   ├── roboliq.js
│   │   └── roboliqDirectiveHandlers.js
│   ├── HTN
│   │   ├── llpl.js
│   │   ├── shop.js
│   │   └── utils.js
│   ├── parsers
│   │   ├── sourceParser.js
│   │   ├── sourceParser0.js
│   │   ├── sourceParser0.pegjs
│   │   ├── wellsParser.js
│   │   ├── wellsParser0.js
│   │   └── wellsParser0.pegjs
│   ├── schemas
│   │   ├── centrifuge.yaml
│   │   ├── equipment.yaml
│   │   ├── fluorescenceReader.yaml
│   │   ├── pipetter.yaml
│   │   ├── roboliq.yaml
│   │   ├── sealer.yaml
│   │   ├── system.yaml
│   │   ├── timer.yaml
│   │   └── transporter.yaml
│   ├── commandHelper.js
│   ├── expect.js
│   ├── expectCore.js
│   ├── generateSchemaDocs.js
│   ├── main.js
│   ├── misc.js
│   ├── roboliq.js
│   ├── roboliqSchemas.js
│   └── WellContents.js
├── tests/
└── tutorials/
```
