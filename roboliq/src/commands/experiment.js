import _ from 'lodash';
import yaml from 'yamljs';
import commandHelper from '../commandHelper.js';
import expect from '../expect.js';
import misc from '../misc.js';

const commandHandlers = {
	"experiment.run": function(params, parsed, data) {
		//console.log("experiment.run");
		//console.log(JSON.stringify(parsed, null, '\t'));
		const experiment = parsed.value.experiment;

		const expansion = {};
		if (parsed.value.steps) {
			for (let i = 0; i < experiment.length; i++) {
				expansion[i+1] = _.cloneDeep(parsed.value.steps);
				expansion[i+1]._scope = _.cloneDeep(experiment[i]);
			}
		}

		//console.log(JSON.stringify(expansion, null, "\t"))
		return {expansion};
	}
};

module.exports = {
	roboliq: "v1",
	schemas: yaml.load(__dirname+"/../schemas/experiment.yaml"),
	commandHandlers
};
