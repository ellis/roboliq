/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017, ETH Zurich, Ellis Whitehead
 * @license GPL-3.0
 */

/**
 * Module for a 1-site Tecan shaker.
 * @module
 */
const _ = require('lodash');
const assert = require('assert');
const math = require('mathjs');
const commandHelper = require('roboliq-processor/dist/commandHelper.js');
const expect = require('roboliq-processor/dist/expect.js');

/**
 * @typedef ShakerTecan1Params
 * @type {object}
 * @property {!string} evowareId - the Evoware ID of this equipment
 * @property {?string} evowareGrid - the grid that the equipment is on
 * @param {!string} site - the equipment's site name (just the base part, without namespace)
 * ```
 * evowareId: "HPShaker",
 * site: "P3"
 * ```
 */
/**
 * Configure the Tecan 1-site shaker
 * @param {ShakerTecan1Params} params - parameters for the configuration
 * @return {EquipmentConfig}
 */
function configure(config, equipmentName, params) {
	const agent = config.getAgentName();
	const equipment = config.getEquipmentName(equipmentName);
	const siteName = config.getSiteName(params.site);

	const objects = {};
	_.set(objects, equipment, {
		type: "Shaker",
		evowareId: params.evowareId
	});

	const predicates = [
		{
			"shaker.canAgentEquipmentSite": {
				agent,
				equipment,
				site: siteName
			}
		},
	];
	// console.log("shaker.predicates: "+JSON.stringify(predicates, null, '\t'));

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
				},
				object: {description: "Plate identifier (optional)", type: "Plate"}
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

	const protocol = {
		objects,
		predicates,
		schemas,
		commandHandlers
	};
	return protocol;
}

module.exports = {
	configure
};
