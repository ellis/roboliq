const _ = require('lodash');
const assert = require('assert');
const math = require('mathjs');
const commandHelper = require('roboliq-processor/dist/commandHelper.js');
const expect = require('roboliq-processor/dist/expect.js');

/**
 * Configure the Tecan 1-site shaker
 * @param {ShakerTecan1Params} params - parameters for the configuration
 * @return {EquipmentConfig}
 */
function configure(config, equipmentName, params) {
	const agent = config.getAgentName();
	const equipment = config.getEquipmentName(equipmentName);
	const siteName = config.getSiteName(params.site);

	const predicates = [
		{
			"shaker.canAgentEquipmentSite": {
				agent,
				equipment,
				site: siteName
			}
		},
	];

	const schemas = {
		[`shaker.run|${agent}|${equipment}`]: {
			properties: {
				agent: {description: "Agent identifier", type: "Agent"},
				equipment: {description: "Equipment identifier", type: "Equipment"},
				program: {
					description: "Program for shaking",
					properties: {
						rpm: {description: "Rotations per minute (RPM)", type: "number"},
						duration: {description: "Duration of shaking", type: "Duration"}
					},
					required: ["duration"]
				}
			},
			required: ["agent", "equipment", "program"]
		}
	};

	const commandHandlers = {
		[`shaker.run|${agent}|${equipment}`]: function(params, parsed, data) {
			//console.log("equipment.run|ourlab.mario.evoware|ourlab.mario.shaker: "+JSON.stringify(parsed, null, '\t'))
			const equipmentId = commandHelper.getParsedValue(parsed, data, "equipment", "evowareId");
			const rpm = parsed.value.program.rpm || 750;

			// Construct the shaker program data
			const shakerNo = 1;
			// FIXME: Let the user specify mode1, steps1, mode2, steps2, power
			const mode1 = 2;
			const steps1 = 0;
			const mode2 = 1;
			const steps2 = 0;
			const freq = (60000000/rpm).toFixed(0);
			const cycles = Math.floor(rpm * parsed.value.program.duration.toNumber("minute")).toFixed(0);
			const powerPerc = 50;
			const power = Math.floor(255 * powerPerc / 100).toFixed(0);
			const s0 = `*27${shakerNo}|${freq}|${mode1}|${steps1}|${mode2}|${steps2}|${cycles}|${power}*27`;
			// Replace all occurences of '0' with "*30"
			const s1 = s0.replace(/0/g, "*30");
			// Finally, split string into 32 char parts, then rebind them, separated by commas
			const s2 = _(s1).chunk(32).map(s => s.join("")).join(",");

			return {
				expansion: [
					{
						command: "evoware._facts",
						agent: parsed.objectName.agent,
						factsEquipment: equipmentId,
						factsVariable: equipmentId+"_HP__Start",
						factsValue: s2
					},
					{
						command: "timer.sleep",
						agent: parsed.objectName.agent,
						duration: parsed.orig.program.duration
					},
					{
						command: "evoware._facts",
						agent: parsed.objectName.agent,
						factsEquipment: equipmentId,
						factsVariable: equipmentId+"_HP__Stop",
						factsValue: "1"
					},
				]
			};
		},
	};
}






/**
 * @typedef CentrifugeParams
 * @type {object}
 * @property {!string} evowareId - the Evoware ID of this equipment
 * @property {!string} evowareGrid - the grid that the equipment is on
 * @param {!Object} sites - keys are the site names (just the base part, without namespace), values are objects with the property `evowareSite`.
 * @param {!CentrifugeSitesToModels[]} siteModelCompatibilities - an array of objects {sites, models} of which sites
 * @example
 * ```
 *		{
 *			evowareId: "Centrifuge",
 *			evowareGrid: 54,
 *			sites: {
 *				CENTRIFUGE_1: { evowareSite: 1 },
 *				CENTRIFUGE_2: { evowareSite: 2 },
 *				CENTRIFUGE_3: { evowareSite: 1 },
 *				CENTRIFUGE_4: { evowareSite: 2 }
 *			},
 *			siteModelCompatibilities: [
 *				{
 *					sites: ["CENTRIFUGE_1", "CENTRIFUGE_2", "CENTRIFUGE_3", "CENTRIFUGE_4"],
 *					models: ["plateModel_384_square", "plateModel_96_round_transparent_nunc"]
 *				},
 *				{
 *					sites: ["CENTRIFUGE_2", "CENTRIFUGE_4"],
 *					models: ["plateModel_96_dwp"]
 *				},
 *			]
 *		}
 */

/**
 * @typedef CentrifugeSitesToModels
 * @type {object}
 * @property {!string[]} sites - array of site names (just the base part, without namespace)
 * @property {!string[]} models - array of model names (just the base part, without namespace)
 */

/**
 * Configure a generic centrifuge
 * @param {CentrifugeParams} params - parameters for the configuration
 * @return {EquipmentConfig}
 */
function configure(config, equipmentName, params) {
	const N = params.sitesInternal.length;
	assert(N == 4, "centrifuge-X has only been designed for 4-site centrifuges.  Please contact the developer if you need a different number of sites.")

	const sites = _.mapValues(params.sites, value => ({
		evowareCarrier: params.evowareId,
		evowareGrid: params.evowareGrid,
		evowareSite: value.evowareSite,
		close: true
	}));

	// Map from site to all its compatible models
	const siteToModels = _(params.siteModelCompatibilities)
		.map(x => x.sites.map(site => ({site, models: x.models})))
		.flatten()
		.groupBy(x => x.site)
		.mapValues(x => _.flatten(_.map(x, "models")))
		.value();

	const siteModelCompatibilities = params.siteModelCompatibilities;

	const agent = config.getAgentName();
	const equipment = config.getEquipmentName(equipmentName);
	const siteNames = Object.keys(sites).map(s => config.getSiteName(s));

	// For the 1st and 3rd sites
	const predicates13 = _(sitesToModels[siteNames[0]]).map(model => {
		return {"centrifuge.canAgentEquipmentModelSite1Site2": {
			agent, equipment,
			model,
			site1: siteNames[0],
			site2: siteNames[2],
		}}
	}).flatten().value();
	// For the 2nd and 4th sites
	const predicates24 = _(sitesToModels[siteNames[1]]).map(model => {
		return {"centrifuge.canAgentEquipmentModelSite1Site2": {
			agent, equipment,
			model,
			site1: siteNames[1],
			site2: siteNames[3],
		}}
	}).flatten().value();

	const predicateTasks = [
		{"action": {"description": equipment+".close: close the centrifuge",
			"task": {[equipment+".close"]: {"agent": "?agent", "equipment": "?equipment"}},
			"preconditions": [],
			"deletions": [],
			"additions": siteNames.map(site => ({siteIsClosed: {site}}))
		}},
		_.map(siteNames, (site, i) => {
			return {"method": {"description": "generic.openSite-"+site,
				"task": {"generic.openSite": {"site": "?site"}},
				"preconditions": [{"same": {"thing1": "?site", "thing2": site}}],
				"subtasks": {"ordered": [{[equipment+".open"+(i+1)]: {}}]}
			}};
		}),
		_.map(siteNames, (site, i) => {
			return {"action": {"description": equipment+".open"+(i+1)+": open an internal site on the centrifuge",
				"task": {[equipment+".open"+(i+1)]: {}},
				"preconditions": [],
				"deletions": [
					{"siteIsClosed": {"site": site}}
				],
				"additions": _.map(_.without(siteNames, site), function(site2) {
					return {"siteIsClosed": {"site": site2}};
				})
			}};
		}),
		_.map(siteNames, (site, i) => ({method: {
			"description": "generic.closeSite-"+site,
			"task": {"generic.closeSite": {"site": "?site"}},
			"preconditions": [
				{"same": {"thing1": "?site", "thing2": site}}
			],
			"subtasks": {"ordered": [
				{[equipment+".close"]: {"agent": "ourlab.mario.evoware", "equipment": "ourlab.mario.centrifuge"}}
			]}
		}})),
	];

	const planHandlers = _.fromPairs(_.flatten({
		[
			equipment+".close",
			function(params, parentParams, data) {
				return [{
					command: "equipment.close",
					agent,
					equipment
				}];
			}
		],
		_.map(siteNames, (site, i) => [
			equipment+".open"+(i+1),
			function(params, parentParams, data) {
				return [{
					command: "equipment.openSite",
					agent,
					equipment,
					site
				}];
			}
		])
	};

	const schemas = {
		[`equipment.close|${agent}|${equipment}`]: {
			properties: {
				agent: {description: "Agent identifier", type: "Agent"},
				equipment: {description: "Equipment identifier", type: "Equipment"},
			},
			required: ["agent", "equipment"]
		},
		[`equipment.open|${agent}|${equipment}`]: {
			properties: {
				agent: {description: "Agent identifier", type: "Agent"},
				equipment: {description: "Equipment identifier", type: "Equipment"},
			},
			required: ["agent", "equipment"]
		},
		[`equipment.openSite|${agent}|${equipment}`]: {
			properties: {
				agent: {description: "Agent identifier", type: "Agent"},
				equipment: {description: "Equipment identifier", type: "Equipment"},
				site: {description: "Site identifier", type: "Site"}
			},
			required: ["agent", "equipment", "site"]
		},
		[`equipment.run|${agent}|${equipment}`]: {
			properties: {
				agent: {description: "Agent identifier", type: "Agent"},
				equipment: {description: "Equipment identifier", type: "Equipment"},
				program: {
					description: "Program for centrifuging",
					type: "object",
					properties: {
						rpm: {type: "number", default: 3000},
						duration: {type: "Duration", default: "30 s"},
						spinUpTime: {type: "Duration", default: "9 s"},
						spinDownTime: {type: "Duration", default: "9 s"},
						temperature: {type: "Temperature", default: "25 degC"}
					}
				}
			},
			required: ["program"]
		},
	};

	const commandHandlers = {
		[`equipment.close|${agent}|${equipment}`]: function(params, parsed, data) {
			return {expansion: [makeEvowareFacts(parsed, data, "Close")]};
		},
		[`equipment.open|${agent}|${equipment}`]: function(params, parsed, data) {
			return {expansion: [makeEvowareFacts(parsed, data, "Open")]};
		},
		[`equipment.openSite|${agent}|${equipment}`]: function(params, parsed, data) {
			var carrier = commandHelper.getParsedValue(parsed, data, "equipment", "evowareId");
			var sitesInternal = commandHelper.getParsedValue(parsed, data, "equipment", "sitesInternal");
			var siteIndex = sitesInternal.indexOf(parsed.objectName.site);
			expect.truthy({paramName: "site"}, siteIndex >= 0, "site must be one of the equipments internal sites: "+sitesInternal.join(", "));
			return {
				expansion: [
					{
						command: "evoware._facts",
						agent: parsed.objectName.agent,
						factsEquipment: carrier,
						factsVariable: carrier+"_MoveToPos",
						factsValue: (siteIndex+1).toString()
					},
					{
						command: "evoware._facts",
						agent: parsed.objectName.agent,
						factsEquipment: carrier,
						factsVariable: carrier+"_Open"
					},
				]
			};
		},
		[`equipment.run|${agent}|${equipment}`]: function(params, parsed, data) {
			//console.log("equipment.run|ourlab.mario.evoware|ourlab.mario.centrifuge:")
			//console.log({parsed, params})
			const parsedProgram = parsed.value.program;
			//console.log({parsedProgram});
			var list = [
				math.round(parsedProgram.rpm),
				math.round(parsedProgram.duration.toNumber('s')),
				math.round(parsedProgram.spinUpTime.toNumber('s')),
				math.round(parsedProgram.spinDownTime.toNumber('s')),
				math.round(parsedProgram.temperature.toNumber('degC'))
			];
			var value = list.join(",");
			return {expansion: [makeEvowareFacts(parsed, data, "Execute1", value)]};
		},
	};
}

module.exports = {
	configure
};
