This document contains information for software developers to help them get
started programming Roboliq's source code.

Roboliq is written in JavaScript and using the [Node.js](https://nodejs.org/) platform.
Before working with the code, please install Node.js, which provides the
Node Pack Manager (`npm`).
You can then install the libraries that Roboliq depends on by changing
to the project's root (where the `package.json` file is) and running:

``npm install``

## Command-line commands

The most commonly used commands are:

* ``npm test`` -- Run Roboliq's unit tests.

* ``npm run jsdoc`` -- Generate the HTML documentation.

Less commonly used commands include:

* ``npm run pegjs`` -- Update the parser for well locations.

* ``npm run generateSchemaDocs`` -- Update the schema-related documentation in `tutorias/Commands.md` and `tutorials/Object_Types.md` after modifications to any of the schema files in `schemas/` or the generating function in `src/generateSchemaDocs.js`.

## Directory structure

Here is a directory listing with the most important files and directories.

<pre>
├── README.md -- project description
├── Todo.md -- todo list
├── beautify.sh -- script for reformatting JavaScript code
├── jsdoc.json -- configuration file for JSDoc documentation
├── out/ -- output directory for JSDoc documentation
├── package.json -- configuration file for npm
├── protocols/ -- sample protocol files
├── runall.sh -- compile all sample protocol files
├── src -- source code
│   ├── commands/ -- command handlers
│   ├── config/ -- Roboliq and robot configuration files
│   │   ├── ourlab.js
│   │   ├── roboliq.js
│   │   └── roboliqDirectiveHandlers.js
│   ├── HTN/ -- logic planning
│   │   ├── llpl.js
│   │   ├── shop.js
│   │   └── utils.js
│   ├── parsers/ -- Roboliq parsers
│   │   ├── wellsParser.js -- parser for well specifier, wraps wellParser0
│   │   ├── wellsParser0.js -- generated parser
│   │   └── wellsParser0.pegjs -- PEGJS description of well parser
│   ├── schemas/ -- schemas for Roboliq's commands and objects
│   ├── commandHelper.js -- helper functions for command handlers
│   ├── expect.js -- functions for catching errors
│   ├── expectCore.js -- functions for catching errors
│   ├── generateSchemaDocs.js -- program to generate tutorials from schemas
│   ├── main.js -- main program
│   ├── misc.js -- some miscellaneous helper functions
│   ├── roboliq.js -- the core Roboliq library
│   ├── roboliqSchemas.js -- a few schemas for Roboliq types
│   └── WellContents.js -- class to handle well contents
├── tests/ -- unit tests
└── tutorials/ -- Markdown files that are included as tutorials in the JSDoc documentation
<pre>

The three most important files/directories for developers are:

* `src/roboliq.js`: if you want to change how Roboliq works,
    this is probably where you'll start.

* `src/commands/`: CONTINUE

* `src/config/`: CONTINUE
