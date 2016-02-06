import _ from 'lodash';
import yaml from 'yamljs';
import commandHelper from '../commandHelper.js';
import expect from '../expect.js';
import misc from '../misc.js';


const commandHandlers = {
	"system.call": function(params, parsed, data) {
		switch (parsed.value.name.type) {
			case "Template":
				const expansion = misc.renderTemplate(parsed.value.name.template, params.params, data);
				return {expansion: expansion};
				break;
			default:
				expect.truthy({paramName: "name"}, false, "expected an object of type 'Template'");
				return {};
		}
	},
	"system.if": function(params, parsed, data) {
		//console.log("system.if:")
		//console.log({parsed, expansion: (parsed.value.test) ? parsed.value.then : parsed.value.else})
		return {expansion: (parsed.value.test) ? parsed.value.then : parsed.value.else};
	},
	"system.repeat": function(params, parsed, data) {
		const expansion = {};
		if (parsed.value.steps) {
			const count = parsed.value.count;
			for (let i = 1; i <= count; i++) {
				expansion[i] = _.cloneDeep(parsed.value.steps);
			}
		}

		return {expansion};
	},
};

module.exports = {
	roboliq: "v1",
	schemas: yaml.load(__dirname+"/../schemas/system.yaml"),
	commandHandlers
};
