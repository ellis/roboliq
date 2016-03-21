import _ from 'lodash';
import commandHelper from '../../commandHelper.js';
import evowareHelper from './evowareHelper.js';

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
