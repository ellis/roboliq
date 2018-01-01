/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017, ETH Zurich, Ellis Whitehead
 * @license GPL-3.0
 */

/**
 * Handles instructions which are specifically for controlling evoware.
 * @module
 */

const _ = require('lodash');
const commandHelper = require('roboliq-processor/dist/commandHelper.js');
const evowareHelper = require('./evowareHelper.js');

function _execute(params, parsed, data) {
	const line = evowareHelper.createExecuteLine(parsed.orig.path, parsed.orig.args, parsed.orig.wait);
	return [{line}];
}

function _facts(params, parsed, data) {
	const line = evowareHelper.createFactsLine(parsed.orig.factsEquipment, parsed.orig.factsVariable, parsed.orig.factsValue);

	if (params.labware) {
		const values = commandHelper.lookupPaths({
			plateModelName: [["@labware", "model"], "evowareName"],
			plateOrigName: ["@labware", "location"],
			plateOrigCarrierName: [["@labware", "location"], "evowareCarrier"],
			plateOrigGrid: [["@labware", "location"], "evowareGrid"],
			plateOrigSite: [["@labware", "location"], "evowareSite"]
		}, params, data);

		const tableEffects = [
			[[values.plateOrigCarrierName, values.plateOrigGrid, values.plateOrigSite], {label: _.last(values.plateOrigName.split('.')), labwareModelName: values.plateModelName}],
		];

		return [{line, tableEffects}];
	}
	else {
		return [{line}];
	}
}

function _raw(params, parsed, data) {
	return _(parsed.value.commands).split(";").map(_.trim).compact().map(s => ({line: s+";"})).value();
}

function _subroutine(params, parsed, data) {
	const line = `Subroutine("${params.filename}",0);`;
	return [{line}];
}

function _userPrompt(params, parsed, data) {
	const line = evowareHelper.createUserPromptLine(parsed.orig.text, parsed.orig.beep, parsed.orig.autoclose);
	return [{line}];
}

function _variable(params, parsed, data) {
	const line = evowareHelper.createVariableLine(parsed.orig.name, parsed.orig.value);
	return [{line}];
}

module.exports = {
  _execute,
  _facts,
  _raw,
  _subroutine,
  _userPrompt,
  _variable,
};
