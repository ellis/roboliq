var utils = require('./HTN/utils.js');

console.log(utils.isEmpty([]));

var llpl = require('./HTN/Logic/llpl.js');
var sicpDB = require('./HTN/Logic/sicpDB.js');
llpl.initializeDatabase(sicpDB);
//console.log(JSON.stringify(llpl.query({"lives-near": {"person1": "?x", "person2": "?y"}})));

console.log();

var shop = require('./HTN/Plan/shop.js');
var basicExample = require('./HTN/Plan/basicExample.js');
console.log(JSON.stringify(basicExample));
var p = shop.makePlanner(basicExample);
var plan = p.plan();
console.log(JSON.stringify(plan));
var x = p.ppPlan(plan);
console.log(x);
