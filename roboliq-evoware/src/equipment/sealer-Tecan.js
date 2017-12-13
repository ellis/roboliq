/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017, ETH Zurich, Ellis Whitehead
 * @license GPL-3.0
 */

import _ from 'lodash';
import commandHelper from 'roboliq-processor/dist/commandHelper.js';

function configure(config, equipmentName, params) {
	const agent = config.getAgentName();
	const equipment = config.getEquipmentName(equipmentName);
	const site = config.getSiteName(params.site);

	const objects = {};
	// Add equipment
	_.set(objects, equipment, {
		type: "Sealer",
		evowareId: params.evowareId,
		// sitesInternal: [site],
		// modelToPlateFile: _.fromPairs(_.map(_.toPairs(params.modelToPlateFile), ([model0, file]) => [config.getModelName(model0), file]))
	});
	// Add site
	_.set(objects, site, {
		type: "Site",
		evowareCarrier: params.evowareCarrier,
		evowareGrid: params.evowareGrid,
		evowareSite: params.evowareSite
	});

	// Add predicates for siteModelCompatibilities
	// const siteModelCompatibilities = [
	// 	{sites: [site], models: Object.keys(params.modelToPlateFile).map(model0 => config.getModelName(model0))}
	// ];
	// const output = {};
	// config.addSiteModelCompatibilities(siteModelCompatibilities, output);
	const predicates = _.map(params.modelToPlateFile, (file, model0) => ({
		"sealer.canAgentEquipmentProgramModelSite": {
			agent,
			equipment,
			program: file,
			model: config.getModelName(model0),
			site
		}
	}));
	// console.log("sealer.predicates: "+JSON.stringify(predicates, null, '\t'))

	const protocol = {
		schemas: module.exports.getSchemas(agent, equipment),
		objects,
		predicates,
		commandHandlers: module.exports.getCommandHandlers(agent, equipment),
	};
	return protocol;
}

module.exports = {
	configure,
	getSchemas: (agentName, equipmentName) => ({
		[`equipment.run|${agentName}|${equipmentName}`]: {
			properties: {
				agent: {description: "Agent identifier", type: "Agent"},
				equipment: {description: "Equipment identifier", type: "Equipment"},
				program: {description: "Program identifier for sealing", type: "string"},
				object: {description: "Plate identifier (optional)", type: "Plate"}
			},
			required: ["agent", "equipment", "program"]
		}
	}),
	getCommandHandlers: (agentName, equipmentName) => ({
		[`equipment.run|${agentName}|${equipmentName}`]: function(params, parsed, data) {
			const equipmentId = commandHelper.getParsedValue(parsed, data, "equipment", "evowareId");
			return {
				expansion: [{
					command: "evoware._facts",
					agent: agentName,
					factsEquipment: equipmentId,
					factsVariable: equipmentId+"_Seal",
					factsValue: parsed.value.program
				}],
				//effects: _.fromPairs([[params.object + ".sealed", true]])
			};
		},
	})
};
