/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017, ETH Zurich, Ellis Whitehead
 * @license GPL-3.0
 */

const _ = require('lodash');
const assert = require('assert');
const yaml = require('yamljs');
const commandHelper = require('../commandHelper.js');
const Design = require('../design.js');
const expect = require('../expect.js');
const misc = require('../misc.js');

function run(parsed, data) {
	// console.log("data.run");
	//console.log("parsed: "+JSON.stringify(parsed, null, '\t'));

	if (_.isEmpty(parsed.value.steps)) {
		return {};
	}

	const DATA = (parsed.value.design)
		? Design.flattenDesign(parsed.value.design)
		: data.objects.DATA;
	// console.log("data.run DATA: "+JSON.stringify(DATA, null, '\t'));
	// console.log("data.run DATA: ");
	// _.forEach(DATA, row => { console.log(JSON.stringify(row)) });
	assert(DATA, "missing DATA; you may want to specify the parameter 'design'");

	const queryResult = Design.query(DATA, {groupBy: parsed.value.groupBy, distinctBy: parsed.value.distinctBy, orderBy: parsed.value.orderBy, where: parsed.value.where});
	const DATAs = (parsed.value.forEachRow)
		? _.flatten(queryResult).map(x => [x])
		: queryResult;
	//console.log("data.run DATAs: "+JSON.stringify(DATAs, null, '\t'));

	// Check how many timers are needed
	const interleave = (parsed.value.forEachRow) ? parsed.value.durationRow : parsed.value.durationGroup;
	const needTimer1 = !_.isUndefined(parsed.value.durationTotal);
	const needTimer2 = !_.isUndefined(interleave);
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
		// console.log(`DATAs[${groupIndex}]:`);
		//_.forEach(DATA, row => { console.log(JSON.stringify(row)) });

		// Update the SCOPE
		const common = Design.getCommonValues(DATA);
		const SCOPE = _.merge({}, data.objects.SCOPE, common);

		// Step starts out as a copy of the given steps
		const step0 = commandHelper.stepify(_.cloneDeep(parsed.value.steps));
		const step1 = (_.size(step0) === 1) ? _(step0).toPairs().head()[1] : step0;
		const step = commandHelper.substituteDeep(step1, data, SCOPE, DATA);
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
				duration: interleave.format()
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
			till: parsed.value.durationTotal.format(),
			stop: true
		});
	}

	//console.log(JSON.stringify(expansion, null, "\t"))
	return {expansion};
}

const commandHandlers = {
	"data.forEachGroup": function(params, parsed, data) {
		return run(parsed, data);
	},
	"data.forEachRow": function(params, parsed, data) {
		parsed.value.forEachRow = true;
		return run(parsed, data);
	},
};

module.exports = {
	roboliq: "v1",
	schemas: yaml.load(__dirname+"/../schemas/data.yaml"),
	commandHandlers
};
