import _ from 'lodash';
import assert from 'assert';
import yaml from 'yamljs';
import commandHelper from '../commandHelper.js';
import * as Design from '../design.js';
import expect from '../expect.js';
import misc from '../misc.js';

function run(params, parsed, data) {
	//console.log("experiment.run");
	//console.log("parsed: "+JSON.stringify(parsed, null, '\t'));

	if (_.isEmpty(parsed.value.steps)) {
		return {};
	}

	const DATA = (parsed.value.design)
	  ? Design.flattenDesign(parsed.value.design)
		: data.objects.DATA;
	assert(DATA, "missing required parameter 'design'");

	const DATAs = Design.query(DATA, {groupBy: parsed.value.groupBy});
	//console.log("experiment.run DATAs: "+JSON.stringify(DATAs, null, '\t'));

	// Check how many timers are needed
	const needTimer1 = !_.isUndefined(parsed.value.duration);
	const needTimer2 = !_.isUndefined(parsed.value.interleave);
	const needTimers = (needTimer1 ? 1 : 0) + (needTimer2 ? 1 : 0);

	// Select the timers
	let timers = [];
	if (!_.isEmpty(parsed.value.timers)) {
		timers = _.range(parsed.value.timers.length).map(i => parsed.objectName[`timers.${i}`]);
		//console.log({timers})
	}
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

		// Update the SCOPE
		const common = Design.getCommonValues(DATA);
		//console.log("DATA1b: "+JSON.stringify(DATA, null, '\t'));
		//const SCOPE = _.merge({}, data.objects.SCOPE, common);
		const data2 = _.merge({}, data, {objects: {SCOPE: common}});
		data2.objects.DATA = DATA;

		// Step starts out as a copy of the given steps
		const step0 = commandHelper.stepify(_.cloneDeep(parsed.value.steps));
		const step1 = (_.size(step0) === 1) ? _(step0).toPairs().head()[1] : step0;
		const step = makeSubstitutions(step1, data2);
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
		expansion[DATAs.length+1] = _.merge({}, {
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

function makeSubstitutions(x, data) {
	return _.mapValues(x, (value, name) => {
		if (_.startsWith(value, "$")) {
			const result = commandHelper._dereferenceVariable(data, value);
			//console.log({name, value, SCOPE: data.objects.SCOPE, DATA: data.objects.DATA, result})
			return (result) ? result.value : undefined;
		}
		else if (_.startsWith(value, "`") && _.endsWith(value, "`")) {
			const value1 = value.substr(1, value.length - 2);
			const scope = _.mapKeys(data.objects.SCOPE, (value, name) => "$"+name);
			//console.log({value, value1, scope})
			return misc.renderTemplate(value1, scope, data);
		}
		else {
			return value;
		}
	});
}

const commandHandlers = {
	"experiment.forEachGroup": function(params, parsed, data) {
		return run(params, parsed, data);
	},
	"experiment.run": run,
};

module.exports = {
	roboliq: "v1",
	schemas: yaml.load(__dirname+"/../schemas/experiment.yaml"),
	commandHandlers
};
