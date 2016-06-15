import _ from 'lodash';
import commandHelper from '../../commandHelper.js';
import expect from '../../expect.js';

/**
 * Have Evoware execute an external command.
 * @param  {string} agentName - Agent identifier
 * @param  {string} path - path to command to execute
 * @param  {array} args - array of arguments to pass
 * @param  {boolean} wait - true if evoware should wait for the command to complete execution
 * @return {object} an object representing an Evoware 'Execute' instruction.
 */
function makeEvowareExecute(agentName, path, args, wait) {
	return {
		command: "evoware._execute",
		agent: agentName,
		path, args, wait
	};
}

function makeEvowareFacts(parsed, data, variable, value, labwareName) {
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
	return _.merge(result2, {factsValue: value2, labware: labwareName});
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
	makeEvowareExecute,
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
		"evoware._execute": {
			description: "An Evoware Execute command",
			properties: {
				agent: {description: "Agent identifier", type: "Agent"},
				path: {description: "Path to program to execute", type: "string"},
				args: {description: "Arguments to pass to the exeternal program", type: "array", items: {type: "string"}},
				wait: {description: "True, if Evoware should wait until the program finishes execution", type: "boolean"}
			},
			required: ["path", "args", "wait"]
		},
		"evoware._facts": {
			description: "An Evoware FACTS command",
			properties: {
				agent: {description: "Agent identifier", type: "Agent"},
				factsEquipment: {type: "string"},
				factsVariable: {type: "string"},
				factsValue: {type: "string"},
				labware: {type: "Plate"}
			},
			required: ["factsEquipment", "factsVariable"]
		},
		"evoware._raw": {
			description: "An Evoware direct command",
			properties: {
				agent: {description: "Agent identifier", type: "Agent"},
				commands: {type: "string"}
			},
			required: ["commands"]
		},
		"evoware._userPrompt": {
			description: "An Evoware UserPrompt command",
			properties: {
				agent: {description: "Agent identifier", type: "Agent"},
				text: {description: "Text to show the user", type: "string"},
				beep: {description: "0: no beep, 1: beep once, 2: beep three times, 3: beep every 3 seconds", type: "integer"},
				autoclose: {description: "number of second to leave the prompt open before autoclosing it and continuing operation (-1 means no autoclose)", type: "integer"}
			},
			required: ["text"]
		}
	}),
	getCommandHandlers: () => ({
		"evoware._execute": function(params, parsed, data) {},
		"evoware._facts": function(params, parsed, data) {},
		"evoware._raw": function(params, parsed, data) {},
		"evoware._userPrompt": function(params, parsed, data) {},
	}),
};
