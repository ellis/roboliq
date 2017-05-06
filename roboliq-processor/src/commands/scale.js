import _ from 'lodash';
import yaml from 'yamljs';
import commandHelper from '../commandHelper.js';
import expect from '../expect.js';
import misc from '../misc.js';

const objectToPredicateConverters = {
	"Scale": function(name, object) {
		return [{ "isScale": { "equipment": name } }];
	},
};

const commandHandlers = {
	/*"scale.weigh": function(params, parsed, data) {
		const expansion = [
			{
				command: "equipment.run|"+params2.agent+"|"+params2.equipment,
				agent: params2.agent,
				equipment: params2.equipment,
				program: parsed.orig.program,
				object: parsed.objectName.object
			}
		];

		return {
			expansion: expansion,
			alternatives: alternatives
		};
	}*/
};

module.exports = {
	roboliq: "v1",
	objectToPredicateConverters,
	schemas: yaml.load(__dirname+"/../schemas/scale.yaml"),
	commandHandlers
};
