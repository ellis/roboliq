import _ from 'lodash';
import centrifuge from './commands/centrifuge.js';
import equipment from './commands/equipment.js';

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

const l0 = [centrifuge, equipment];
const l1 = _.compact(_.map(l0, x => x.commandSpecs));
const commandSpecs = _.merge.apply(_, [{}].concat(l1));
const s = _.map(commandSpecs, toMarkdown).join('\n\n')
console.log(s)
