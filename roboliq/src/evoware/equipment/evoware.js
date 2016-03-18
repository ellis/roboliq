import _ from 'lodash';
import commandHelper from '../../commandHelper.js';
import expect from '../../expect.js';

function makeEvowareFacts(parsed, data, variable, value) {
	const equipmentId = commandHelper.getParsedValue(parsed, data, "equipment", "evowareId");
	const result2 = {
		command: "evoware._facts",
		agent: parsed.objectName.agent,
		factsEquipment: equipmentId,
		factsVariable: equipmentId+"_"+variable
	};
	const value2 = (_.isFunction(value))
		? value(parsed, data)
		: value;
	return _.merge(result2, {factsValue: value2});
}

/**
 * Expect spec of this form:
 * ``{siteModel: string, sites: [string], labwareModels: [string]}``
 */
function makeSiteModelPredicates(spec) {
	return _.flatten([
		{isSiteModel: {model: spec.siteModel}},
		_.map(spec.sites, site => ({siteModel: {site, siteModel: spec.siteModel}})),
		_.map(spec.labwareModels, labwareModel => ({stackable: {below: spec.siteModel, above: labwareModel}}))
	]);
}

/**
 * Expect specs of this form:
 * ``{<transporter>: {<program>: [site names]}}``
 */
function makeTransporterPredicates(namespaceName, agentName, specs) {
	let siteCliqueId = 1;
	const l = [];
	_.forEach(specs, (programs, equipment) => {
		_.forEach(programs, (cliques, program) => {
			_.forEach(cliques, (sites) => {
				const siteClique = `${namespaceName}.siteClique${siteCliqueId}`;
				siteCliqueId++;
				_.forEach(sites, site => {
					l.push({"siteCliqueSite": {siteClique, site}});
				});
				l.push({
					"transporter.canAgentEquipmentProgramSites": {
						"agent": agentName,
						equipment,
						program,
						siteClique
					}
				});
			});
		});
	});
	return l;
}

module.exports = {
	makeEvowareFacts,
	makeSiteModelPredicates,
	makeTransporterPredicates,

	objectToPredicateConverters: {
		"EvowareRobot": function(name) {
			return {
				value: [{
					"isAgent": {
						"agent": name
					}
				}]
			};
		}
	},
	getSchemas: () => ({
		"EvowareRobot": {
			properties: {
				type: {enum: ["EvowareRobot"]},
				config: {description: "configuration options for evoware", type: "object"}
			},
			required: ["type"]
		},
		"EvowareWashProgram": {
			properties: {
				type: {enum: ["EvowareWashProgram"]}
			},
			required: ["type"]
		},
	}),
	getCommandHandlers: () => ({
		"evoware._facts": function(params, parsed, data) {},
	}),
};
