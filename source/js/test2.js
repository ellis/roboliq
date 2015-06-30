var utils = require('./HTN/utils.js');
var movePlatePlanning = require('./movePlatePlanning.js');

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

var moveExample = [].concat(movePlatePlanning.evowareConfig, movePlatePlanning.taskDefs, [
	{"isLabware": {"labware": "plate1"}},
	{"isLabware": {"labware": "plate2"}},
	{"isPlate": {"labware": "plate1"}},
	{"isPlate": {"labware": "plate2"}},
	{"model": {"labware": "plate1", "model": "ourlab.model1"}},
	{"model": {"labware": "plate2", "model": "ourlab.model1"}},
	//{"location": {"labware": "plate1", "site": "ourlab.mario.P3"}},
	{"location": {"labware": "plate1", "site": "ourlab.mario.P2"}},

	//
	// Tasks
	//
	{"tasks": {"ordered": [
		//{"movePlate-a": {"agent": "ourlab.mario.evoware", "labware": "plate1", "destination": "ourlab.mario.SEALER"}},
		//{"movePlate": {"labware": "plate1", "destination": "ourlab.mario.P2"}}
		{"movePlate": {"labware": "plate1", "destination": "ourlab.mario.P3"}}
	]}},
]);

//console.log(JSON.stringify(sealerExample, null, '\t'));
console.log(JSON.stringify(moveExample, null, '\t'));
var shop = require('./HTN/Plan/shop.js');
//var p = shop.makePlanner(sealerExample);
var p = shop.makePlanner(moveExample);
var plan = p.plan();
//console.log("state:");
//console.log(JSON.stringify(plan.state));
var x = p.ppPlan(plan);
console.log(x);
