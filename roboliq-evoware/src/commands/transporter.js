/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017, ETH Zurich, Ellis Whitehead
 * @license GPL-3.0
 */

/**
 * Handles transporter instructions and supplies some additional control functions
 * for moving the ROMAs.
 * @module
 */

const _ = require('lodash');
const commandHelper = require('roboliq-processor/dist/commandHelper.js');

function getMoveRomaHomeLine(romaIndex) {
	return getRomaMoveLine(romaIndex, 2);
}

/**
 * Move a ROMA
 * @param  {number} romaIndex - index of roma
 * @param  {number} action - 0=open gripper, 1=close gripper, 2=move home, 3=move relative,
 * @return {string} string representation of ROMA command
 */
function getRomaMoveLine(romaIndex, action) {
	const x = {
		action,
		gripperDistance: 80,
		force: 0,
		dx: 0,
		dy: 0,
		dz: 0,
		speed: 150,
		maximumSpeed: 1,
		romaIndex
	};
	return `ROMA(${x.action},${x.gripperDistance},${x.force},${x.dx},${x.dy},${x.dz},${x.speed},${x.maximumSpeed},${x.romaIndex});`;
}

/**
 * Handle the `transporter._moveLidFromContainerToSite` instruction.
 *
 * @param  {object} params - original paramters
 * @param  {object} parsed - parsed parameters
 * @param  {object} data - protocol data
 * @return {array} an array of objects that describe output, effects, and table effects
 */
export function _moveLidFromContainerToSite(params, parsed, data) {
	// console.log("_moveLidFromContainerToSite: "+JSON.stringify(parsed, null, '\t'));
	const params2 = {
		agent: params.agent,
		equipment: params.equipment,
		program: params.program,
		object: params.container,
		destination: parsed.value.container.location
	};
	// console.log("params2: "+JSON.stringify(params2, null, '\t'));
	const parsed2 = commandHelper.parseParams(params2, data, data.protocol.schemas["transporter._movePlate"]);
	const lidHandling = {
		lid: parsed.objectName.object,
		action: "remove",
		location: parsed.objectName.destination,
		destination: parsed.objectName.destination
	};
	return _movePlate(params2, parsed2, data, lidHandling);
}

/**
 * Handle the `transporter._moveLidFromSiteToContainer` instruction.
 *
 * @param  {object} params - original paramters
 * @param  {object} parsed - parsed parameters
 * @param  {object} data - protocol data
 * @return {array} an array of objects that describe output, effects, and table effects
 */
export function _moveLidFromSiteToContainer(params, parsed, data) {
	// console.log("_moveLidFromContainerToSite: "+JSON.stringify(parsed, null, '\t'));
	const params2 = {
		agent: params.agent,
		equipment: params.equipment,
		program: params.program,
		object: params.container,
		destination: parsed.value.container.location
	};
	// console.log("params2: "+JSON.stringify(params2, null, '\t'));
	const parsed2 = commandHelper.parseParams(params2, data, data.protocol.schemas["transporter._movePlate"]);
	const lidHandling = {
		lid: parsed.objectName.object,
		action: "cover",
		location: parsed.objectName.origin,
		destination: parsed.objectName.container
	};
	return _movePlate(params2, parsed2, data, lidHandling);
}

/**
 * Handle the `transporter._movePlate` instruction.
 *
 * @param {object} params - original paramters
 * @param {object} parsed - parsed parameters
 * @param {object} data - protocol data
 * @param {object} [lidHandling0] - an optional option to define lid handling - this is only used by the `_moveLidFromContainerToSite` and `_moveLidFromSiteToContainer` handlers.
 * @param {string} [lidHandling0.lid] - name of the lid
 * @param {string} [lidHandling0.action] - should either be "remove" or "cover"
 * @param {string} [lidHandling0.location] - the site where the lid should be moved from or to
 * @param {string} [lidHandling0.destination] - the site where the lid should be after the transfer
 * @return {array} an array of objects that describe output, effects, and table effects
 */
export function _movePlate(params, parsed, data, lidHandling0) {
	// console.log("_movePlate: "+JSON.stringify(parsed, null, '\t'));
	// romaIndex: "(@equipment).evowareRoma: integer"
	const values = commandHelper.lookupPaths({
		romaIndex: ["@equipment", "evowareRoma"],
		programName: ["@program"],
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

	// Lid handling parameters
	const lidHandling = {
		enabled: false,
		grid: "",
		carrierName: "",
		site: "(Not defined)"
	};
	if (!_.isEmpty(lidHandling0)) {
		lidHandling.enabled = true;
		lidHandling.removeAtSource = (lidHandling0.action == "remove");
		lidHandling.grid = commandHelper.lookupPath(["@location", "evowareGrid"], lidHandling0, data)
		lidHandling.carrierName = commandHelper.lookupPath(["@location", "evowareCarrier"], lidHandling0, data)
		lidHandling.site = commandHelper.lookupPath(["@location", "evowareSite"], lidHandling0, data)
	}

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
	const bMoveBackToHome = parsed.value.evowareMoveBackToHome || false; // 1 = move back to home position
	values.moveBackToHome = (bMoveBackToHome) ? 1 : 0;
	//console.log(JSON.stringify(values, null, '\t'))
	const l = [
		`"${values.plateOrigGrid}"`,
		`"${values.plateDestGrid}"`,
		values.moveBackToHome,
		(lidHandling.enabled) ? 1 : 0,
		0, // speed: 0 = maximum, 1 = taught in vector dialog
		values.romaIndex,
		(lidHandling.removeAtSource) ? 1 : 0,
		`"${lidHandling.grid}"`,
		`"${values.plateModelName}"`,
		`"${values.programName}"`,
		'""',
		'""',
		`"${values.plateOrigCarrierName}"`,
		`"${lidHandling.carrierName}"`,
		`"${values.plateDestCarrierName}"`,
		`"${values.plateOrigSite}"`,
		`"${lidHandling.site}"`,
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
		effects:  _.fromPairs(_.compact([
			[`${plateName}.location`, plateDestName],
			(lidHandling.enabled) ? [`${lidHandling0.lid}.location`, lidHandling0.destination] : undefined,
			[`EVOWARE.romaIndexPrev`, values.romaIndex]]
		)),
		tableEffects: [
			[[values.plateOrigCarrierName, values.plateOrigGrid, values.plateOrigSite], {label: _.last(values.plateOrigName.split('.')), labwareModelName: values.plateModelName}],
			[[values.plateDestCarrierName, values.plateDestGrid, values.plateDestSite], {label: _.last(plateDestName.split('.')), labwareModelName: values.plateModelName}],
		]
	});

	return items;
}

/**
 * Move the last-moved ROMA back to its home position.
 *
 * @param  {object} data - protocol data
 * @return {array} an array of objects that describe output, effects, and table effects
 */
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
