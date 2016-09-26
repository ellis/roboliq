/**
 * Generate documentation from the schemas in `schemas/*.yaml`.
 * Running this module will create these files:
 *
 * * `tutorials/Object_Types.md`
 * * `tutorials/Commands.md`
 *
 * @module
 */

import _ from 'lodash';
import fs from 'fs';
import path from 'path';
import yaml from 'yamljs';

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
			_.map(o.properties, (p, pName) => {
				const isRequired = _.includes(o.required, pName);
				const nameText = (isRequired) ? pName : `[${pName}]`;
				const nameTypeText = (p.type) ? `${nameText}: ${p.type}` : nameText;
				const descriptionText = p.description || "";
				return `* \`${nameTypeText}\` -- ${descriptionText}`;
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
 * Convert a name/schema pair to jsdoc text.
 * @param {array} pair - [name, schema]
 * @return A markdown string.
 */
function toJsdoc(pair) {
	const [name, o] = pair;
	//console.log({name, o})
	const s = _.flattenDeep([
		o.description ? [o.description, ""] : [],
		`@typedef ${name}`,
		"@memberof types",
		_.map(o.properties, (p, pName) => {
			const isRequired = _.includes(o.required, pName);
			const nameText = (isRequired) ? pName : `[${pName}]`;
			const typeText = (p.type) ? `{${_.flatten([p.type]).join("|")}}` : "";
			const descriptionText = (p.description) ? `- ${p.description}` : "";
			return `@property ${typeText} ${nameText} ${descriptionText}`;
		}),
		(_.isEmpty(o.example)) ? [] : [
			"@example",
			o.example
		]
	]).join('\n').split("\n").map(s => " * "+s).join("\n");
	return "/**\n" + s + "\n */\n\n";
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

// Generate documentation for object types
const generatedTypesText = "/**\n * Namespace for the object types available in Roboliq.\n * @namespace types\n * @version v1 \n */\n\n" + _.map(objectSchemas, toJsdoc).join('\n\n');
fs.writeFileSync(__dirname+"/generatedTypes.js", generatedTypesText);
