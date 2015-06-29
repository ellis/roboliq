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

var sealerExample0 = [
	// State
	{
		"isAgent": {"agent": "ourlab.mario.evoware"},
		"isSealer": {"sealer": "sealer1"},
		"isLabware": {"labware": "plate1"},
		"isModel": {"model": "model1"},
		"isModel": {"model": "siteModel1"},
		"isPlate": {"labware": "plate1"},
		"isSite": {"site": "site1"},
		"isSiteModel": {"model": "siteModel1"},
		"model": {"labware": "plate1", "model": "model1"},
		"siteModel": {"site": "site1", "siteModel": "siteModel1"},
		"stackable": {"below": "siteModel1", "above": "model1"},
		"location": {"labware": "plate1", "site": "site1"}
	},

	// Tasks
	{"tasks": {"ordered": [
		//{"sealPlate": {"labware": "plate1"}}
		{"sealAction": {"agent": "ourlab.mario.evoware", "equipment": "sealer1", "program": "someprogram", "labware": "plate1", "model": "model1", "site": "site1", "siteModel": "siteModel1"}}
	]}},

	// Actions
	{"action": {"description": "fully specified seal command",
		"task": {"sealAction": {"agent": "?agent", "equipment": "?equipment", "program": "?program", "labware": "?labware", "model": "?model", "site": "?site", "siteModel": "?siteModel"}},
		"preconditions": [
			{"isAgent": {"agent": "?agent"}},
			{"isSealer": {"equipment": "?equipment"}},
			{"isPlate": {"labware": "?labware"}},
			{"isModel": {"model": "?model"}},
			{"isSiteModel": {"model": "?siteModel"}},
			{"location": {"labware": "?labware", "site": "?site"}}
		],
		"deletions": [],
		"additions": []
	}}/*,

   // Methods
   {"method": {"description": "drop thing1 and pick up thing2",
	       "task": {"sealPlate": {"labware": "?labware"}},
	       "preconditions": [{"have": {"agent": "?agent", "thing": "?thing1"}}],
	       "subtasks": {"ordered": [{"drop": {"agent": "?agent", "thing": "?thing1"}},
                                        {"pickup": {"agent": "?agent", "thing": "?thing2"}}]}}},
   {"method": {"description": "drop thing2 and pick up thing1",
	       "task": {"swap": {"agent": "?agent",
				 "thing1": "?thing1",
				 "thing2": "?thing2"}},
	       "preconditions": [{"have": {"agent": "?agent", "thing": "?thing2"}}],
	       "subtasks": {"ordered": [{"drop": {"agent": "?agent", "thing": "?thing2"}},
                                        {"pickup": {"agent": "?agent", "thing": "?thing1"}}]}}},*/
   // Rules: no rules in this example
];

var sealerExample = [
	// State
	{"isAgent": {"agent": "ourlab.mario.evoware"}},
	{"isSealer": {"equipment": "sealer1"}},
	{"isLabware": {"labware": "plate1"}},
	{"isModel": {"model": "model1"}},
	{"isModel": {"model": "siteModel1"}},
	{"isPlate": {"labware": "plate1"}},
	{"isSite": {"site": "site1"}},
	{"isSiteModel": {"model": "siteModel1"}},
	{"model": {"labware": "plate1", "model": "model1"}},
	{"siteModel": {"site": "site1", "siteModel": "siteModel1"}},
	{"stackable": {"below": "siteModel1", "above": "model1"}},
	{"location": {"labware": "plate1", "site": "site1"}},

	// Tasks
	{"tasks": {"ordered": [
		{"sealPlate": {"labware": "plate1"}}
	]}},

	// Actions
	{"action": {"description": "fully specified seal command",
		"task": {"sealAction": {"agent": "?agent", "equipment": "?equipment", "program": "?program", "labware": "?labware", "model": "?model", "site": "?site", "siteModel": "?siteModel"}},
		"preconditions": [
			{"isAgent": {"agent": "?agent"}},
			{"isSealer": {"equipment": "?equipment"}},
			{"isPlate": {"labware": "?labware"}},
			{"isModel": {"model": "?model"}},
			{"isSiteModel": {"model": "?siteModel"}},
			{"location": {"labware": "?labware", "site": "?site"}}
		],
		"deletions": [],
		"additions": []
	}},

	// Methods
	{"method": {"description": "method for sealing",
		"task": {"sealPlate": {"labware": "?labware"}},
		"preconditions": [
			{"isAgent": {"agent": "?agent"}},
			{"isSealer": {"equipment": "?equipment"}},
			{"isPlate": {"labware": "?labware"}},
			{"isModel": {"model": "?model"}},
			{"isSiteModel": {"model": "?siteModel"}},
			{"location": {"labware": "?labware", "site": "?site"}}
		],
		"subtasks": {"ordered": [
			{"sealAction": {"agent": "?agent", "equipment": "?equipment", "program": "program1", "labware": "?labware", "model": "?model", "site": "?site", "siteModel": "?siteModel"}}
		]}
	}}
	/*,
   {"method": {"description": "drop thing1 and pick up thing2",
	       "task": {"sealPlate": {"labware": "?labware"}},
	       "preconditions": [{"have": {"agent": "?agent", "thing": "?thing1"}}],
	       "subtasks": {"ordered": [{"drop": {"agent": "?agent", "thing": "?thing1"}},
                                        {"pickup": {"agent": "?agent", "thing": "?thing2"}}]}}},
   {"method": {"description": "drop thing2 and pick up thing1",
	       "task": {"swap": {"agent": "?agent",
				 "thing1": "?thing1",
				 "thing2": "?thing2"}},
	       "preconditions": [{"have": {"agent": "?agent", "thing": "?thing2"}}],
	       "subtasks": {"ordered": [{"drop": {"agent": "?agent", "thing": "?thing2"}},
                                        {"pickup": {"agent": "?agent", "thing": "?thing1"}}]}}},*/
   // Rules: no rules in this example
];

console.log(JSON.stringify(sealerExample));
//var llpl = require('./HTN/Logic/llpl.js');
//llpl.initializeDatabase(sealerExample);
//console.log(llpl.query({"isPlate": {"labware": "?labware"}}));
var shop = require('./HTN/Plan/shop.js');
var p = shop.makePlanner(sealerExample);
console.log("state:");
console.log(p['state']);
var plan = p.plan();
console.log(JSON.stringify(plan));
var x = p.ppPlan(plan);
console.log(x);
