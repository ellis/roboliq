var _ = require('lodash');
var assert = require('assert');
var expect = require('./expect.js');
var jmespath = require('jmespath');
import math from 'mathjs';
var misc = require('./misc.js');
import wellsParser from './parsers/wellsParser.js';

/**
 * Try to get a value from data.objects with the given name.
 * @param  {object} data Data object with 'objects' property
 * @param  {array|string} name Name of the object value to lookup
 * @return {Any} The value at the given path, if any
 */
function g(data, name, dflt) {
	if (_.isArray(name))
		name = name.join('.');

	if (_.isArray(data.accesses))
		data.accesses.push(name);
	else
		data.accesses = [name];

	return _.get(data.objects, name, dflt);
}

function dereferenceVariable(data, name) {
	const result = {};
	while (_.has(data.objects, name)) {
		const value = g(data, name);
		result.objectName = name;
		//console.log({name, value})
		if (value.type === 'Variable') {
			result.value = value.value;
			name = value.value;
		}
		else {
			result.value = value;
			break;
		}
	}
	return (_.isEmpty(result)) ? undefined : result;
}

function lookupValue(params, data, paramName, defaultValue) {
	// Get value from params
	const value0 = _.get(params, paramName, defaultValue);
	const result = {};

	if (_.isUndefined(value0)) {
		// do nothing
	}
	else if (!_.isString(value0) || _.startsWith(value0, '"')) {
		result.value = value0;
	}
	else {
		result.value = value0;
		const deref = dereferenceVariable(data, value0);
		if (deref) {
			result.value = deref.value;
			result.objectName = deref.objectName;
		}
	}

	return (result.value || result.objectName)
		? _.merge({}, result)
		: undefined;
}

function lookupString(params, data, paramName, defaultValue) {
	// Get value from params
	var value1 = params[paramName];
	// If parameter is missing, use the default value:
	if (_.isUndefined(value1))
		value1 = defaultValue;

	// Follow de-references:
	var references = [];
	var objectName = undefined;
	while (_.isString(value1) && _.startsWith(value1, "${") && references.indexOf(value1) < 0) {
		references.push(value1);
		objectName = value1.substring(2, value1.length - 1);
		if (_.has(data.objects, objectName)) {
			var type2 = g(data, objectName+".type");
			if (type2 === "Variable") {
				value1 = g(data, objectName+".value");
			}
			else {
				value1 = g(data, objectName);
			}
		}
	}

	return (_.isUndefined(value1))
		? undefined
		: _.merge({}, {objectName: objectName, value: value1.toString()});
}

function lookupObject(params, data, paramName, defaultValue) {
	var x = lookupValue(params, data, paramName, defaultValue);
	if (x) {
		var value = x.value;
		if (_.isString(value) && !_.isEmpty(value) && !_.startsWith(value, '"')) {
			x.objectName = value;
			x.value = g(data, x.objectName);
		}
		expect.truthy({paramName: paramName}, _.isPlainObject(x.value), "expected an object, received: "+JSON.stringify(x.value));
	}
	return x;
}

function lookupNumber(params, data, paramName, defaultValue) {
	var x = lookupValue(params, data, paramName, defaultValue);
	expect.truthy({paramName: paramName}, _.isNumber(x.value), "expected a number, received: "+JSON.stringify(x.value));
	return x;
}

function lookupVolume(params, data, paramName, defaultValue) {
	var x = lookupValue(params, data, paramName, defaultValue);
	if (_.isNumber(x.value)) {
		x.value = math.unit(x.value, 'l');
	}
	else if (_.isString(x.value)) {
		x.value = math.eval(x.value);
	}
	expect.truthy({paramName: paramName}, math.unit('l').equalBase(x.value), "expected a volume with liter units (l, ul, etc.): "+JSON.stringify(x));
	return x;
}

function lookupWells(params, data, paramName, defaultValue) {
	var x = lookupValue(params, data, paramName, defaultValue);
	if (_.isString(x.value)) {
		x.value = wellsParser.parse(x.value, data.objects);
	}
	expect.truthy({paramName: paramName}, _.isArray(x.value), "expected a list of wells: "+JSON.stringify(x));
	return x;
}

function lookupDuration(params, data, paramName, defaultValue) {
	var x0 = lookupValue(params, data, paramName, defaultValue);
	var x = _.clone(x0);
	if (_.isNumber(x.value)) {
		x.value = math.unit(x.value, 's');
	}
	else if (_.isString(x.value)) {
		x.value = math.eval(x.value);
	}
	//console.log({a: math.unit('s'), value: x.value, x0})
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
	expect.truthy({objectName: value1}, !_.isUndefined(value2), "not found");
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
			return (_.isUndefined(value)) ? undefined : {objectName: value};
		case "Any": return lookupValue(params, data, name, defaultValue);
		case "Number": return lookupNumber(params, data, name, defaultValue);
		case "Object": return lookupObject(params, data, name, defaultValue);
		case "String":  return lookupString(params, data, name, defaultValue);
		case "Duration": return lookupDuration(params, data, name, defaultValue);
		case "Volume": return lookupVolume(params, data, name, defaultValue);
		case "Wells": return lookupWells(params, data, name, defaultValue);
		case "File":
			var filename = params[name];
			var filedata = data.files[filename];
			if (_.isUndefined(filedata))
				filedata = defaultValue;
			if (_.isUndefined(filedata) && _.isUndefined(filename))
				return undefined;
			expect.truthy({paramName: name, objectName: filename}, !_.isUndefined(filedata), "file not loaded: "+filename);
			return {objectName: filename, value: filedata};
	}
	return undefined;
}

/**
 * Parse command parameters.
 *
 * @param  {object} params - the parameters passed to the command
 * @param  {object} data - protocol data
 * @param  {object} specs - description of the expected parameters
 * @return {object} and objects whose keys are the expected parameters and whose
 *  values are `{objectName: ..., value: ...}` objects, or `undefined` if the paramter
 *  is optional and not presents in `params`..
 */
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
		var objectName = (x.objectName) ? x.objectName+"."+propertyName : paramName+"/"+propertyName;
		expect.truthy({objectName: objectName}, !_.isUndefined(value), "missing value");
		return value;
	}
	else {
		var objectName = x.objectName+"."+propertyName;
		var value = g(data, objectName);
		if (_.isUndefined(value))
			value = defaultValue;
		expect.truthy({objectName: objectName}, !_.isUndefined(value), "missing value");
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
	_dereferenceVariable: dereferenceVariable,
	getParameter: get,
	getObjectParameter: getObjectParameter,
	getNumberParameter: getNumberParameter,
	getParsedValue: getParsedValue,
	parseParams: parseParams,
	queryLogic: queryLogic,
	_lookupValue: lookupValue
}
