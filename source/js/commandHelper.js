var _ = require('lodash');
var expect = require('./expect.js');
var misc = require('./misc.js');

/**
 * Try to get a value from data.objects with the given name.
 * @param  {Object} data Data object with 'objects' property
 * @param  {String} name Name of the object value to lookup
 * @return {Any} The value at the given path, if any
 */
function g(data, name) {
	data.accesses.push(name);
	return _.get(data.objects, name);
}

function getNameAndValue(params, data, paramName, defaultValue) {
	// Get value from params
	var value1 = params[paramName];
	// If parameter is missing, use the default value:
	if (_.isUndefined(value1))
		value1 = defaultValue;

	// Follow de-references:
	var references = [];
	var valueName = undefined;
	while (_.isString(value1) && _.startsWith(value1, "${") && references.indexOf(value1) < 0) {
		references.push(value1);
		valueName = value1.substring(2, value1.length - 1);
		if (_.has(data.objects, valueName)) {
			var type2 = g(data, valueName+".type");
			if (type2 === "Variable") {
				value1 = g(data, valueName+".value");
			}
			else {
				value1 = g(data, valueName);
			}
		}
	}

	return (_.isUndefined(value1))
		? undefined
		: _.merge({}, {valueName: valueName, value: value1});
}

function getNameAndObject(params, data, paramName, defaultValue) {
	var x = getNameAndValue(params, data, paramName, defaultValue);
	var value = x.value;
	if (_.isString(value) && !_.isEmpty(value) && !_.startsWith(value, '"')) {
		x.valueName = value;
		x.value = g(data, x.valueName);
	}
	expect.truthy({paramName: paramName}, _.isPlainObject(x.value), "expected an object, received: "+JSON.stringify(x.value));
	return x;
}

function getNameAndNumber(params, data, paramName, defaultValue) {
	var x = getNameAndValue(params, data, paramName, defaultValue);
	expect.truthy({paramName: paramName}, _.isNumber(x.value), "expected a number, received: "+JSON.stringify(x.value));
	return x;
}

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
	expect.truthy({paramName: name}, _.isPlainObject(value), "expected an object, received: "+JSON.stringify(value));
	return value;
}

function getNumberParameter(params, data, name, defaultValue) {
	var value = get(params, data, name, defaultValue);
	expect.truthy({paramName: name}, _.isNumber(value), "expected a number, received: "+JSON.stringify(value));
	return value;
}

function getTypedNameAndValue(type, params, data, name, defaultValue) {
	switch (type) {
		case "name":
			var value = params[name];
			if (_.isUndefined(value))
				value = defaultValue;
			return (_.isUndefined(value)) ? undefined : {value: value};
		case "Any": return getNameAndValue(params, data, name, defaultValue);
		case "Number": return getNameAndNumber(params, data, name, defaultValue);
		case "Object": return getNameAndObject(params, data, name, defaultValue);
	}
	return undefined;
}

function parseParams(params, data, specs) {
	return _.zipObject(_.compact(_.map(specs, function(info, paramName) {
		var type = undefined;
		var optional = false;
		var defaultValue = undefined;

		if (_.isString(info)) {
			type = info;
		}
		else {
			assert(_.isPlainObject(info));
			assert(info.type);
			type = info.type;
			defaultValue = info.default;
		}

		var optional = _.endsWith(type, "?");
		type = (optional) ? type.substr(0, type.length - 1) : info;
		// If not optional, require the variable's presence:
		if (!optional)
			expect.paramsRequired(params, [paramName]);

		var x = getTypedNameAndValue(type, params, data, paramName, defaultValue);
		if (_.isUndefined(x)) {
			expect.truthy({paramName: paramName}, optional, "missing value for required parameter");
			return undefined;
		}
		else {
			return [paramName, x];
		}
	})));
}

module.exports = {
	getParameter: get,
	getObjectParameter: getObjectParameter,
	getNumberParameter: getNumberParameter,
	parseParams: parseParams
}
