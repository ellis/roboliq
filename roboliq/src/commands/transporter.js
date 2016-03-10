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

function makeMovePlateParams(parsed) {
	return _.merge({}, {
		agent: (parsed.objectName.agent) ? "?agent" : undefined,
		equipment: (parsed.objectName.equipment) ? "?equipment" : undefined,
		program: (parsed.objectName.program || parsed.value.program) ? "?program" : undefined,
		labware: "?labware",
		destination: "?destination"
	});
}

function makeMovePlateMethod(parsed, movePlateParams, n) {
	//console.log("makePlateLogic: "+JSON.stringify(parsed, null, '\t'));
	function makeArray(name, value) {
		return _.map(_.range(n), i => (_.isUndefined(value)) ? name+(i+1) : value);
	}
	const labware = parsed.objectName.object;
	const model = parsed.value.object.model;
	const origin = parsed.value.object.location;
	const destination = parsed.objectName.destination;

	const agents = makeArray("?agent", parsed.objectName.agent);
	const equipments = makeArray("?equipment", parsed.objectName.equipment);
	const programs = makeArray("?program", parsed.objectName.program || parsed.value.program)
	//const origin = parsed.value.object.location;
	//

	if (n === 0) {
		const name = "movePlate-0";
		return {
			"description": `${name}: transport plate from origin to destination in ${n} step(s)`,
			"task": {"movePlate": movePlateParams},
			"preconditions": [
				{"location": {"labware": labware, "site": destination}}
			],
			"subtasks": {"ordered": [
				{"print": {"text": name}}
			]}
		};
	}
	else if (n === 1) {
		const name = "movePlate-1";
		return {
			"description": `${name}: transport plate from origin to destination in ${n} step(s)`,
			"task": {"movePlate": movePlateParams},
			"preconditions": [
				{"model": {"labware": labware, "model": model}}, // TODO: Superfluous, but maybe check anyway
				{"location": {"labware": labware, "site": origin}}, // TODO: Superfluous, but maybe check anyway
				{"siteModel": {"site": origin, "siteModel": "?originModel"}},
				{"siteModel": {"site": destination, "siteModel": "?destinationModel"}},
				{"stackable": {"below": "?destinationModel", "above": model}},
				{"siteIsClear": {"site": destination}},
				{"siteCliqueSite": {"siteClique": "?siteClique1", "site": origin}},
				{"siteCliqueSite": {"siteClique": "?siteClique1", "site": destination}},
				{"transporter.canAgentEquipmentProgramSites": {"agent": agents[0], "equipment": equipments[0], "program": programs[0], "siteClique": "?siteClique1"}}
			],
			"subtasks": {"ordered": [
				{"print": {"text": name}},
				{"openAndMovePlate": {"agent": agents[0], "equipment": equipments[0], "program": programs[0], "labware": labware, "model": model, "origin": origin, "originModel": "?originModel", "destination": destination, "destinationModel": "?destinationModel"}}
			]}
		};
	}
	else if (n === 2) {
		const name = "movePlate-2";
		return {
			"description": `${name}: transport plate from origin to destination in ${n} step(s)`,
			"task": {"movePlate": movePlateParams},
			"preconditions": [
				{"model": {"labware": labware, "model": model}}, // TODO: can handle this in programatically
				{"location": {"labware": labware, "site": origin}}, // TODO: can handle this in programatically
				{"siteModel": {"site": origin, "siteModel": "?originModel"}},
				{"siteModel": {"site": destination, "siteModel": "?destinationModel"}},
				{"stackable": {"below": "?destinationModel", "above": model}},
				{"siteIsClear": {"site": destination}}, // TODO: Check this programmatically instead of via logic
				{"siteCliqueSite": {"siteClique": "?siteClique1", "site": origin}},
				{"siteCliqueSite": {"siteClique": "?siteClique2", "site": destination}},
				{"siteCliqueSite": {"siteClique": "?siteClique1", "site": "?site2"}},
				{"siteCliqueSite": {"siteClique": "?siteClique2", "site": "?site2"}},
				{"not": {"same": {"thing1": "?site2", "thing2": origin}}},
				{"not": {"same": {"thing1": "?site2", "thing2": destination}}},
				{"transporter.canAgentEquipmentProgramSites": {"agent": agents[0], "equipment": equipments[0], "program": programs[0], "siteClique": "?siteClique1"}},
				{"transporter.canAgentEquipmentProgramSites": {"agent": agents[1], "equipment": equipments[1], "program": programs[1], "siteClique": "?siteClique2"}},
				{"siteModel": {"site": "?site2", "siteModel": "?site2Model"}},
				{"stackable": {"below": "?site2Model", "above": model}},
				{"siteIsClear": {"site": "?site2"}}
			],
			"subtasks": {"ordered": [
				{"print": {"text": name}},
				{"openAndMovePlate": {"agent": agents[0], "equipment": equipments[0], "program": programs[0], "labware": labware, "model": model, "origin": origin, "originModel": "?originModel", "destination": "?site2", "destinationModel": "?site2Model"}},
				{"openAndMovePlate": {"agent": agents[1], "equipment": equipments[1], "program": programs[1], "labware": labware, "model": model, "origin": "?site2", "originModel": "?site2Model", "destination": destination, "destinationModel": "?destinationModel"}}
			]}
		};
	}
	else if (n === 3) {
		const name = "movePlate-3";
	 	return {
			"description": `${name}: transport plate from origin to destination in ${n} step(s)`,
			"task": {"movePlate": movePlateParams},
			"preconditions": [
				{"model": {"labware": labware, "model": model}}, // TODO: can handle this in programatically
				{"location": {"labware": labware, "site": origin}}, // TODO: can handle this in programatically
				{"siteModel": {"site": origin, "siteModel": "?originModel"}},
				{"siteModel": {"site": destination, "siteModel": "?destinationModel"}},
				{"stackable": {"below": "?destinationModel", "above": model}},
				{"siteIsClear": {"site": destination}}, // TODO: Check this programmatically instead of via logic
				{"siteCliqueSite": {"siteClique": "?siteClique1", "site": origin}},
				{"siteCliqueSite": {"siteClique": "?siteClique3", "site": destination}},
				{"siteCliqueSite": {"siteClique": "?siteClique1", "site": "?site2"}},
				{"siteCliqueSite": {"siteClique": "?siteClique2", "site": "?site2"}},
				{"siteCliqueSite": {"siteClique": "?siteClique2", "site": "?site3"}},
				{"siteCliqueSite": {"siteClique": "?siteClique3", "site": "?site3"}},
				{"not": {"same": {"thing1": "?site2", "thing2": origin}}},
				{"not": {"same": {"thing1": "?site2", "thing2": destination}}},
				{"not": {"same": {"thing1": "?site2", "thing2": "?site3"}}},
				{"not": {"same": {"thing1": "?site3", "thing2": origin}}},
				{"not": {"same": {"thing1": "?site3", "thing2": destination}}},
				{"transporter.canAgentEquipmentProgramSites": {"agent": agents[0], "equipment": equipments[0], "program": programs[0], "siteClique": "?siteClique1"}},
				{"transporter.canAgentEquipmentProgramSites": {"agent": agents[1], "equipment": equipments[1], "program": programs[1], "siteClique": "?siteClique2"}},
				{"transporter.canAgentEquipmentProgramSites": {"agent": agents[2], "equipment": equipments[2], "program": programs[2], "siteClique": "?siteClique3"}},
				{"siteModel": {"site": "?site2", "siteModel": "?site2Model"}},
				{"siteModel": {"site": "?site3", "siteModel": "?site3Model"}},
				{"stackable": {"below": "?site2Model", "above": model}},
				{"stackable": {"below": "?site3Model", "above": model}},
				{"siteIsClear": {"site": "?site2"}},
				{"siteIsClear": {"site": "?site3"}}
			],
			"subtasks": {"ordered": [
				{"print": {"text": name}},
				{"openAndMovePlate": {"agent": agents[0], "equipment": equipments[0], "program": programs[0], "labware": labware, "model": model, "origin": origin, "originModel": "?originModel", "destination": "?site2", "destinationModel": "?site2Model"}},
				{"openAndMovePlate": {"agent": agents[1], "equipment": equipments[1], "program": programs[1], "labware": labware, "model": model, "origin": "?site2", "originModel": "?site2Model", "destination": "?site3", "destinationModel": "?site3Model"}},
				{"openAndMovePlate": {"agent": agents[2], "equipment": equipments[2], "program": programs[2], "labware": labware, "model": model, "origin": "?site3", "originModel": "?site3Model", "destination": destination, "destinationModel": "?destinationModel"}}
			]}
		};
	}
	assert(false);
}

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


function debugMovePlateMethod(input, method, n) {
	const lines = [];
	var llpl = require('../HTN/llpl.js').create();
	llpl.initializeDatabase(input);
	//console.log("originId: "+originId)
	const criteria = method.preconditions;
	const queryAll = {"and": criteria};
	const queryResultsAll = llpl.query(queryAll);
	//console.log("queryResultsAll:\n"+JSON.stringify(queryResultsAll, null, '\t'));

	//console.log(queryResultsAll.length);
	if (queryResultsAll.length === 0) {
		lines.push(`\nmovePlate-${n} failed"`);
		//console.log("debug: "+criteria);
		_.forEach(criteria, criterion => {
			//console.log({criterion})
			const queryOne = {"and": [criterion]};
			const queryResultOne = llpl.query(queryOne);
			//console.log("queryResultOne: "+JSON.stringify(queryResultOne));
			if (queryResultOne.length === 0) {
				lines.push("FAILED: "+JSON.stringify(criterion));
			}
			else {
				//console.log("queryResults:\n"+JSON.stringify(queryResultOne, null, '\t'));
			}
		});
	}
	return lines.join("\n");
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
		//console.log("transporter.movePlate("+JSON.stringify(params)+")")
		const transporterLogic = require('./transporterLogic.json');

		const keys = ["null", "one", "two", "three"];
		const movePlateParams = makeMovePlateParams(parsed);

		const shop = require('../HTN/shop.js');
		let input0 = data.predicates;
		let plan;
		let errorLog = "";
		for (let i = 0; i < keys.length; i++) {
			const key = keys[i];
			const method = {method: makeMovePlateMethod(parsed, movePlateParams, i)};
			input0 = input0.concat(_.values(transporterLogic[key]));
			input0 = input0.concat([method]);
			const tasks = { "tasks": { "ordered": [{ movePlate: movePlateParams }] } };
			//console.log(JSON.stringify(tasks))
			const input = input0.concat([tasks]);
			var planner = shop.makePlanner(input);
			plan = planner.plan();
			//console.log(key, plan)
			if (!_.isEmpty(plan)) {
				//console.log("plan found for "+key)
				//console.log(planner.ppPlan(plan));
				break;
			}
			else {
				errorLog += debugMovePlateMethod(input, method.method, i);
			}
		}

		/*
		const transporterPredicates = _(transporterLogic).values().map(x => _.values(x)).flatten().value();
		var input0 = [].concat(data.predicates, transporterPredicates, [tasksOrdered]);
		var input = input0;
		//console.log(JSON.stringify(input, null, '\t'));

		var shop = require('../HTN/shop.js');
		var planner = shop.makePlanner(input);
		var plan = planner.plan();
		//console.log("plan:\n"+JSON.stringify(plan, null, '  '));
		var x = planner.ppPlan(plan);
		console.log(x);
		*/

		if (_.isEmpty(plan)) {
			console.log(errorLog);
			/*
			var agentId = params.agent || "?agent";
			var modelId = parsed.value.object.model || "?model";
			var originId = parsed.value.object.location || "?site";
			debug_movePlate_null(input0, agentId, parsed.objectName.object, modelId, originId, parsed.objectName.destination);
			debug_movePlate_one(input0, agentId, parsed.objectName.object, modelId, originId, parsed.objectName.destination);
			for (let i = 0; i <= 3; i++) {
				const method = makeMovePlateMethod(parsed, movePlateParams, i);
				debugMovePlateMethod()
			*/
		}
		if (_.isEmpty(plan)) {
			const x = _.merge({}, {agent: parsed.objectName.agent, equipment: parsed.objectName.equipment, program: parsed.objectName.program || parsed.value.program, model: parsed.value.object.model, origin: parsed.value.object.location, destination: parsed.objectName.destination});
			//console.log("transporter.movePlate: "+JSON.stringify(parsed, null, '\t'))
			return {errors: ["unable to find a transportation path for `"+parsed.objectName.object+"` from `"+misc.findObjectsValue(parsed.objectName.object+".location", data.objects)+"` to `"+parsed.objectName.destination+"`", JSON.stringify(x)]};
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
	},
	"transporter.doThenRestoreLocation": function(params, parsed, data) {
		//console.log("transporter.doThenRestoreLocation("+JSON.stringify(parsed, null, '\t')+")");

		const restoreSteps = [];
		for (let i = 0; i < parsed.value.objects.length; i++) {
			const labwareName = parsed.objectName[`objects.${i}`];
			const command = _.merge({}, {
				command: "transporter.movePlate",
				agent: parsed.objectName.agent,
				object: labwareName,
				destination: parsed.value.objects[i].location
			});
			restoreSteps.push(command);
		}

		const expansion = [
			_.values(parsed.value.steps),
			restoreSteps
		];
		//console.log("expansion:\n"+JSON.stringify(expansion, null, '\t'))

		return { expansion };
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
