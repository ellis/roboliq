import _ from 'lodash';

export function _movePlate(step, objects, protocol, path) {
	// romaIndex: "(@equipment).evowareRoma: integer"
	const romaIndex = _.get(_.get(objects, step.equipment), "evowareRoma");
	const programName = step.program;
	const object = _.get(objects, step.object);
	const plateModelName0 = _.get(object, "model");
	// plateModelName: "((@object).model).evowareName: string"
	// plateModelName: [["@object", "model"], "evowareName"]
	const plateModelName = _.get(_.get(objects, plateModelName0), "evowareName");
	const plateOrigName = _.get(object, "location");
	const plateOrig = _.get(objects, plateOrigName);
	const plateOrigCarrierName = _.get(plateOrig, "evowareCarrier");
	const plateOrigGrid = _.get(plateOrig, "evowareGrid");
	const plateOrigSite = _.get(plateOrig, "evowareSite");
	const plateDestName = step.destination;
	const plateDest = _.get(objects, plateDestName);
	const plateDestCarrierName = _.get(plateDest, "evowareCarrier");
	const plateDestGrid = _.get(plateDest, "evowareGrid");
	const plateDestSite = _.get(plateDest, "evowareSite");

	const bMoveBackToHome = step.evowareMoveBackToHome || false; // 1 = move back to home position
	const l = [
		`"${plateOrigGrid}"`,
		`"${plateDestGrid}"`,
		(bMoveBackToHome) ? 1 : 0,
		0, //if (lidHandling == NoLid) 0 else 1,
		0, // speed: 0 = maximum, 1 = taught in vector dialog
		romaIndex,
		0, //if (lidHandling == RemoveAtSource) 1 else 0,
		'""', //'"'+(if (lidHandling == NoLid) "" else iGridLid.toString)+'"',
		`"${plateModelName}"`,
		`"${programName}"`,
		'""',
		'""',
		`"${plateOrigCarrierName}"`,
		'""', //'"'+sCarrierLid+'"',
		`"${plateDestCarrierName}"`,
		`"${plateOrigSite-1}"`,
		"(Not defined)", // '"'+(if (lidHandling == NoLid) "(Not defined)" else iSiteLid.toString)+'"',
		`"${plateDestSite-1}"`
	];
	const line = `Transfer_Rack(${l.join(",")});`;
	//println(s"line: $line")
	//val let = JsonUtils.makeSimpleObject(x.`object`+".location", JsString(plateDestName))
	return [{
		line,
		effects: _.fromPairs([[`${object}.location`, plateDestName]]),
		tableEffects: [
			[[plateOrigCarrierName, plateOrigGrid, plateOrigSite], {label: _.last(plateOrigName.split('.')), labwareModelName: plateModelName}],
			[[plateDestCarrierName, plateDestGrid, plateDestSite], {label: _.last(plateDestName.split('.')), labwareModelName: plateModelName}],
		]
	}];
}
