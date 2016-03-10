import _ from 'lodash';
import commandHelper from '../../commandHelper.js';

function getMoveRomaHomeLine(romaIndex) {
	return getRomaMoveLine(romaIndex, 2);
}

/**
 * Move a ROMA
 * @param  {number} romaIndex - index of roma
 * @param  {number} action - 0=open gripper, 1=close gripper, 2=move home, 3=move relative,
 * @return {[type]}           [description]
 */
function getRomaMoveLine(romaIndex, action) {
	const x = {
		action,
		gripperDistance: 80,
		dx: 0,
		dy: 0,
		dz: 0,
		speed: 150,
		maximumSpeed: 1,
		romaIndex
	};
	return `ROMA(${x.action},${x.gripperDistance},${x.force},${x.dx},${x.dy},${x.dz},${x.speed},${x.maximumSpeed},${x.romaIndex});`;
}

export function _movePlate(params, parsed, data) {
	//console.log("_movePlate: "+JSON.stringify(parsed, null, '\t'));
	// romaIndex: "(@equipment).evowareRoma: integer"
	const values = commandHelper.lookupPaths({
		romaIndex: ["@equipment", "evowareRoma"],
		plateModelName: [["@object", "model"], "evowareName"],
		plateOrigName: ["@object", "location"],
		plateOrigCarrierName: [["@object", "location"], "evowareCarrier"],
		plateOrig: [["@object", "location"]],
		plateOrigGrid: [["@object", "location"], "evowareGrid"],
		plateOrigSite: [["@object", "location"], "evowareSite"],
		plateDest: [["@destination"]],
		plateDestCarrierName: ["@destination", "evowareCarrier"],
		plateDestGrid: ["@destination", "evowareGrid"],
		plateDestSite: ["@destination", "evowareSite"],
	}, params, data);

	const plateDestName = parsed.objectName.destination;

	// It may be that multiple sites are defined which are actually the same physical location.
	// We can supress transporter commands between the logical sites by checking whether the sames have the same siteIdUnique.
	// console.log({plateOrig: values.plateOrig, plateDest: values.plateDest})
	if (values.plateOrig.siteIdUnique && values.plateOrig.siteIdUnique === values.plateDest.siteIdUnique) {
		return [{
			tableEffects: [
				[[values.plateOrigCarrierName, values.plateOrigGrid, values.plateOrigSite], {label: _.last(values.plateOrigName.split('.')), labwareModelName: values.plateModelName}],
				[[values.plateDestCarrierName, values.plateDestGrid, values.plateDestSite], {label: _.last(plateDestName.split('.')), labwareModelName: values.plateModelName}],
			]
		}];
	}

	const romaIndexPrev = _.get(data.objects, ["EVOWARE", "romaIndexPrev"], values.romaIndex);
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

	const items = [];

	if (values.romaIndex !== romaIndexPrev) {
		items.push({
			line: getMoveRomaHomeLine(romaIndexPrev)
		});
	}

	items.push({
		line,
		effects: _.fromPairs([[`${plateName}.location`, plateDestName], [`EVOWARE.romaIndexPrev`, values.romaIndex]]),
		tableEffects: [
			[[values.plateOrigCarrierName, values.plateOrigGrid, values.plateOrigSite], {label: _.last(values.plateOrigName.split('.')), labwareModelName: values.plateModelName}],
			[[values.plateDestCarrierName, values.plateDestGrid, values.plateDestSite], {label: _.last(plateDestName.split('.')), labwareModelName: values.plateModelName}],
		]
	});

	return items;
}

export function moveLastRomaHome(data) {
	const romaIndexPrev = _.get(data.objects, ["EVOWARE", "romaIndexPrev"]);
	if (romaIndexPrev) {
		return [{
			line: getMoveRomaHomeLine(romaIndexPrev)
		}];
	}
	else {
		return [];
	}
}
