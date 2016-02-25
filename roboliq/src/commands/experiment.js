import _ from 'lodash';
import assert from 'assert';
import yaml from 'yamljs';
import commandHelper from '../commandHelper.js';
import * as Design from '../design.js';
import expect from '../expect.js';
import misc from '../misc.js';

const commandHandlers = {
	"experiment.run": function(params, parsed, data) {
		//console.log("experiment.run");
		//console.log(JSON.stringify(parsed, null, '\t'));

		if (_.isEmpty(parsed.value.steps)) {
			return {};
		}

		const DATA = (parsed.value.design)
		  ? Design.flattenDesign(parsed.value.design)
			: data.objects.DATA;
		assert(DATA, "missing required parameter 'design'");

		const DATAs = Design.query(DATA, {groupBy: parsed.value.groupBy});

		// Check how many timers are needed
		const needTimer1 = !_.isUndefined(parsed.value.duration);
		const needTimer2 = !_.isUndefined(parsed.value.interleave);
		const needTimers = (needTimer1 ? 1 : 0) + (needTimer2 ? 1 : 0);

		// Select the timers
		let timers = parsed.value.timers || [];
		assert(timers.length >= needTimers, `please supply ${needTimers} timers`);
		const timer1 = (needTimer1) ? timers.shift() : undefined;
		const timer2 = (needTimer2) ? timers.shift() : undefined;

		const expansion = {};

		// Start timer at beginning?
		const postponeTimer1 = (_.isString(parsed.value.startTimerAfterStep) && !_.isEmpty(parsed.value.startTimerAfterStep));
		const timer1Step = _.merge({}, {
			command: "timer.start",
			agent: parsed.objectName.agent,
			equipment: timer1
		});
		if (needTimer1 && !postponeTimer1) {
			expansion["0"] = timer1Step;
		}

		for (let groupIndex = 0; groupIndex < DATAs.length; groupIndex++) {
			const DATA = DATAs[groupIndex];

			// Step starts out as a copy of the given steps
			const step = _.cloneDeep(parsed.value.steps);
			// Set the current DATA group
			step["@DATA"] = _.cloneDeep(DATA);
			// Add postponed timer1, if necessary
			if (postponeTimer1 && groupIndex === 0) {
				step[parsed.value.startTimerAfterStep+"+"] = timer1Step;
			}

			const groupKey = (groupIndex + 1).toString();
			// If interleaving, run the step within timer2
			if (needTimer2) {
				const timedStep = _.merge({}, {
					command: "timer.doAndWait",
					agent: parsed.objectName.agent,
					equipment: timer2,
					duration: parsed.value.interleave.format()
				});
				timedStep.steps = step;
				expansion[groupKey] = timedStep;
			}
			else {
				expansion[groupKey] = step;
			}
		}

		// Wait till timer1 has elapsed
		if (needTimer1) {
			expansion[expansion.length+1] = _.merge({}, {
				command: "timer.wait",
				agent: parsed.objectName.agent,
				equipment: timer1,
				till: parsed.value.duration.format(),
				stop: true
			});
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
