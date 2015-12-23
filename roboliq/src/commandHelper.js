var _ = require('lodash');
var assert = require('assert');
var expect = require('./expect.js');
var jmespath = require('jmespath');
import math from 'mathjs';
import tv4 from 'tv4';
import roboliqSchemas from './roboliqSchemas.js';
import wellsParser from './parsers/wellsParser.js';

/**
 * Ensure that the value is an array.
 * If the value is already an array, return it directly.
 * If the value is undefined, return an empty array.
 * Otherwise, return the value wrapped in an array.
 *
 * @param  {any} x - value
 * @return {array} an array
 */
function asArray(x) {
	if (_.isArray(x)) return x;
	else if (_.isUndefined(x)) return [];
	else return [x];
}

/**
 * Parse command parameters.
 *
 * @param  {object} params - the parameters passed to the command
 * @param  {object} data - protocol data
 * @param  {object} schemas - JSON Schema description, with roboliq extensions
 * @return {object} and objects whose keys are the expected parameters and whose
 *  values are `{objectName: ..., value: ...}` objects, or `undefined` if the paramter
 *  is optional and not presents in `params`..
 */
function parseParams(params, data, schema) {
	return processValueAsObject({value: {}, objectName: {}}, [], params, data, schema);
}

/**
 * processValueBySchema():
 * Input: value and a schema.
 *
 *
 * ---
 *
 * processValueBySchemaAndType():
 * If type === "name", return value.
 * If _.isString(value), try to lookup object.
 *
 * If type === "array"
 */

/**
 * Try to convert value0 (a "raw" value, no yet looked up) to the given type.
 *
 * - If schema is undefined, return value.
 *
 * - If schema.enum: return processValue0AsEnum()
 *
 * - If schema.type is undefined but there are schema.properties, assume schema.type = "object".
 *
 * - If type is undefined or empty, return value.
 *
 * - If type is an array, try processing for each element of the array
 *
 * @param  {any} value0 - initial value
 * @param  {string} type - type to convert to
 * @param  {object} data
 * @param  {string} name - parameter name used to lookup value0
 * @param  {object} schema - property schema information (e.g. for arrays)
 * @return nothing of interest
 */
function processValue0BySchema(result, path, value0, schema, data, name) {
	//console.log(`processValue0BySchema(${path.join('.')}, ${value0})`)
	if (_.isUndefined(schema)) {
		return _.set(result.value, path, value0);
	}

	if (schema.hasOwnProperty('enum')) {
		return processValue0AsEnum(result, path, value0, schema, data, name);
	}

	const type = (_.isUndefined(schema.type) && !_.isEmpty(schema.properties))
		? "object"
		: schema.type;

	if (_.isEmpty(type)) {
		return _.set(result.value, path, value0);
	}

	if (_.isString(type)) {
		return processValue0BySchemaType(result, path, value0, schema, type, data, name);
	}
	else {
		// Try each type alternative:
		const types = _.flatten([schema.type]);
		let es = [];
		for (const t of types) {
			try {
				return processValue0BySchemaType(result, path, value0, schema, t, data, name);
			}
			catch (e) {
				es.push(e);
			}
		}

		if (!_.isEmpty(es))
			throw es[0];
	}
}

function processValue0AsEnum(result, path, value0, schema, data, name) {
	const value1 = lookupValue0(result, path, value0, data);
	expect.truthy({paramName: name}, schema.enum.includes(value1), "expected one of "+schema.enum+": "+JSON.stringify(value0));
	_.set(result.value, path, value1);
}

function processValue0BySchemaType(result, path, value0, schema, type, data, name) {
	//console.log(`processValue0BySchemaType(${path.join('.')}, ${value0}, ${type})`)
	if (type === 'name') {
		return 	_.set(result.value, path, value0);
	}

	const value = lookupValue0(result, path, value0, data);
	// By default, set result.value@path = value
	_.set(result.value, path, value);

	switch (type) {
		case "array": return processValueAsArray(result, path, value, schema.items, data, name);
		case "boolean":
			expect.truthy({paramName: name}, _.isBoolean(value), "expected boolean: "+value);
			return;
		case "integer":
			expect.truthy({paramName: name}, _.isNumber(value) && (value % 1) === 0, "expected integer: "+value);
			return;
		case "number":
			expect.truthy({paramName: name}, _.isNumber(value), "expected number: "+value);
			return;
		case "null":
			expect.truthy({paramName: name}, _.isNull(value), "expected null: "+value);
			return;
		case "string": return processString(result, path, value, data, name);
		case "object": return processValueAsObject(result, path, value, data, schema);
		case "Agent":
			// TODO: need to check a list of which types are Agent types
			expect.truthy({paramName: name}, _.isPlainObject(value), "expected object: "+value);
			return;
		case "Any": return;
		case "Duration": return processDuration(result, path, value, data, name);
		case "Equipment":
			// TODO: need to check a list of which types are Equipment types
			expect.truthy({paramName: name}, _.isPlainObject(value), "expected object: "+value);
			return;
		case "Plate": return processObjectOfType(result, path, value, data, name, type);
		case "Site": return processObjectOfType(result, path, value, data, name, type);
		case "Source": return processSource(result, path, value, data, name);
		case "Sources": return processSources(result, path, value, data, name);
		case "String": return processString(result, path, value, data, name);
		case "Volume": return processVolume(result, path, value, data, name);
		case "Volumes": return processOneOrArray(path, value, (x, i) => processVolume(result, path.concat(i), x, data, name));
		case "Well": return processWell(result, path, value, data, name);
		case "Wells": return processWells(result, path, value, data, name);
		case "File":
			var filename = value;
			var filedata = data.files[filename];
			if (_.isUndefined(filedata))
				filedata = defaultValue;
			if (_.isUndefined(filedata) && _.isUndefined(filename))
				return;
			expect.truthy({paramName: name, objectName: filename}, !_.isUndefined(filedata), "file not loaded: "+filename);
			//console.log({filedata})
			result.objectName[path.join('.')] = filename;
			_.set(result.value, path, filedata);
			return;
		default: {
			if (data.schemas.hasOwnProperty(type)) {
				const schema = data.schemas[type];
				//console.log({type, schema})
				processValue0BySchema(result, path, value, schema, data, name);
				//console.log({type, parsed})
				return;
			}
			else {
				const schema = roboliqSchemas[type];
				expect.truthy({paramName: name}, schema, "unknown type: "+type);
				const isValid = tv4.validate(value, schema);
				expect.truthy({paramName: name}, isValid, tv4.toString());
				return;
			}
		}
	}
}

/**
 * Parse command parameters.
 *
 * @param  {object} result - the resulting object to return, containing objectName and value representations of params.
 * @param  {array}  path - path in the original params object
 * @param  {object} params - the part of the original parameters refered to by `path`
 * @param  {object} data - protocol data
 * @param  {object} schemas - JSON Schema description, with roboliq extensions
 * @return {object} and objects whose keys are the expected parameters and whose
 *  values are `{objectName: ..., value: ...}` objects, or `undefined` if the paramter
 *  is optional and not presents in `params`..
 */
function processValueAsObject(result, path, params, data, schema) {
	//console.log(`processValueAsObject: ${JSON.stringify(params)} ${JSON.stringify(schema)}`)
	const required_l = schema.required || [];
	const l0 = _.pairs(schema.properties);
	// If no properties are schemafied, return the original parameters
	if (l0.length === 0) {
		_.set(result.value, path, params);
		return result;
	}
	// Otherwise, convert the parameters
	for (const [propertyName, p] of l0) {
		const type = p.type;
		const required = required_l.includes(propertyName);
		const defaultValue = p.default;
		const path1 = path.concat(propertyName);

		if (type === 'name') {
			const value1 = _.get(params, propertyName, defaultValue);
			if (!_.isUndefined(value1)) {
				_.set(result.value, path1, value1);
			}
			// If not optional, require the variable's presence:
			if (required) {
				//console.log({propertyName, type, info, params})
				expect.truthy({propertyName: path1.join('.')}, !_.isUndefined(value1), "missing required value");
			}
		}
		else {
			const info = lookupValue(params, data, propertyName, defaultValue);
			if (info.value) {
				processValue0BySchema(result, path1, info.value, p, data, propertyName);
				//console.log({propertyName, type, info})
				//console.log({value: info.value})
				//console.trace();
				//_.set(result.value, path1, info.value);
				if (info.objectName) {
					result.objectName[path1.join('.')] = info.objectName;
				}
			}
			// If not optional, require the variable's presence:
			if (required) {
				//console.log({propertyName, type, info, params})
				expect.truthy({propertyName}, !_.isUndefined(info.value), "missing required value");
			}
		}
	}

	return result;
}

function processValueAsArray(result, path, list0, schema, data, name) {
	//console.log(`processValueAsArray(${path}, ${list0})`)
	expect.truthy({paramName: name}, _.isArray(list0), "expected an array: "+list0);
	//console.log({t2})
	list0.forEach((x, index) => {
		//return processValueByType(x, t2, data, `${name}[${index}]`);
		processValue0BySchema(result, path.concat(index), x, schema, data, `${name}[${index}]`);
		//console.log({x, t2, x2})
		//return x2;
	});
	//console.log({list1})
	//return list1;
}

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

/**
 * Try to lookup value0 in objects set.
 * This function is recursive, insofar as if the value refers to a variable,
 * the variables value will also be dereferenced.
 * When a variable is looked up, its also added to result.objectName[path].
 *
 * @param  {any} value0 - The value from the user.
 * @param  {object} data - The data object.
 * @return {any} A new value, if value0 referred to something in data.objects.
 */
function lookupValue0(result, path, value0, data) {
	if (_.isString(value0) && !_.startsWith(value0, '"')) {
		const deref = dereferenceVariable(data, value0);
		if (deref) {
			result.objectName[path.join('.')] = deref.objectName;
			return deref.value;
		}
	}

	return value0;
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

	return _.merge({}, result);
}

/**
 * Recursively lookup variable by name and return the final value.
 */
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

/**
 * Try to call fn on value0.  If that works, return the value is be made into
 * a singleton array.  Otherwise try to process value0 as an array.
 */
function processOneOrArray(path, value0, fn) {
	//console.log({value0, name})
	try {
		fn(value0, 0);
	} catch (e) {}

	expect.truthy({paramName: path.join('.')}, _.isArray(value0), "expected an array: "+JSON.stringify(value0));
	value0.forEach((x, i) => fn(x, i));
}

function processString(result, path, value0, data, paramName) {
	// Follow de-references:
	var references = [];
	var objectName = undefined;
	let value1 = value0;
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

	_.set(result.value, path, value1.toString());
}

function processObjectOfType(result, path, x, data, paramName, type) {
	expect.truthy({paramName}, _.isPlainObject(x), `expected an object of type ${type}: `+JSON.stringify(x));
	expect.truthy({paramName}, _.get(x, 'type') === type, `expected an object of type ${type}: `+JSON.stringify(x));
}

function processSource(result, path, x, data, paramName) {
	const l = processSources(result, path, x, data, paramName);
	expect.truthy({paramName: paramName}, _.isArray(l) && l.length === 1, "expected a single liquid source: "+JSON.stringify(x));
	_.set(result.value, path, l[0]);
}

/**
 * @returns list of sources
 */
function processSources(result, path, x, data, paramName) {
	//console.log({before: x, paramName})
	if (_.isString(x)) {
		x = wellsParser.parse(x, data.objects);
		//console.log({x})
		expect.truthy({paramName: paramName}, _.isArray(x), "expected a liquid source: "+JSON.stringify(x));
		//x = [x];
	}
	else if (_.isPlainObject(x) && x.type === 'Liquid') {
		x = [x.wells];
	}
	else if (_.isArray(x)) {
		x = x.map(x2 => {
			const paramName2 = `$paramName[$index]`;
			return expect.try({paramName: paramName2}, () => {
				const result2 = {value: {}, objectName: {}};
				processSource(result2, ['x'], x2, data, paramName2);
				return result2.value.x;
			});
		});
	}
	_.set(result.value, path, x);
	return x;
}

function processVolume(result, path, x, data, paramName) {
	if (_.isNumber(x)) {
		x = math.unit(x, 'l');
	}
	else if (_.isString(x)) {
		x = math.eval(x);
	}
	expect.truthy({paramName: paramName}, math.unit('l').equalBase(x), "expected a volume with liter units (l, ul, etc.): "+JSON.stringify(x));
	_.set(result.value, path, x);
}

function processWell(result, path, x, data, paramName) {
	if (_.isString(x)) {
		//console.log("processWell:")
		//console.log({result, path, x, paramName})
		x = wellsParser.parse(x, data.objects);
	}
	expect.truthy({paramName: paramName}, _.isArray(x) && x.length === 1, "expected a single well indicator: "+JSON.stringify(x));
	_.set(result.value, path, x[0]);
}

function processWells(result, path, x, data, paramName) {
	if (_.isString(x)) {
		x = wellsParser.parse(x, data.objects);
	}
	expect.truthy({paramName: paramName}, _.isArray(x), "expected a list of wells: "+JSON.stringify(x));
	_.set(result.value, path, x);
}

function processDuration(result, path, x0, data, paramName) {
	let x = x0;
	if (_.isNumber(x)) {
		x = math.unit(x, 's');
	}
	else if (_.isString(x)) {
		x = math.eval(x);
	}
	//console.log({a: math.unit('s'), value: x, x0})
	expect.truthy({paramName: paramName}, math.unit('s').equalBase(x), "expected a value with time units (s, second, seconds, minute, minutes, h, hour, hours, day, days): "+JSON.stringify(x0));
	_.set(result.value, path, x);
}

function getParsedValue(parsed, data, paramName, propertyName, defaultValue) {
	//console.log({parsed, x: parsed[paramName], paramName, propertyName})
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
	asArray,
	_dereferenceVariable: dereferenceVariable,
	getParsedValue,
	parseParams,
	queryLogic,
	_lookupValue: lookupValue
}
