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

function debug_movePlate_null(input, agentId, labwareId, modelId, originId, destinationId) {
	var llpl = require('../HTN/llpl.js').create();
	llpl.initializeDatabase(input);
	// Preconditions for movePlate-null
	const query = {"and": [
		{"location": {"labware": labwareId, "site": destinationId}}
	]};
	const queryResult = llpl.query(query);
	console.log("queryResultNull:\n"+JSON.stringify(queryResult, null, '\t'));
}

function debug_movePlate_one(input, agentId, labwareId, modelId, originId, destinationId) {

	var llpl = require('../HTN/llpl.js').create();
	llpl.initializeDatabase(input);
	//console.log("originId: "+originId)
	const criteria = [
		// From movePlate-one preconditions
		//{"model": {"labware": "?labware", "model": "?model"}},
		//{"location": {"labware": "?labware", "site": "?origin"}},
		//{"siteModel": {"site": "?origin", "siteModel": "?originModel"}},
		//{"siteModel": {"site": "?destination", "siteModel": "?destinationModel"}},
		//{"stackable": {"below": "?destinationModel", "above": "?model"}},
		//{"siteIsClear": {"site": "?destination"}},
		//{"siteCliqueSite": {"siteClique": "?siteClique1", "site": "?origin"}},
		//{"siteCliqueSite": {"siteClique": "?siteClique1", "site": "?destination"}},
		//{"transporter.canAgentEquipmentProgramSites": {"agent": "?agent", "equipment": "?equipment", "program": "?program", "siteClique": "?siteClique1"}},
		// From transporter._movePlate
		{"model": {"labware": labwareId, "model": modelId}},
		{"location": {"labware": labwareId, "site": originId}},
		{"siteModel": {"site": originId, "siteModel": "?originModel"}},
		{"siteModel": {"site": destinationId, "siteModel": "?destinationModel"}},
		{"stackable": {"below": "?destinationModel", "above": modelId}},
		{"siteIsClear": {"site": destinationId}},
		{"siteCliqueSite": {"siteClique": "?siteClique1", "site": originId}},
		{"siteCliqueSite": {"siteClique": "?siteClique1", "site": destinationId}},
		{"transporter.canAgentEquipmentProgramSites": {"agent": agentId, "equipment": "?equipment", "program": "?program", "siteClique": "?siteClique1"}},
		// From openAndMovePlate-openNeither
		{"siteIsOpen": {"site": originId}},
		{"siteIsOpen": {"site": destinationId}}
	];
	const queryAll = {"and": criteria};
	const queryResultsAll = llpl.query(queryAll);
	//console.log("queryResultsAll:\n"+JSON.stringify(queryResultsAll, null, '\t'));

	//console.log(queryResultsAll.length);
	if (queryResultsAll.length === 0) {
		console.log("movePlate-one failed for ", agentId, labwareId, modelId, originId, destinationId);
		//console.log("debug: "+criteria);
		const queryResultLabware = llpl.query({and: [
			{"location": {"labware": "?labware", "site": destinationId}}
		]});
		if (queryResultLabware.length > 0) {
			console.log("Labware already at destination site:\n"+JSON.stringify(queryResultLabware, null, '\t'));
		}
		_.forEach(criteria, criterion => {
			//console.log({criterion})
			const queryOne = {"and": [criterion]};
			const queryResultOne = llpl.query(queryOne);
			//console.log("queryResultOne: "+JSON.stringify(queryResultOne));
			if (queryResultOne.length === 0) {
				console.log("FAILED: "+JSON.stringify(criterion));
			}
			else {
				//console.log("queryResults:\n"+JSON.stringify(queryResultOne, null, '\t'));
			}
		});
	}
}

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
		effects[`${parsed.objectName.object}.location`] = parsed.objectName.destination;
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
		console.log("transporter.movePlate("+JSON.stringify(params)+")")
		var transporterLogic = require('./transporterLogic.json');
		/*
		const infix = (parsed.objectName.agent) ? "-a" : "";
		const taskNames = [`movePlate${infix}-null`, `movePlate${infix}-one`, `movePlate${infix}-two`];
		const taskParams = _.merge({}, {
			"agent": parsed.objectName.agent, // this may be undefined
			"labware": parsed.objectName.object,
			"destination": parsed.objectName.destination
		});
		const input0 = [].concat(data.predicates, transporterLogic);

		const shop = require('../HTN/shop.js');
		let plan;
		for (let i = 0; i < taskNames.length; i++) {
			const taskName = taskNames[i];
			const tasks = {
				"tasks": {
					"ordered": [_.fromPairs([[taskName, taskParams]])]
				}
			};
			console.log(JSON.stringify(tasks))
			const input = input0.concat([tasks]);
			var planner = shop.makePlanner(input);
			plan = planner.plan();
			console.log(taskName, plan)
			if (!_.isEmpty(plan)) {
				console.log("plan found for "+taskName)
				console.log(planner.ppPlan(plan));
				break;
			}
		}*/
		var taskList = [];
		if (parsed.objectName.agent) {
			taskList.push({
				"movePlate-a": {
					"agent": parsed.objectName.agent,
					"labware": parsed.objectName.object,
					"destination": parsed.objectName.destination
				}
			});
		} else {
			taskList.push({
				"movePlate": {
					"labware": parsed.objectName.object,
					"destination": parsed.objectName.destination
				}
			});
		}
		const tasksOrdered = {
			"tasks": {
				"ordered": taskList
			}
		};
		var input0 = [].concat(data.predicates, transporterLogic, [tasksOrdered]);
		var input = input0;
		//console.log(JSON.stringify(input, null, '\t'));

		var shop = require('../HTN/shop.js');
		var planner = shop.makePlanner(input);
		var plan = planner.plan();
		//console.log("plan:\n"+JSON.stringify(plan, null, '  '));
		var x = planner.ppPlan(plan);
		console.log(x);
		if (_.isEmpty(plan)) {
			var agentId = params.agent || "?agent";
			var modelId = parsed.value.object.model || "?model";
			var originId = parsed.value.object.location || "?site";
			debug_movePlate_null(input0, agentId, parsed.objectName.object, modelId, originId, parsed.objectName.destination);
			debug_movePlate_one(input0, agentId, parsed.objectName.object, modelId, originId, parsed.objectName.destination);
		}
		if (_.isEmpty(plan)) {
			return {errors: ["unable to find a transportation path for `"+parsed.objectName.object+"` from `"+misc.findObjectsValue(parsed.objectName.object+".location", data.objects)+"` to `"+parsed.objectName.destination+"`"]}
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
		effects[`${parsed.objectName.object}.location`] = parsed.objectName.destination;

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
