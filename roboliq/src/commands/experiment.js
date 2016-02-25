import _ from 'lodash';
import yaml from 'yamljs';
import commandHelper from '../commandHelper.js';
import expect from '../expect.js';
import misc from '../misc.js';

interleave: {description: "The time offset for interleaving each group", type: Duration}
duration: {description: "The total duration of this step (all groups should execute within the allotted time)", type: Duration}
timers: {description: "Timers that should be used", type: array, items: {type: Timer}}
startTimerAfterStep: {description: "The duration timer can be started after a given step rather than from the beginning", type: integer}

const commandHandlers = {
	"experiment.run": function(params, parsed, data) {
		//console.log("experiment.run");
		//console.log(JSON.stringify(parsed, null, '\t'));

		if (_.isEmpty(parsed.value.steps)) {
			return {};
		}

		const DATA = (params.value.design)
		  ? Design.flattenDesign(params.value.design)
			: data.objects.DATA;
		assert(DATA, "missing required parameter 'design'");

		const DATAs = Design.query(DATA, {groupBy: params.value.groupBy});

		// Check how many timers are needed
		const needTimer1 = !_.isUndefined(params.value.duration);
		const needTimer2 = !_.isUndefined(params.value.interleave);
		const needTimers = (needTimer1 ? 1 : 0) + (needTimer2 ? 1 : 0);

		// Select the timers
		let timers = params.value.timers || [];
		assert(timers.length >= needTimers, `please supply ${needTimers} timers`);
		const timer1 = (needTimer1) ? timers.shift();
		const timer2 = (needTimer2) ? timers.shift();

		const expansion = {};

		// Start timer at beginning?
		if (params.value.duration && _.isEmpty(params.value.startTimerAfterStep)) {
			expansion[0] = _.merge({}, {
				command: "timer.start",
				agent: parsed.objectName.agent,
				equipment: timer1
			};
		}

		for (let groupIndex = 0; groupIndex < DATAs.length; groupIndex++) {
			const DATA = DATAs[groupIndex];
			expansion[i+1] = _.cloneDeep(parsed.value.steps);
			expansion[i+1]."@DATA" = _.cloneDeep(DATA);
			CONTINUE with startTimerAfterStep
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
