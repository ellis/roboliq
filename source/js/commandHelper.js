var _ = require('lodash');
var expect = require('./expect.js');
var misc = require('./misc.js');

function get(params, data, name, defaultValue) {
	// If there's no default value, require the variable's presence
	if (_.isUndefined(defaultValue))
		expect.paramsRequired(params, [name]);

	// If parameter is missing, use the default value
	var value1 = params[name];
	if (_.isUndefined(value1))
		value1 = defaultValue;

	var value2 = misc.getVariableValue(value1, data.objects, data.effects);
	expect.truthy({valueName: value1}, !_.isUndefined(value2), "not found");
	return value2;
}

function getObjectParameter(params, data, name, defaultValue) {
	var value = get(params, data, name, defaultValue);
	expect.truthy({paramName: name}, _.isNumber(value), "expected an objet, received: "+JSON.stringify(value));
	return value;
}

function getNumberParameter(params, data, name, defaultValue) {
	var value = get(params, data, name, defaultValue);
	expect.truthy({paramName: name}, _.isNumber(value), "expected a number, received: "+JSON.stringify(value));
	return value;
}

module.exports = {
	getParameter: get,
	getObjectParameter: getObjectParameter,
	getNumberParameter: getNumberParameter,
}
