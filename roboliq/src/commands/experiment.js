import _ from 'lodash';
import yaml from 'yamljs';
import commandHelper from '../commandHelper.js';
import expect from '../expect.js';
import misc from '../misc.js';

const commandHandlers = {
	"experiment.run": function(params, parsed, data) {
		console.log("experiment.run");
		console.log(JSON.stringify(parsed, null, '\t'));
		const experiment = parsed.value.experiment;

		return {
			expansion: expansion,
			effects: effects,
			alternatives: alternatives
		};
	}
};

module.exports = {
	roboliq: "v1",
	schemas: yaml.load(__dirname+"/../schemas/experiment.yaml"),
	commandHandlers
};
