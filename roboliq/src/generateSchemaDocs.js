/**
 * @file Generate documentation from the schemas in `schemas/*.yaml`
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
		return `\n## ${name}\n\n${o.module}`;
	}
	else {
		return _.flattenDeep([
			`### ${name}`,
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
		]).join('\n');
	}
}

// All files in the schema/ directory
const filename_l = _.filter(fs.readdirSync(__dirname+"/schemas/"), s => path.extname(s) === ".yaml");
// Load the schemas from the files
const schemas_l = _.map(filename_l, filename => yaml.load(__dirname+"/schemas/"+filename));
// Merge all schemas together
const schemas = _.merge.apply(_, [{}].concat(schemas_l));
// Separate object schemas from command schemas
const [objectSchemas, commandSchemas] = _.partition(_.pairs(schemas), ([name, schema]) => name[0] === name[0].toUpperCase());

// Generate documentation for object types
const objectSchemasText = _.map(objectSchemas, toMarkdown).join('\n\n')
fs.writeFileSync(__dirname+"/../tutorials/Object_Types.md", objectSchemasText);

// Generate documentation for commands
const commandSchemasText = _.map(commandSchemas, toMarkdown).join('\n\n')
fs.writeFileSync(__dirname+"/../tutorials/Commands.md", commandSchemasText);
