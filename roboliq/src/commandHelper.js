var _ = require('lodash');
var assert = require('assert');
var expect = require('./expect.js');
var jmespath = require('jmespath');
var math = require('mathjs');
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

function getNameAndVolume(params, data, paramName, defaultValue) {
	var x = getNameAndValue(params, data, paramName, defaultValue);
	if (_.isNumber(x.value)) {
		x.value = math.unit(x.value, 'l');
	}
	else if (_.isString(x.value)) {
		x.value = math.eval(x.value);
	}
	expect.truthy({paramName: paramName}, math.unit('l').equalBase(x.value), "expected a volume with liter units (l, ul, etc.): "+JSON.stringify(x));
	return x;
}

function getNameAndTime(params, data, paramName, defaultValue) {
	var x0 = getNameAndValue(params, data, paramName, defaultValue);
	var x = _.clone(x0);
	if (_.isNumber(x.value)) {
		x.value = math.unit(x.value, 's');
	}
	else if (_.isString(x.value)) {
		x.value = math.eval(x.value);
	}
	expect.truthy({paramName: paramName}, math.unit('s').equalBase(x.value), "expected a value with time units (s, second, seconds, minute, minutes, h, hour, hours, day, days): "+JSON.stringify(x0));
	return x;
}

// REFACTOR: remove
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

// REFACTOR: remove
function getObjectParameter(params, data, name, defaultValue) {
	var value = get(params, data, name, defaultValue);
	expect.truthy({paramName: name}, _.isPlainObject(value), "expected an object, received: "+JSON.stringify(value));
	return value;
}

// REFACTOR: remove
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
			return (_.isUndefined(value)) ? undefined : {valueName: value};
		case "Any": return getNameAndValue(params, data, name, defaultValue);
		case "Number": return getNameAndNumber(params, data, name, defaultValue);
		case "Object": return getNameAndObject(params, data, name, defaultValue);
		case "Time": return getNameAndTime(params, data, name, defaultValue);
		case "Volume": return getNameAndVolume(params, data, name, defaultValue);
		case "File":
			var filename = params[name];
			var filedata = data.files[filename];
			if (_.isUndefined(filedata))
				filedata = defaultValue;
			if (_.isUndefined(filedata) && _.isUndefined(filename))
				return undefined;
			expect.truthy({paramName: name, valueName: filename}, !_.isUndefined(filedata), "file not loaded: "+filename);
			return {valueName: filename, value: filedata};
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
		type = (optional) ? type.substr(0, type.length - 1) : type;
		optional |= !_.isUndefined(defaultValue);

		// If not optional, require the variable's presence:
		if (!optional)
			expect.paramsRequired(params, [paramName]);

		var x = getTypedNameAndValue(type, params, data, paramName, defaultValue);
		if (_.isUndefined(x)) {
			expect.truthy({paramName: paramName}, optional, "missing value for required parameter");
			x = {};
		}
		return [paramName, x];
	})));
}

function getParsedValue(parsed, data, paramName, propertyName, defaultValue) {
	if (!parsed.hasOwnProperty(paramName)) {
		expect.truthy({paramName: paramName}, !_.isUndefined(defaultValue), "missing parameter value");
		return defaultValue;
	}
	var x = parsed[paramName];

	if (x.hasOwnProperty('value')) {
		var value = _.get(x.value, propertyName, defaultValue);
		var valueName = (x.valueName) ? x.valueName+"."+propertyName : paramName+"/"+propertyName;
		expect.truthy({valueName: valueName}, !_.isUndefined(value), "missing value");
		return value;
	}
	else {
		var valueName = x.valueName+"."+propertyName;
		var value = g(data, valueName);
		if (_.isUndefined(value))
			value = defaultValue;
		expect.truthy({valueName: valueName}, !_.isUndefined(value), "missing value");
		return value;
	}
}

function fixPredicateUndefines(predicate) {
	if (_.isArray(predicate)) {
		_.forEach(predicate, function(p) { fixPredicateUndefines(p); });
	}
	else if (_.isPlainObject(predicate)) {
		_.forEach(predicate, function(value, name) {
			if (_.isUndefined(value))
				predicate[name] = "?"+name;
			else if (_.isPlainObject(value)) {
				fixPredicateUndefines(value);
			}
		})
	}
}

/**
 * Query the logic database with the given predicates and return an extracted values from the query result.
 * @param  {Object} data         Command data
 * @param  {Array} predicates    Array of llpl predicates
 * @param  {String} queryExtract A jmespath query string to extract values of interest from the llpl result list
 * @return {Array}               Array of objects holding valid values
 */
function queryLogic(data, predicates, queryExtract) {
	var llpl = require('./HTN/llpl.js').create();
	llpl.initializeDatabase(data.predicates);

	fixPredicateUndefines(predicates);
	var query = {"and": predicates};
	var resultList = llpl.query(query);
	//console.log("resultList:\n"+JSON.stringify(resultList, null, '  '));

	if (_.isEmpty(resultList)) {
		var predicates2 = [];
		_.forEach(predicates, function(p, index) {
			var p2 = _.mapValues(p, function(value, name) { return "?"+name; });
			predicates2.push(p2);
			var query2 = {"and": predicates2};
			var resultList2 = llpl.query(query);
			expect.truthy({}, !_.isEmpty(resultList2), "logical query found no result for predicate "+(index+1)+" in: "+JSON.stringify(query));
		});
	}

	if (queryExtract) {
		var alternatives = jmespath.search(resultList, queryExtract);
		return alternatives;
	}
	else {
		return resultList;
	}
}

module.exports = {
	getParameter: get,
	getObjectParameter: getObjectParameter,
	getNumberParameter: getNumberParameter,
	getParsedValue: getParsedValue,
	parseParams: parseParams,
	queryLogic: queryLogic
}
