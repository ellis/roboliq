var utils = require('./HTN/utils.js');

console.log(utils.isEmpty([]));

var llpl = require('./HTN/Logic/llpl.js');
var sicpDB = require('./HTN/Logic/sicpDB.js');
llpl.initializeDatabase(sicpDB);
console.log(JSON.stringify(llpl.query({"lives-near": {"person1": "?x", "person2": "?y"}})));
