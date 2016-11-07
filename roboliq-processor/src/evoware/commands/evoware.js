/**
 * Handles instructions which are specifically for controlling evoware.
 * @module
 */

import _ from 'lodash';
import commandHelper from '../../commandHelper.js';
import * as evowareHelper from './evowareHelper.js';

export function _execute(params, parsed, data) {
	const line = evowareHelper.createExecuteLine(parsed.orig.path, parsed.orig.args, parsed.orig.wait);
	return [{line}];
}

export function _facts(params, parsed, data) {
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

export function _raw(params, parsed, data) {
	return _(parsed.value.commands).split(";").map(_.trim).compact().map(s => ({line: s+";"})).value();
}

export function _userPrompt(params, parsed, data) {
	const line = evowareHelper.createUserPromptLine(parsed.orig.text, parsed.orig.beep, parsed.orig.autoclose);
	return [{line}];
}

export function _variable(params, parsed, data) {
	const line = evowareHelper.createVariableLine(parsed.orig.name, parsed.orig.value);
	return [{line}];
}
