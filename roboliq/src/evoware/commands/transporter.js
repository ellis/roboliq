import _ from 'lodash';
import commandHelper from '../../commandHelper.js';

export function _movePlate(params, parsed, data) {
	//console.log("_movePlate: "+JSON.stringify(parsed, null, '\t'));
	// romaIndex: "(@equipment).evowareRoma: integer"
	const values = commandHelper.lookupPaths({
		romaIndex: ["@equipment", "evowareRoma"],
		plateModelName: [["@object", "model"], "evowareName"],
		plateOrigName: ["@object", "location"],
		plateOrigCarrierName: [["@object", "location"], "evowareCarrier"],
		plateOrigGrid: [["@object", "location"], "evowareGrid"],
		plateOrigSite: [["@object", "location"], "evowareSite"],
		plateDestCarrierName: ["@destination", "evowareCarrier"],
		plateDestGrid: ["@destination", "evowareGrid"],
		plateDestSite: ["@destination", "evowareSite"],
	}, params, data);
	values.programName = parsed.value.program;
	const bMoveBackToHome = parsed.value.evowareMoveBackToHome || false; // 1 = move back to home position
	values.moveBackToHome = (bMoveBackToHome) ? 1 : 0;
	//console.log(JSON.stringify(values, null, '\t'))
	const l = [
		`"${values.plateOrigGrid}"`,
		`"${values.plateDestGrid}"`,
		values.moveBackToHome,
		0, //if (lidHandling == NoLid) 0 else 1,
		0, // speed: 0 = maximum, 1 = taught in vector dialog
		values.romaIndex,
		0, //if (lidHandling == RemoveAtSource) 1 else 0,
		'""', //'"'+(if (lidHandling == NoLid) "" else iGridLid.toString)+'"',
		`"${values.plateModelName}"`,
		`"${values.programName}"`,
		'""',
		'""',
		`"${values.plateOrigCarrierName}"`,
		'""', //'"'+sCarrierLid+'"',
		`"${values.plateDestCarrierName}"`,
		`"${values.plateOrigSite}"`,
		`"(Not defined)"`, // '"'+(if (lidHandling == NoLid) "(Not defined)" else iSiteLid.toString)+'"',
		`"${values.plateDestSite}"`
	];
	const line = `Transfer_Rack(${l.join(",")});`;
	//println(s"line: $line")
	//val let = JsonUtils.makeSimpleObject(x.`object`+".location", JsString(plateDestName))

	const plateName = parsed.objectName.object;
	const plateDestName = parsed.objectName.destination;
	return [{
		line,
		effects: _.fromPairs([[`${plateName}.location`, plateDestName]]),
		tableEffects: [
			[[values.plateOrigCarrierName, values.plateOrigGrid, values.plateOrigSite], {label: _.last(values.plateOrigName.split('.')), labwareModelName: values.plateModelName}],
			[[values.plateDestCarrierName, values.plateDestGrid, values.plateDestSite], {label: _.last(plateDestName.split('.')), labwareModelName: values.plateModelName}],
		]
	}];
}
