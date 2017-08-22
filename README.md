# Roboliq

Roboliq aims to make it easier to use liquid handling robots for automation
in biological laboratories.

It lets you write protocols that are portable between labs,
and it compiles the protocols for execution by liquid handling robots.

The only supported backend is for Tecan Evoware robots, but other backends
can also be added.

# Installation

This guide assumes that you have some familiarity with using the command line terminal.

* Install [`nodejs`](https://nodejs.org/en/download/), which lets you execute
  Javascript programs.
* If you're using Microsoft Windows, install the [cygwin terminal](https://cygwin.com/install.html),
  which will be used for typing in commands.
* Copy the Roboliq repository to your computer.
* Open the terminal, navigate to the directory where you copied the Roboliq
  repository, and run `npm install` to download Roboliq's software requirements.
* To generate the documentations, run `npm run docs`.  You can then find the
	documentation in the `docs` subfolder with three sub-subfolders: `protocol`,
	`roboliq-evoware`, and `roboliq-processor`.  In each of those, you can open
	the `index.html` file.

# Documentation

For more information, please see one of the following sources of documentation:

* [Manual](https://ellis.github.io/roboliq/manual/index.html) -- the Roboliq manual
* [Protocol](https://ellis.github.io/roboliq/protocol/index.html) -- reference documentation for the commands and type available in Roboliq protocols
* [Processor API](https://ellis.github.io/roboliq/roboliq-processor/index.html) -- programmer documentation for Roboliq's protocol processor
* [Evoware API](https://ellis.github.io/roboliq/roboliq-evoware/index.html) -- programmer documentation for Roboliq's Evoware backend
