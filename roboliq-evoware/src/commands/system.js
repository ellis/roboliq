/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017, ETH Zurich, Ellis Whitehead
 * @license GPL-3.0
 */

const _ = require('lodash');
const assert = require('assert');
const math = require('mathjs');
const commandHelper = require('roboliq-processor/dist/commandHelper.js');
import * as evowareHelper from './evowareHelper.js';

export function runtimeExitLoop(params, parsed, data) {
	const target = data.loopEndStack[0];
	return [
		{line: evowareHelper.createExecuteLine("%{ROBOLIQ}", ["execTest", "%{SCRIPTFILE}", data.path.join(".")], true, "EXITLOOP")},
		{line: evowareHelper.createIfLine("EXITLOOP", "==", 0, target)}
	];
}

export function runtimeLoadVariables(params, parsed, data) {
	const stepId = data.path.join(".");
	return [
		{line: evowareHelper.createExecuteLine("%{ROBOLIQ}", ["evowareRuntimeLoadVariables", "%{SCRIPTFILE}", stepId], true)},
		{line: evowareHelper.createStartScriptLine(`%{SCRIPTDIR}\\continue.esc`), file: {filename: "continue.esc", data: ""}}
	];
}

export function runtimeSteps(params, parsed, data) {
	const stepId = data.path.join(".");
	return [
		{line: evowareHelper.createExecuteLine("%{ROBOLIQ}", ["runtimeSteps", "%{SCRIPTFILE}", stepId], true)},
		{line: evowareHelper.createWorklistLine(`%{SCRIPTDIR}\\temp.ewl`)}
	];
}
