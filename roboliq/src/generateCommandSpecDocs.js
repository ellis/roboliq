import _ from 'lodash';
import fs from 'fs';
import path from 'path';
import yaml from 'yamljs';

function toMarkdown(o, name) {
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

const filename_l = _.filter(fs.readdirSync(__dirname+"/commandSpecs/"), s => path.extname(s) === ".yaml");
const commandSpecs_l = _.map(filename_l, filename => yaml.load(__dirname+"/commandSpecs/"+filename));
const commandSpecs = _.merge.apply(_, [{}].concat(commandSpecs_l));
const s = _.map(commandSpecs, toMarkdown).join('\n\n')
console.log(s)
