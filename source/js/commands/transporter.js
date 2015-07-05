var _ = require('lodash');

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
		var shop = require('../HTN/shop.js');
		var planner = shop.makePlanner(input);
		var plan = planner.plan();
		//console.log("plan:");
		//console.log(JSON.stringify(plan, null, '  '));
		var x = planner.ppPlan(plan);
		//console.log(x);
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
