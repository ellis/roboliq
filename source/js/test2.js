var utils = require('./HTN/utils.js');

//console.log(utils.isEmpty([]));

/*
var llpl = require('./HTN/Logic/llpl.js');
var sicpDB = require('./HTN/Logic/sicpDB.js');
llpl.initializeDatabase(sicpDB);
console.log(llpl.query({"lives-near": {"person1": "?x", "person2": "?y"}}));
console.log();
*/

/*
var shop = require('./HTN/Plan/shop.js');
var basicExample = require('./HTN/Plan/basicExample.js');
console.log(JSON.stringify(basicExample));
var p = shop.makePlanner(basicExample);
var plan = p.plan();
console.log(JSON.stringify(plan));
var x = p.ppPlan(plan);
console.log(x);
console.log();
*/

var sealerExample = [
	// State
	{"isAgent": {"agent": "ourlab.mario.evoware"}},
	{"isSealer": {"equipment": "ourlab.mario.sealer"}},
	{"isLabware": {"labware": "plate1"}},
	{"isModel": {"model": "model1"}},
	{"isModel": {"model": "siteModel1"}},
	{"isPlate": {"labware": "plate1"}},
	{"isSite": {"site": "ourlab.mario.P3"}},
	{"isSite": {"site": "ourlab.mario.SEALER"}},
	{"isSiteModel": {"model": "siteModel1"}},
	{"siteModel": {"site": "ourlab.mario.SEALER", "siteModel": "siteModel1"}},
	{"stackable": {"below": "siteModel1", "above": "model1"}},
	{"canAgentEquipmentProgramModelSite": {"agent": "ourlab.mario.evoware", "equipment": "ourlab.mario.sealer", "program": "sealerProgram1", "model": "model1", "site": "ourlab.mario.SEALER"}},
	{"model": {"labware": "plate1", "model": "model1"}},
	{"location": {"labware": "plate1", "site": "ourlab.mario.SEALER"}},

	// Tasks
	{"tasks": {"ordered": [
		{"sealPlate": {"labware": "plate1"}}
	]}},

	// Actions
	{"action": {"description": "fully specified seal command",
		"task": {"sealAction": {"agent": "?agent", "equipment": "?equipment", "program": "?program", "labware": "?labware", "model": "?model", "site": "?site"}},
		"preconditions": [
			{"model": {"labware": "?labware", "model": "?model"}},
			{"canAgentEquipmentProgramModelSite": {"agent": "?agent", "equipment": "?equipment", "program": "?program", "model": "?model", "site": "?site"}},
			{"location": {"labware": "?labware", "site": "?site"}}
		],
		"deletions": [],
		"additions": [{"plateHasSeal": {"labware": "?labware"}}]
	}},

	// Methods
	{"method": {"description": "method for sealing",
		"task": {"sealPlate": {"labware": "?labware"}},
		"preconditions": [
			{"model": {"labware": "?labware", "model": "?model"}},
			{"canAgentEquipmentProgramModelSite": {"agent": "?agent", "equipment": "?equipment", "program": "?program", "model": "?model", "site": "?site"}}
		],
		"subtasks": {"ordered": [
			{"sealAction": {"agent": "?agent", "equipment": "?equipment", "program": "?program", "labware": "?labware", "model": "?model", "site": "?site"}}
		]}
	}}
];

var moveExample = [
	//
	// State
	//
	{"isAgent": {"agent": "ourlab.mario.evoware"}},
	//{"isSealer": {"equipment": "ourlab.mario.sealer"}},
	{"isTransporter": {"equipment": "ourlab.mario.roma1"}},
	{"isLabware": {"labware": "plate1"}},
	{"isLabware": {"labware": "plate2"}},
	{"isModel": {"model": "model1"}},
	{"isModel": {"model": "siteModel1"}},
	{"isPlate": {"labware": "plate1"}},
	{"isPlate": {"labware": "plate2"}},
	{"isSite": {"site": "ourlab.mario.P2"}},
	{"isSite": {"site": "ourlab.mario.P3"}},
	{"isSite": {"site": "ourlab.mario.SEALER"}},
	{"isSiteModel": {"model": "siteModel1"}},
	{"siteModel": {"site": "ourlab.mario.P2", "siteModel": "siteModel1"}},
	{"siteModel": {"site": "ourlab.mario.P3", "siteModel": "siteModel1"}},
	{"siteModel": {"site": "ourlab.mario.SEALER", "siteModel": "siteModel1"}},
	{"stackable": {"below": "siteModel1", "above": "model1"}},
	//{"agentEquipmentProgramModelSiteCanSeal": {"agent": "ourlab.mario.evoware", "equipment": "ourlab.mario.sealer", "program": "sealerProgram1", "model": "model1", "site": "ourlab.mario.SEALER"}},
	{"movePlate_canAgentEquipmentProgramModelSite": {"agent": "ourlab.mario.evoware", "equipment": "ourlab.mario.roma1", "program": "Narrow", "model": "model1", "site": "ourlab.mario.P2"}},
	{"movePlate_canAgentEquipmentProgramModelSite": {"agent": "ourlab.mario.evoware", "equipment": "ourlab.mario.roma1", "program": "Narrow", "model": "model1", "site": "ourlab.mario.P3"}},
	{"movePlate_canAgentEquipmentProgramModelSite": {"agent": "ourlab.mario.evoware", "equipment": "ourlab.mario.roma1", "program": "Narrow", "model": "model1", "site": "ourlab.mario.SEALER"}},
	{"model": {"labware": "plate1", "model": "model1"}},
	{"model": {"labware": "plate2", "model": "model1"}},
	{"location": {"labware": "plate1", "site": "ourlab.mario.P3"}},
	//{"location": {"labware": "plate2", "site": "ourlab.mario.SEALER"}},
	//{"movePlate_excludePath": {"siteA": "ourlab.mario.P3", "siteB": "ourlab.mario.SEALER"}},

	//
	// Tasks
	//
	{"tasks": {"ordered": [
		{"movePlate": {"labware": "plate1", "destination": "ourlab.mario.SEALER"}}
	]}},

	//
	// Actions
	//
	{"action": {"description": "transport plate from origin to destination",
		"task": {"movePlateAction": {"agent": "?agent", "equipment": "?equipment", "program": "?program", "labware": "?labware", "model": "?model", "origin": "?origin", "originModel": "?originModel", "destination": "?destination", "destinationModel": "?destinationModel"}},
		"preconditions": [
			{"model": {"labware": "?labware", "model": "?model"}},
			{"location": {"labware": "?labware", "site": "?origin"}},
			{"siteModel": {"site": "?origin", "siteModel": "?originModel"}},
			{"siteModel": {"site": "?destination", "siteModel": "?destinationModel"}},
			{"stackable": {"below": "?destinationModel", "above": "?model"}},
			{"siteIsClear": {"site": "?destination"}},
			{"not": {"movePlate_excludePath": {"origin": "?origin", "destination": "?destination"}}},
			{"movePlate_canAgentEquipmentProgramModelSite": {"agent": "?agent", "equipment": "?equipment", "program": "?program", "model": "?model", "site": "?origin"}},
			{"movePlate_canAgentEquipmentProgramModelSite": {"agent": "?agent", "equipment": "?equipment", "program": "?program", "model": "?model", "site": "?destination"}},
		],
		"deletions": [],
		"additions": [{"plateHasSeal": {"labware": "?labware"}}]
	}},

	//
	// Methods
	//

	// movePlate-null
	{"method": {"description": "transport plate from origin to destination",
		"task": {"movePlate": {"labware": "?labware", "destination": "?destination"}},
		"preconditions": [
			{"location": {"labware": "?labware", "site": "?destination"}}
		],
		"subtasks": {"ordered": [
		]}
	}},

	// movePlate-direct
	{"method": {"description": "transport plate from origin to destination",
		"task": {"movePlate": {"labware": "?labware", "destination": "?destination"}},
		"preconditions": [
			{"model": {"labware": "?labware", "model": "?model"}},
			{"location": {"labware": "?labware", "site": "?origin"}},
			{"siteModel": {"site": "?origin", "siteModel": "?originModel"}},
			{"siteModel": {"site": "?destination", "siteModel": "?destinationModel"}},
			{"stackable": {"below": "?destinationModel", "above": "?model"}},
			{"siteIsClear": {"site": "?destination"}},
			{"movePlate_pathOk": {"siteA": "?origin", "siteB": "?destination"}},
			//{"not": {"movePlate_excludePath": {"siteA": "?origin", "siteB": "?destination"}}},
			//{"not": {"movePlate_excludePath": {"siteA": "?destination", "siteB": "?origin"}}},
			{"movePlate_canAgentEquipmentProgramModelSite": {"agent": "?agent", "equipment": "?equipment", "program": "?program", "model": "?model", "site": "?origin"}},
			{"movePlate_canAgentEquipmentProgramModelSite": {"agent": "?agent", "equipment": "?equipment", "program": "?program", "model": "?model", "site": "?destination"}},
		],
		"subtasks": {"ordered": [
			{"movePlateAction": {"agent": "?agent", "equipment": "?equipment", "program": "?program", "labware": "?labware", "model": "?model", "origin": "?origin", "originModel": "?originModel", "destination": "?destination", "destinationModel": "?destinationModel"}},
		]}
	}},

	// movePlate-two
	{"method": {"description": "transport plate from origin to destination",
		"task": {"movePlate": {"labware": "?labware", "destination": "?destination"}},
		"preconditions": [
			{"model": {"labware": "?labware", "model": "?model"}},
			{"location": {"labware": "?labware", "site": "?origin"}},
			{"siteModel": {"site": "?origin", "siteModel": "?originModel"}},
			{"siteModel": {"site": "?destination", "siteModel": "?destinationModel"}},
			{"stackable": {"below": "?destinationModel", "above": "?model"}},
			{"siteIsClear": {"site": "?destination"}},
			{"movePlate_pathOk": {"siteA": "?origin", "siteB": "?site2"}},
			{"movePlate_pathOk": {"siteA": "?site2", "siteB": "?destination"}},
			{"movePlate_canAgentEquipmentProgramModelSite": {"agent": "?agent", "equipment": "?equipment", "program": "?program", "model": "?model", "site": "?origin"}},
			{"movePlate_canAgentEquipmentProgramModelSite": {"agent": "?agent", "equipment": "?equipment", "program": "?program", "model": "?model", "site": "?site2"}},
			{"movePlate_canAgentEquipmentProgramModelSite": {"agent": "?agent", "equipment": "?equipment", "program": "?program", "model": "?model", "site": "?destination"}},
			{"siteModel": {"site": "?site2", "siteModel": "?site2Model"}},
			{"stackable": {"below": "?site2Model", "above": "?model"}},
			{"siteIsClear": {"site": "?site2"}},
		],
		"subtasks": {"ordered": [
			{"movePlateAction": {"agent": "?agent", "equipment": "?equipment", "program": "?program", "labware": "?labware", "model": "?model", "origin": "?origin", "originModel": "?originModel", "destination": "?site2", "destinationModel": "?site2Model"}},
			{"movePlateAction": {"agent": "?agent", "equipment": "?equipment", "program": "?program", "labware": "?labware", "model": "?model", "origin": "?site2", "originModel": "?site2Model", "destination": "?destination", "destinationModel": "?destinationModel"}},
		]}
	}},

	//
	// Rules
	//

	// same: Two things are the same if they unify.
	{"<--": {"same": {"thing1": "?thing", "thing2": "?thing"}}},

	// clear: a site is clear if no labware is on it
	{"<--": {"siteIsClear": {"site": "?site"},
		"and": [{"not": {"location": {"labware": "?labware", "site": "?site"}}}]}
	},

	// clear: a site is clear if no labware is on it
	{"<--": {"movePlate_pathOk": {"siteA": "?siteA", "siteB": "?siteB"},
		"and": [
			{"not": {"movePlate_excludePath": {"siteA": "?siteA", "siteB": "?siteB"}}},
			{"not": {"movePlate_excludePath": {"siteA": "?siteB", "siteB": "?siteA"}}},
		]}
	},

];

//console.log(JSON.stringify(sealerExample, null, '\t'));
var shop = require('./HTN/Plan/shop.js');
//var p = shop.makePlanner(sealerExample);
var p = shop.makePlanner(moveExample);
var plan = p.plan();
console.log("state:");
console.log(JSON.stringify(plan.state));
var x = p.ppPlan(plan);
console.log(x);
