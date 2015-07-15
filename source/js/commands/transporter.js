var _ = require('lodash');
var misc = require('../misc.js');

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

var commandHandlers = {
	"transporter.instruction.movePlate": function(params, data) {
		var effects = {};
		effects[params.object + ".location"] = params.destination;
		return {
			effects: effects
		};
	},
	/**
	 * params: [agent], object, destination
	 */
	"transporter.action.movePlate": function(params, data) {
		//console.log("transporter.action.movePlate("+JSON.stringify(params)+")")
		var transporterLogic = require('./transporterLogic.json');
		var taskList = [];
		if (params.hasOwnProperty("agent")) {
			taskList.push({
				"movePlate-a": {
					"agent": params.agent,
					"labware": params['object'],
					"destination": params.destination
				}
			});
		} else {
			taskList.push({
				"movePlate": {
					"labware": params['object'],
					"destination": params.destination
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

		/*// DEBUG
		var llpl = require('../HTN/llpl.js');
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
		}
		var queryResults = llpl.query(query);
		console.log("queryResults:\n"+JSON.stringify(queryResults, null, '\t'));
		// END DEBUG*/

		//console.log(JSON.stringify(input, null, '\t'));
		var shop = require('../HTN/shop.js');
		var planner = shop.makePlanner(input);
		var plan = planner.plan();
		//console.log("plan:");
		//console.log(JSON.stringify(plan, null, '  '));
		//var x = planner.ppPlan(plan);
		//console.log(x);
		if (_.isEmpty(plan)) {
			return {errors: ["unable to find a transportation path for `"+params.object+"` from `"+misc.findObjectsValue(params.object+".location", data.objects)+"` to `"+params.destination+"`"]}
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
		effects[params.object + ".location"] = params.destination;

		return {
			expansion: expansion,
			effects: effects
		};
	}
};

var planHandlers = {
	"movePlateAction": function(params, parentParams, data) {
		return [{
			command: "transporter.instruction.movePlate",
			agent: params.agent,
			equipment: params.equipment,
			program: params.program,
			object: params.labware,
			destination: params.destination
		}];
	}
};

module.exports = {
	objectToPredicateConverters: objectToPredicateConverters,
	commandHandlers: commandHandlers,
	planHandlers: planHandlers
};
