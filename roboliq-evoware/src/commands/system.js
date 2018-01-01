/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017, ETH Zurich, Ellis Whitehead
 * @license GPL-3.0
 */

const _ = require('lodash');
const assert = require('assert');
const math = require('mathjs');
const commandHelper = require('roboliq-processor/src/commandHelper.js');
const evowareHelper = require('./evowareHelper.js');

function runtimeExitLoop(params, parsed, data) {
	const target = data.loopEndStack[0];
	return [
		{line: evowareHelper.createExecuteLine("%{ROBOLIQ}", ["execTest", "%{SCRIPTFILE}", data.path.join(".")], true, "EXITLOOP")},
		{line: evowareHelper.createIfLine("EXITLOOP", "==", 0, target)}
	];
}

function runtimeLoadVariables(params, parsed, data) {
	const stepId = data.path.join(".");
	return [
		{line: evowareHelper.createExecuteLine("%{ROBOLIQ}", ["evowareRuntimeLoadVariables", "%{SCRIPTFILE}", stepId], true)},
		{line: evowareHelper.createStartScriptLine(`%{SCRIPTDIR}\\continue.esc`), file: {filename: "continue.esc", data: ""}}
	];
}

function runtimeSteps(params, parsed, data) {
	const stepId = data.path.join(".");
	return [
		{line: evowareHelper.createExecuteLine("%{ROBOLIQ}", ["runtimeSteps", "%{SCRIPTFILE}", stepId], true)},
		{line: evowareHelper.createWorklistLine(`%{SCRIPTDIR}\\temp.ewl`)}
	];
}

module.exports = {
  runtimeExitLoop,
  runtimeLoadVariables,
  runtimeSteps,
};
