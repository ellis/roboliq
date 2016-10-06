# Todos

# Documentations TODOS

* [ ] distribution documentation
	* [ ] src/roboliq/README.md
		* [ ] Getting Started
			* [ ] a protocol that doesn't require any configuration or backend
			* [ ] a protocol with a minimal configuration
			* [ ] a protocol with a backend
			* [ ] a protocol compiled for Evoware
			* [ ] running the server and getting the data during execution of an Evoware script
- [ ] user documentation (see <http://usejsdoc.org/about-tutorials.html>)
	- [x] Commands.md: Add general documentation to each command namespace
	- [ ] Commands.md: add examples for each command
	- [ ] generatedTypes: every type should completely define type, description, example
	- [ ] document the properties of the types (i.e. add 'description' field)
	- [ ] WritingADesign.md
	- [ ] Developing_Roboliq.md
	- [ ] Object_Types.md: Add general documentation to top of page
	- [ ] Using_Roboliq.md
		- [ ] add reference to WritingAProtocol.md
	- [ ] WritingAProtocol.md
	- [ ] Cookbook.md: explaining how to solve specific problems
	- [ ] Configuring a lab (e.g. `config/ourlab.js`)
	- [ ] for all commands, include documentation about required logic (e.g. transporter, equipment, pipetter)
	- [ ] convention for syringes being in 'syringe' property of pipetter
	- [ ] pipetter.cleanTips: document the various allowed parameter combinations of equipment, items, syringes, intensity
	- [ ] document the directives, such as "#createPipetteMixtureList"
- [ ] configuration documentation
	- [ ] models
	- [ ] agents
	- [ ] ...
- [ ] code documentation
	- [x] roboliq.js
	- [x] WellContents.js
	- [x] generateSchemaDocs.js
	- [x] commandHelper.js
	- [ ] figure out how to reference an anchor from a separate file (e.g. commands/centrifuge.js should reference 'centrifuge' in Commands.md)
			- continue with centrifuge.js documentaiton header (currently have experiments in there)
	- [ ] generateSchemaDocs.js: set anchors for command modules, so they can be referenced from the source code
	- [ ] generateSchemaDocs.js: why aren't descriptions generated for "Object Types" properties?
	- [ ] for parameter types, can they be links to the Type definition?
	- [ ] check generated jsdoc, and make appropriate improvements (e.g. setting modules and indicating which methods are exported)
	- [ ] generateSchemaDocs.js shouldn't be listed on the Home page of the jsdocs (probably need to remote `@file`)
	- [ ] expectCore.js
	- [ ] expect.js
	- [ ] main.js
	- [ ] misc.js
	- [ ] commands...
	- [ ] document roboliq's extensions to JSON Schema (types, 'module')
- [ ] sort through `notes`, `doc`, and `old` directories
