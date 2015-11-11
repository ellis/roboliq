import _ from 'lodash';
import fs from 'fs';
import path from 'path';
import yaml from 'yamljs';

function toMarkdown(pair) {
	const [name, o] = pair;
	return _.flattenDeep([
		`## ${name}`,
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

const filename_l = _.filter(fs.readdirSync(__dirname+"/schemas/"), s => path.extname(s) === ".yaml");
const schemas_l = _.map(filename_l, filename => yaml.load(__dirname+"/schemas/"+filename));
const schemas = _.merge.apply(_, [{}].concat(schemas_l));
const [objectSchemas, commandSchemas] = _.partition(_.pairs(schemas), ([name, schema]) => name[0] === name[0].toUpperCase());

const objectSchemasText = _.map(objectSchemas, toMarkdown).join('\n\n')
fs.writeFileSync(__dirname+"/../tutorials/Object_Types.md", objectSchemasText);

const commandSchemasText = _.map(commandSchemas, toMarkdown).join('\n\n')
fs.writeFileSync(__dirname+"/../tutorials/Commands.md", commandSchemasText);
