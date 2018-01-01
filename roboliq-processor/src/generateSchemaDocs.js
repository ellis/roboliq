/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017, ETH Zurich, Ellis Whitehead
 * @license GPL-3.0
 */

/**
 * Generate documentation from the schemas in `schemas/*.yaml`.
 * Running this module will create these files:
 *
 * * `tutorials/Object_Types.md`
 * * `tutorials/Commands.md`
 *
 * @module
 */

const _ = require('lodash');
const fs = require('fs');
const path = require('path');
const yaml = require('yamljs');

/**
 * Convert a name/schema pair to markdown text.
 * @param {array} pair - [name, schema]
 * @return A markdown string.
 */
function toMarkdown(pair) {
	const [name, o] = pair;
	//console.log({name, o})
	if (o.module) {
		return `\n## <a name="${name}"></a>${name}\n\n${o.module}`;
	}
	else {
		return _.flattenDeep([
			`### \`${name}\``,
			"",
			o.description ? [o.description, ""] : [],
			"Properties:",
			"",
			"Name | Type | Argument | Description",
			"-----|------|----------|------------",
			_.map(o.properties, (p, pName) => {
				const isRequired = _.includes(o.required, pName);
				// const nameText = (isRequired) ? pName : `[${pName}]`;
				// const nameTypeText = (p.type) ? `${nameText}: ${p.type}` : nameText;
				// const descriptionText = p.description || "";
				// return `* \`${nameTypeText}\` -- ${descriptionText}`;
				return `${pName} | ${p.type || ""} | ${(isRequired) ? "" : "*optional*"} | ${p.description || ""}`;
			}),
			(_.isEmpty(o.example)) ? [] : [
				"",
				"Example:",
				"",
				o.example,
				""
			]
		]).join('\n');
	}
}

/**
 * Take a string, split it on all newlines, prepend each newline with " * ", and then rejoin.
 * @param {string} s - string to indent
 * @return {string} - string indented by " * "
 */
function indentJsdocComment(s) {
	return s.trim().split("\n").map(s => " * "+s).join("\n");
}

/**
 * Convert a name/schema pair to jsdoc text.
 * @param {array} pair - [name, schema]
 * @param {boolean} isCommand - true if pair is a command, in which case the `memberOf` field is handled differently.
 * @return A markdown string.
 */
function typeToJsdoc(pair, isCommand = false) {
	const [name, o] = pair;
	//console.log({name, o})
	const s = _.flattenDeep([
		o.description ? [o.description, ""] : [],
		(isCommand) ? `@typedef "${name}"` : `@class ${name}`,
		//(isCommand) ? `@memberof ${_.initial(name.split(".")).join(".")}` : [],
		//(isCommand) ? `@memberof commands`: "@memberof types",
		_.map(o.properties, (p, pName) => {
			const isRequired = _.includes(o.required, pName);
			const nameText = (isRequired) ? pName : `[${pName}]`;
			const typeText = (p.type) ? `{${_.flatten([p.type]).join("|").replace(/ /g, "")}}` : "";
			const descriptionText = (p.description) ? `- ${p.description}` : "";
			return `@property ${typeText} ${nameText} ${descriptionText}`;
		}),
		(_.isEmpty(o.example)) ? [] : [
			"@example",
			o.example
		]
	]).join('\n').trim().split("\n").map(s => " * "+s).join("\n");
	return "/**\n" + s + "\n */\n\n";
}

/**
 * Convert a name/schema pair to markdown text.
 * @param {array} pair - [name, schema]
 * @return A markdown string.
 */
function commandToJsdoc(pair) {
	const [name, o] = pair;
	//console.log({name, o})
	if (o.module) {
		return `\n/**\n${indentJsdocComment(o.module)}\n *\n * @module ${name}\n */\n`;
	}
	else {
		return typeToJsdoc(pair, true);
	}
}

// All files in the schema/ directory
const filename_l = _.filter(fs.readdirSync(__dirname+"/schemas/"), s => path.extname(s) === ".yaml");
// Load the schemas from the files
const schemas_l = _.map(filename_l, filename => yaml.load(__dirname+"/schemas/"+filename));
// Merge all schemas together
const schemas = _.merge.apply(_, [{}].concat(schemas_l));
// Separate object schemas from command schemas
const [objectSchemas, commandSchemas] = _.partition(_.toPairs(schemas), ([name, schema]) => name[0] === name[0].toUpperCase());

// Generate documentation for object types
const objectSchemasText = _.map(objectSchemas, toMarkdown).join('\n\n');
fs.writeFileSync(__dirname+"/../tutorials/Object_Types.md", objectSchemasText);

// Generate documentation for commands
const commandSchemasText = _.map(commandSchemas, toMarkdown).join('\n\n');
fs.writeFileSync(__dirname+"/../tutorials/Commands.md", commandSchemasText);
fs.writeFileSync(__dirname+"/../generated/content/commands.md", commandSchemasText);

// Generate documentation for object types
const generatedTypesText = "/**\n * Namespace for the object types available in Roboliq protocols.\n * @namespace types\n * @version v1 \n */\n\n" + _.map(objectSchemas, x => typeToJsdoc(x)).join('\n\n');
fs.writeFileSync(__dirname+"/../generated/types.jsdoc", generatedTypesText);

// Generate documentation for commands
const generatedCommandsText = "/**\n * Namespace for the commands available in Roboliq protocols.\n * @namespace commands\n * @version v1 \n */\n\n" + _.map(commandSchemas, commandToJsdoc).join('\n\n');
fs.writeFileSync(__dirname+"/../generated/commands.jsdoc", generatedCommandsText);
