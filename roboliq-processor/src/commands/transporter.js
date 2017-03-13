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

const _ = require('lodash');
import yaml from 'yamljs';
const commandHelper = require('../commandHelper.js');
const expect = require('../expect.js');
const misc = require('../misc.js');

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
 * Transport a lid from a container to a destination site.
 */
function moveLidFromContainerToSite(params, parsed, data) {
	// console.log("transporter.moveLidFromContainerToSite:"); console.log(JSON.stringify(parsed, null, '\t'))
	const transporterLogic = require('./transporterLogic.json');

	const keys = ["null", "one"];
	const movePlateParams = makeMoveLidFromContainerToSiteParams(parsed);

	const shop = require('../HTN/shop.js');
	const llpl = require('../HTN/llpl.js').create();
	llpl.initializeDatabase(data.predicates);
	let input0 = data.predicates;
	let plan;
	let errorLog = "";
	for (let i = 0; i < keys.length; i++) {
		const key = keys[i];
		const method = {method: makeMoveLidFromContainerToSiteMethod(parsed, movePlateParams, i)};
		input0 = input0.concat(_.values(transporterLogic[key]));
		input0 = input0.concat([method]);
		if (transporterLogic.hasOwnProperty(key)) {
			llpl.addToDatabase(transporterLogic[key]);
		}
		llpl.addToDatabase([method]);
		// const fs = require('fs');
		// fs.writeFileSync("a.json", JSON.stringify(llpl.database, null, '\t'));
		const queryResultsAll = queryMovePlateMethod(llpl, method.method, i);
		// If we didn't find a path for this method:
		if (queryResultsAll.length === 0) {
			//const queryResultOne = llpl.query({stackable: {below: "?below", above: "ourlab.model.lidModel_384_square"}});
			//console.log("queryResultOne: " +JSON.stringify(queryResultOne))
			const text = debugMovePlateMethod(llpl, method.method, queryResultsAll, i);
			errorLog += text;
		}
		// If we did find a path:
		else {
			// console.log(`${queryResultsAll.length} path(s) found, e.g.: ${JSON.stringify(queryResultsAll[0])}`);
			const tasks = { "tasks": { "ordered": [{ moveLidFromContainerToSite: movePlateParams }] } };
			const input = input0.concat([tasks]);
			var planner = shop.makePlanner(input);
			plan = planner.plan();
			//console.log(key, plan)
			if (!_.isEmpty(plan)) {
				//console.log("plan found for "+key)
				//console.log(planner.ppPlan(plan));
			}
			else {
				console.log("apparently unable to open some site or something")
			}
			break;
		}
	}

	if (_.isEmpty(plan)) {
		console.log(errorLog);
		const x = _.pickBy({agent: parsed.objectName.agent, equipment: parsed.objectName.equipment, program: parsed.objectName.program || parsed.value.program, model: parsed.value.object.model, origin: parsed.objectName["object.location"], destination: parsed.objectName.destination});
		//console.log("transporter.movePlate: "+JSON.stringify(parsed, null, '\t'))
		return {errors: ["unable to find a transportation path for lid `"+parsed.objectName.object+"` on `"+parsed.objectName.container+"` from `"+misc.findObjectsValue(parsed.objectName.container+".location", data.objects)+"` to `"+parsed.objectName.destination+"`", JSON.stringify(x)]};
	}
	var tasks = planner.listAndOrderTasks(plan, true);
	//console.log("Tasks:")
	//console.log(JSON.stringify(tasks, null, '  '));
	var cmdList = _(tasks).map(function(task) {
		return _(task).map(function(taskParams, taskName) {
			return (data.planHandlers.hasOwnProperty(taskName)) ? data.planHandlers[taskName](taskParams, params, data) : [];
		}).flatten().value();
	}).flatten().value();
	// console.log("cmdList: "+JSON.stringify(cmdList, null, '  '));

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
moveLidFromContainerToSite.inputSpec = {
	lid: "object",
	lidModel: "object*model",
	container: "object*location",
	model: "object*location*model",
	origin: "object*location*location",
	destination: "destination"
};

/**
 * Take the parsed parameters passed to `transporter.moveLidFromContainerToSite`
 * and create the parameter list for the task `moveLidFromContainerToSite` in `makeMoveLidFromContainerToSiteMethod()`.
 * If any of the following parameters are not specified, then they will also
 * be a part of the created parameters: agent, equipment, program
 */
function makeMoveLidFromContainerToSiteParams(parsed) {
	// console.log("makeMoveLidFromContainerToSiteParams: "+JSON.stringify(parsed))
	return _.pickBy({
		agent: (parsed.objectName.agent) ? "?agent" : undefined,
		equipment: (parsed.objectName.equipment) ? "?equipment" : undefined,
		program: (parsed.objectName.program || parsed.value.program) ? "?program" : undefined,
		lid: "?lid",
		container: "?container",
		destination: "?destination"
	});
}

function makeMoveLidFromContainerToSiteMethod(parsed, moveLidParams, n) {
	// console.log("makeMoveLidFromContainToSiteMethod: "+JSON.stringify(parsed, null, '\t'));
	const {lid, lidModel, container, model, origin, destination} = parsed.input;

	function makeArray(name, value) {
		return _.map(_.range(n), i => (_.isUndefined(value)) ? name+(i+1) : value);
	}
	const agents = makeArray("?agent", parsed.objectName.agent);
	const equipments = makeArray("?equipment", parsed.objectName.equipment);
	const programs = makeArray("?program", parsed.objectName.program || parsed.value.program)
	//const origin = parsed.value.object.location;
	//

	if (n === 0) {
		const name = "moveLidFromContainerToSite-0";
		return {
			"description": `${name}: transport lid from container to destination in ${n} step(s)`,
			"task": {"moveLidFromContainerToSite": moveLidParams},
			"preconditions": [
				{"location": {"labware": lid, "site": destination}}
			],
			"subtasks": {"ordered": [
				{"print": {"text": name}}
			]}
		};
	}
	else if (n === 1) {
		const name = "moveLidFromContainerToSite-1";
		return {
			"description": `${name}: transport plate from origin to destination in ${n} step(s)`,
			"task": {"moveLidFromContainerToSite": moveLidParams},
			"preconditions": [
				{"model": {"labware": lid, "model": lidModel}}, // TODO: Superfluous, but maybe check anyway
				{"location": {"labware": lid, "site": container}},
				//{"labwareHasLid": {"labware": container}},
				{"model": {"labware": container, "model": model}},
				{"location": {"labware": container, "site": origin}},
				{"siteIsOpen": {"site": origin}},
				{"siteIsOpen": {"site": destination}},
				{"siteModel": {"site": origin, "siteModel": "?originModel"}},
				{"siteModel": {"site": destination, "siteModel": "?destinationModel"}},
				{"stackable": {"below": "?destinationModel", "above": lidModel}},
				{"siteIsClear": {"site": destination}},
				{"siteCliqueSite": {"siteClique": "?siteClique1", "site": origin}},
				{"siteCliqueSite": {"siteClique": "?siteClique1", "site": destination}},
				{"transporter.canAgentEquipmentProgramSites": {"agent": agents[0], "equipment": equipments[0], "program": programs[0], "siteClique": "?siteClique1"}}
			],
			"subtasks": {"ordered": [
				{"print": {"text": name}},
				{"transporter._moveLidFromContainerToSite": {"agent": agents[0], "equipment": equipments[0], "program": programs[0], lid, lidModel, container, model, origin, "originModel": "?originModel", destination, "destinationModel": "?destinationModel"}}
			]}
		};
	}
	assert(false);
}




/**
 * Transport a lid from an origin site to a container.
 */
function moveLidFromSiteToContainer(params, parsed, data) {
	console.log("transporter.moveLidFromSiteToContainer:"); console.log(JSON.stringify(parsed, null, '\t'));
	if (parsed.input.originType != "Site") {
		expect.throw({paramName: "object"}, "expected lid to be on a site; instead lid's location is "+parsed.input.origin);
	}
	const transporterLogic = require('./transporterLogic.json');

	const keys = ["null", "one"];
	const movePlateParams = makeMoveLidFromSiteToContainerParams(parsed);

	const shop = require('../HTN/shop.js');
	const llpl = require('../HTN/llpl.js').create();
	llpl.initializeDatabase(data.predicates);
	let input0 = data.predicates;
	let plan;
	let errorLog = "";
	for (let i = 0; i < keys.length; i++) {
		const key = keys[i];
		const method = {method: makeMoveLidFromSiteToContainerMethod(parsed, movePlateParams, i, llpl)};
		input0 = input0.concat(_.values(transporterLogic[key]));
		input0 = input0.concat([method]);
		if (transporterLogic.hasOwnProperty(key)) {
			llpl.addToDatabase(transporterLogic[key]);
		}
		llpl.addToDatabase([method]);
		// const fs = require('fs');
		// fs.writeFileSync("a.json", JSON.stringify(llpl.database, null, '\t'));
		const queryResultsAll = queryMovePlateMethod(llpl, method.method, i);
		// If we didn't find a path for this method:
		if (queryResultsAll.length === 0) {
			//const queryResultOne = llpl.query({stackable: {below: "?below", above: "ourlab.model.lidModel_384_square"}});
			//console.log("queryResultOne: " +JSON.stringify(queryResultOne))
			const text = debugMovePlateMethod(llpl, method.method, queryResultsAll, i);
			errorLog += text;
		}
		// If we did find a path:
		else {
			// console.log(`${queryResultsAll.length} path(s) found, e.g.: ${JSON.stringify(queryResultsAll[0])}`);
			const tasks = { "tasks": { "ordered": [{ moveLidFromSiteToContainer: movePlateParams }] } };
			const input = input0.concat([tasks]);
			var planner = shop.makePlanner(input);
			plan = planner.plan();
			//console.log(key, plan)
			if (!_.isEmpty(plan)) {
				//console.log("plan found for "+key)
				//console.log(planner.ppPlan(plan));
			}
			else {
				console.log("apparently unable to open some site or something")
			}
			break;
		}
	}

	if (_.isEmpty(plan)) {
		console.log(errorLog);
		const x = _.pickBy({agent: parsed.objectName.agent, equipment: parsed.objectName.equipment, program: parsed.objectName.program || parsed.value.program, model: parsed.value.object.model, origin: parsed.objectName["object.location"], destination: parsed.objectName.destination});
		//console.log("transporter.movePlate: "+JSON.stringify(parsed, null, '\t'))
		return {errors: ["unable to find a transportation path for lid `"+parsed.objectName.object+"` from `"+parsed.objectName.origin+"` to `"+parsed.objectName.container+"` at `"+parsed.value.container.location+"`", JSON.stringify(x)]};
	}
	var tasks = planner.listAndOrderTasks(plan, true);
	//console.log("Tasks:")
	//console.log(JSON.stringify(tasks, null, '  '));
	var cmdList = _(tasks).map(function(task) {
		return _(task).map(function(taskParams, taskName) {
			return (data.planHandlers.hasOwnProperty(taskName)) ? data.planHandlers[taskName](taskParams, params, data) : [];
		}).flatten().value();
	}).flatten().value();
	// console.log("cmdList: "+JSON.stringify(cmdList, null, '  '));

	// Create the expansion object
	var expansion = {};
	var i = 1;
	_.forEach(cmdList, function(cmd) {
		expansion[i.toString()] = cmd;
		i += 1;
	});

	// Create the effets object
	var effects = {};
	effects[`${parsed.objectName.object}.location`] = parsed.objectName.container;
	// console.log("expansion: "+JSON.stringify(expansion, null, '\t'))
	// console.log("effects: "+JSON.stringify(effects, null, '\t'))

	return {
		expansion: expansion,
		effects: effects
	};
}
moveLidFromSiteToContainer.inputSpec = {
	origin: "object*location",
	originType: "object*location*type",
	lid: "object",
	lidModel: "object*model",
	container: "container",
	model: "container*model",
	destination: "container*location"
};

/**
 * Take the parsed parameters passed to `transporter.moveLidFromSiteToContainer`
 * and create the parameter list for the task `moveLidFromSiteToContainer` in `makeMoveLidFromSiteToContainerMethod()`.
 * If any of the following parameters are not specified, then they will also
 * be a part of the created parameters: agent, equipment, program
 */
function makeMoveLidFromSiteToContainerParams(parsed) {
	// console.log("makeMoveLidFromSiteToContainerParams: "+JSON.stringify(parsed))
	return _.pickBy({
		agent: (parsed.objectName.agent) ? "?agent" : undefined,
		equipment: (parsed.objectName.equipment) ? "?equipment" : undefined,
		program: (parsed.objectName.program || parsed.value.program) ? "?program" : undefined,
		lid: "?lid",
		origin: "?origin",
		container: "?container"
	});
}

function makeMoveLidFromSiteToContainerMethod(parsed, moveLidParams, n, llpl) {
	// console.log("makeMoveLidFromContainToSiteMethod: "+JSON.stringify(parsed, null, '\t'));
	const {origin, lid, lidModel, container, model, destination} = parsed.input;
	// console.log({lid, lidModel, container, model, origin, destination})

	const originQuery = llpl.query({"siteModel": {"site": origin, "siteModel": "?originModel"}});
	const destinationQuery = llpl.query({"siteModel": {"site": destination, "siteModel": "?destinationModel"}});
	console.log("originQuery: "+JSON.stringify(originQuery, null, '\t'))
	console.log("destinationQuery: "+JSON.stringify(destinationQuery, null, '\t'))
	const originModel = originQuery[0].siteModel.siteModel;
	const destinationModel = destinationQuery[0].siteModel.siteModel;

	// console.log("labwareHasNoLid?: "+JSON.stringify(llpl.query({"labwareHasNoLid": {"site": container}})))

	function makeArray(name, value) {
		return _.map(_.range(n), i => (_.isUndefined(value)) ? name+(i+1) : value);
	}
	const agents = makeArray("?agent", parsed.objectName.agent);
	const equipments = makeArray("?equipment", parsed.objectName.equipment);
	const programs = makeArray("?program", parsed.objectName.program || parsed.value.program)

	if (n === 0) {
		const name = "moveLidFromSiteToContainer-0";
		return {
			"description": `${name}: transport lid from container to destination in ${n} step(s)`,
			"task": {"moveLidFromSiteToContainer": moveLidParams},
			"preconditions": [
				{"location": {"labware": lid, "labware": destination}}
			],
			"subtasks": {"ordered": [
				{"print": {"text": name}}
			]}
		};
	}
	else if (n === 1) {
		const name = "moveLidFromSiteToContainer-1";
		return {
			"description": `${name}: transport plate from origin to destination in ${n} step(s)`,
			"task": {"moveLidFromSiteToContainer": moveLidParams},
			"preconditions": [
				// {"model": {"labware": lid, "model": lidModel}}, // TODO: Superfluous, but maybe check anyway
				{"location": {"labware": lid, "site": origin}},
				{"model": {"labware": container, "model": model}},
				{"location": {"labware": container, "site": destination}},
				{"siteIsOpen": {"site": origin}},
				{"siteIsOpen": {"site": destination}},
				{"siteModel": {"site": origin, "siteModel": originModel}},
				{"siteModel": {"site": destination, "siteModel": destinationModel}},
				{"stackable": {"below": model, "above": lidModel}},
				{"labwareHasNoLid": {"labware": container}},
				{"siteCliqueSite": {"siteClique": "?siteClique1", "site": origin}},
				{"siteCliqueSite": {"siteClique": "?siteClique1", "site": destination}},
				{"transporter.canAgentEquipmentProgramSites": {"agent": agents[0], "equipment": equipments[0], "program": programs[0], "siteClique": "?siteClique1"}}
			],
			"subtasks": {"ordered": [
				{"print": {"text": name}},
				{"transporter._moveLidFromSiteToContainer": {"agent": agents[0], "equipment": equipments[0], "program": programs[0], lid, lidModel, container, model, origin, "originModel": originModel, destination, "destinationModel": destinationModel}}
			]}
		};
	}
	assert(false);
}

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
	//console.log("makeMovePlateMethod: "+JSON.stringify(parsed, null, '\t'));
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

function queryMovePlateMethod(llpl, method, n) {
	//console.log("originId: "+originId)
	const criteria = method.preconditions;
	const queryAll = {"and": criteria};
	const queryResultsAll = llpl.query(queryAll);
	return queryResultsAll;
}

function debugMovePlateMethod(llpl, method, queryResultsAll, n) {
	const lines = [];
	const criteria = method.preconditions;

	//console.log(queryResultsAll.length);
	if (queryResultsAll.length === 0) {
		lines.push(`\nmovePlate-${n} failed:`);
		// console.log("debug: "+criteria);
		// let failures = 0;
		_.forEach(criteria, criterion => {
			// console.log({criterion})
			const queryOne = {"and": [criterion]};
			const queryResultOne = llpl.query(queryOne);
			//console.log("queryResultOne: "+JSON.stringify(queryResultOne));
			if (queryResultOne.length === 0) {
				// failures++;
				lines.push("FAILED: "+JSON.stringify(criterion));
			}
			else {
				//console.log("queryResults:\n"+JSON.stringify(queryResultOne, null, '\t'));
			}
		});
	}
	else {
		console.log("found paths: "+queryResultsAll)
	}
	return lines.join("\n");
}

/**
 * Handlers for {@link transporter} commands.
 * @static
 */
var commandHandlers = {
	/**
	 * Transport a lid from labware to site or from site to labware.
	 *
	 * Handler should return `effects` with the lid's new location
	 * and set or remove labware's `hasLid` property.
	 */
	"transporter._moveLidFromContainerToSite": function(params, parsed, data) {
		return {effects: {
			//[`${parsed.objectName.container}.hasLid`]: false,
			[`${parsed.objectName.object}.location`]: parsed.objectName.destination
		}};
	},
	/**
	 * Transport a lid from labware to site or from site to labware.
	 *
	 * Handler should return `effects` with the lid's new location
	 * and set or remove labware's `hasLid` property.
	 */
	"transporter._moveLidFromSiteToContainer": function(params, parsed, data) {
		return {effects: {
			//[`${parsed.objectName.container}.hasLid`]: false,
			[`${parsed.objectName.object}.location`]: parsed.objectName.container
		}};
	},
	/**
	 * Transport a plate to a destination.
	 *
	 * Handler should return `effects` with the plate's new location.
	 */
	"transporter._movePlate": function(params, parsed, data) {
		return {effects: {
			[`${parsed.objectName.object}.location`]: parsed.objectName.destination
		}};
	},
	"transporter.moveLidFromContainerToSite": moveLidFromContainerToSite,
	"transporter.moveLidFromSiteToContainer": moveLidFromSiteToContainer,
	/**
	 * Transport a plate to a destination.
	 */
	"transporter.movePlate": function(params, parsed, data) {
		//console.log("transporter.movePlate("+JSON.stringify(params)+")")
		const transporterLogic = require('./transporterLogic.json');

		const keys = ["null", "one", "two", "three"];
		const movePlateParams = makeMovePlateParams(parsed);

		const shop = require('../HTN/shop.js');
		const llpl = require('../HTN/llpl.js').create();
		llpl.initializeDatabase(data.predicates);
		let input0 = data.predicates;
		let plan;
		let errorLog = "";
		for (let i = 0; i < keys.length; i++) {
			const key = keys[i];
			const method = {method: makeMovePlateMethod(parsed, movePlateParams, i)};
			input0 = input0.concat(_.values(transporterLogic[key]));
			input0 = input0.concat([method]);
			if (transporterLogic.hasOwnProperty(key)) {
				llpl.addToDatabase(transporterLogic[key]);
			}
			llpl.addToDatabase([method]);
			// const fs = require('fs');
			// fs.writeFileSync("a.json", JSON.stringify(llpl.database, null, '\t'));
			const queryResultsAll = queryMovePlateMethod(llpl, method.method, i);
			// If we didn't find a path for this method:
			if (queryResultsAll.length === 0) {
				const text = debugMovePlateMethod(llpl, method.method, queryResultsAll, i);
				errorLog += text;
			}
			// If we did find a path:
			else {
				// console.log(`${queryResultsAll.length} path(s) found, e.g.: ${JSON.stringify(queryResultsAll[0])}`);
				const tasks = { "tasks": { "ordered": [{ movePlate: movePlateParams }] } };
				const input = input0.concat([tasks]);
				var planner = shop.makePlanner(input);
				plan = planner.plan();
				//console.log(key, plan)
				if (!_.isEmpty(plan)) {
					//console.log("plan found for "+key)
					//console.log(planner.ppPlan(plan));
				}
				else {
					console.log("apparently unable to open some site or something")
				}
				break;
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

		const expansion = _.cloneDeep(_.values(parsed.value.steps));
		// console.log("objects: "+JSON.stringify(parsed.value.objects))
		for (let i = 0; i < parsed.value.objects.length; i++) {
			const labwareName = parsed.objectName[`objects.${i}`];
			const command = _.merge({}, {
				command: "transporter.movePlate",
				agent: parsed.objectName.agent,
				equipment: parsed.objectName.equipment,
				program: parsed.value.program,
				object: labwareName,
				destination: parsed.value.objects[i].location
			});
			expansion.push(command);
		}
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
	},
	"transporter._moveLidFromContainerToSite": function(params, parentParams, data) {
		return [{
			command: "transporter._moveLidFromContainerToSite",
			agent: params.agent,
			equipment: params.equipment,
			program: params.program,
			object: params.lid,
			container: params.container,
			destination: params.destination
		}];
	},
	"transporter._moveLidFromSiteToContainer": function(params, parentParams, data) {
		return [{
			command: "transporter._moveLidFromSiteToContainer",
			agent: params.agent,
			equipment: params.equipment,
			program: params.program,
			object: params.lid,
			origin: params.origin,
			container: params.container
		}];
	},
};

module.exports = {
	roboliq: "v1",
	objectToPredicateConverters: objectToPredicateConverters,
	schemas: yaml.load(__dirname+'/../schemas/transporter.yaml'),
	commandHandlers: commandHandlers,
	planHandlers: planHandlers
};
