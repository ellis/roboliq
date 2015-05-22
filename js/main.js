yaml = require('js-yaml');
fs = require('fs');
_ = require('underscore');

function isNumeric(n) {
  return !isNaN(parseFloat(n)) && isFinite(n);
}

function handleStep(step) {
	if (step.hasOwnProperty('command')) {
		handleCommand(step);
	_.each(step, function(value, key) {
		if (isNumeric(key)) {
		}
	});
}

function handleCommand(cmd) {

// Get document, or throw exception on error
try {
	var protocol = yaml.safeLoad(fs.readFileSync('protocol1.yaml', 'utf8'));
	console.log(protocol);
	
	
} catch (e) {
  console.log(e);
}
