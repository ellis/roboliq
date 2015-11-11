/**
 * Namespace for the ``transporter`` commands.
 * @namespace transporter
 * @version v1
 */

/**
 * Transporter commands module.
 * @module commands/transporter
 * @return {Protocol}
 * @version v1
 */

var _ = require('lodash');
import yaml from 'yamljs';
var expect = require('../expect.js');
var misc = require('../misc.js');

/**
 * Create predicates for objects of type = "Transporter"
 * @static
 */
var objectToPredicateConverters = {
	"Transporter": function(name, data) {
		return {
			value: [{
				"isTransporter": {
					"equipment": name
				}
			}]
		};
	},
};

/**
 * Handlers for {@link transporter} commands.
 * @static
 */
var commandHandlers = {
	/**
	 * Transport a plate to a destination.
	 *
	 * Handler should return `effects` with the plate's new location.
	 *
	 * @typedef _movePlate
	 * @memberof transporter
	 * @property {string} command - "transporter._movePlate"
	 * @property {string} agent - Agent identifier
	 * @property {string} equipment - Equipment identifier
	 * @property {string} object - Plate identifier
	 * @property {string} destination - Location identifier
	 */
	"transporter._movePlate": function(params, parsed, data) {
		var effects = {};
		effects[`${parsed.object.objectName}.location`] = parsed.destination.objectName;
		return {
			effects: effects
		};
	},
	/**
	 * Transport a plate to a destination.
	 *
	 * @typedef movePlate
	 * @memberof transporter
	 * @property {string} command - "transporter.movePlate"
	 * @property {string} [agent] - Agent identifier
	 * @property {string} object - Plate identifier
	 * @property {string} destination - Location identifier
	 */
	"transporter.movePlate": function(params, parsed, data) {
		//console.log("transporter.movePlate("+JSON.stringify(params)+")")
		var transporterLogic = require('./transporterLogic.json');
		var taskList = [];
		if (parsed.agent.objectName) {
			taskList.push({
				"movePlate-a": {
					"agent": parsed.agent.objectName,
					"labware": parsed.object.objectName,
					"destination": parsed.destination.objectName
				}
			});
		} else {
			taskList.push({
				"movePlate": {
					"labware": parsed.object.objectName,
					"destination": parsed.destination.objectName
				}
			});
		}
		var tasks = {
			"tasks": {
				"ordered": taskList
			}
		};
		var input = [].concat(data.predicates, transporterLogic, [tasks]);
		//console.log(JSON.stringify(input, null, '\t'));

		// DEBUG
		/*var llpl = require('../HTN/llpl.js').create();
		llpl.initializeDatabase(input);
		var agentId = params.agent || "?agent";
		var modelId = misc.findObjectsValue(params.object+".model", data.objects) || "?model";
		var originId = misc.findObjectsValue(params.object+".location", data.objects) || "?site";
		var query = {
			"and": [
				{"movePlate_canAgentEquipmentProgramModelSite": {"agent": agentId, "equipment": "?equipment", "program": "?program", "model": modelId, "site": originId}},
				{"movePlate_canAgentEquipmentProgramModelSite": {"agent": agentId, "equipment": "?equipment", "program": "?program", "model": modelId, "site": params.destination}}
			]
		};
		console.log("originId: "+originId)
		query = {
			"and": [
				{"transporter.canAgentEquipmentProgramSites": {"agent": "?agent", "equipment": "?equipment", "program": "?program", "siteClique": "?siteClique1"}},
				{"siteCliqueSite": {"siteClique": "?siteClique1", "site": originId}},
				{"siteCliqueSite": {"siteClique": "?siteClique1", "site": params.destination}},
			]
		};
		query = {
			"and": [
				{"siteCliqueSite": {"siteClique": "?siteClique1", "site": originId}},
				{"siteCliqueSite": {"siteClique": "?siteClique2", "site": params.destination}},
				{"siteCliqueSite": {"siteClique": "?siteClique1", "site": "?site2"}},
				{"siteCliqueSite": {"siteClique": "?siteClique2", "site": "?site2"}},
				{"transporter.canAgentEquipmentProgramSites": {"agent": "?agent1", "equipment": "?equipment1", "program": "?program1", "siteClique": "?siteClique1"}},
				{"transporter.canAgentEquipmentProgramSites": {"agent": "?agent2", "equipment": "?equipment2", "program": "?program2", "siteClique": "?siteClique2"}},
				//{"siteCliqueSite": {"siteClique": "?siteClique1", "site": originId}},
			]
		};
		var queryResults = llpl.query(query);
		console.log("queryResults:\n"+JSON.stringify(queryResults, null, '\t'));
		// END DEBUG*/

		//console.log(JSON.stringify(input, null, '\t'));
		var shop = require('../HTN/shop.js');
		var planner = shop.makePlanner(input);
		var plan = planner.plan();
		//console.log("plan:\n"+JSON.stringify(plan, null, '  '));
		//var x = planner.ppPlan(plan);
		//console.log(x);
		if (_.isEmpty(plan)) {
			return {errors: ["unable to find a transportation path for `"+parsed.object.objectName+"` from `"+misc.findObjectsValue(parsed.object.objectName+".location", data.objects)+"` to `"+parsed.destination.objectName+"`"]}
		}
		var tasks = planner.listAndOrderTasks(plan, true);
		//console.log("Tasks:")
		//console.log(JSON.stringify(tasks, null, '  '));
		var cmdList = _(tasks).map(function(task) {
			return _(task).map(function(taskParams, taskName) {
				return (data.planHandlers.hasOwnProperty(taskName)) ? data.planHandlers[taskName](taskParams, params, data) : [];
			}).flatten().value();
		}).flatten().value();
		//console.log("cmdList:")
		//console.log(JSON.stringify(cmdList, null, '  '));

		// Create the expansion object
		var expansion = {};
		var i = 1;
		_.forEach(cmdList, function(cmd) {
			expansion[i.toString()] = cmd;
			i += 1;
		});

		// Create the effets object
		var effects = {};
		effects[`${parsed.object.objectName}.location`] = parsed.destination.objectName;

		return {
			expansion: expansion,
			effects: effects
		};
	}
};

/**
 * Plan handler to allow other modules to use `transporter._movePlate` as a
 * planning action.
 * @static
 */
var planHandlers = {
	"transporter._movePlate": function(params, parentParams, data) {
		return [{
			command: "transporter._movePlate",
			agent: params.agent,
			equipment: params.equipment,
			program: params.program,
			object: params.labware,
			destination: params.destination
		}];
	}
};

module.exports = {
	roboliq: "v1",
	objectToPredicateConverters: objectToPredicateConverters,
	schemas: yaml.load(__dirname+'/../schemas/transporter.yaml'),
	commandHandlers: commandHandlers,
	planHandlers: planHandlers
};
