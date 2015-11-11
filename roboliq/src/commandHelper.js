var _ = require('lodash');
var assert = require('assert');
var expect = require('./expect.js');
var jmespath = require('jmespath');
import math from 'mathjs';
import tv4 from 'tv4';
import roboliqSchemas from './roboliqSchemas.js';
import wellsParser from './parsers/wellsParser.js';

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
 * Try to convert value0 (a "raw" value, no yet looked up) to the type given by type, possibly considering p.
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
 * @return {any} converted value
 */
function processValue0BySchema(value0, schema, data, name) {
	if (_.isUndefined(schema)) {
		return value0;
	}

	if (schema.hasOwnProperty('enum')) {
		return processValue0AsEnum(value0, schema, data, name);
	}

	const type = (_.isUndefined(schema.type) && !_.isEmpty(schema.properties))
		? "object"
		: schema.type;

	if (_.isEmpty(type))
		return value0;

	if (_.isString(type)) {
		return processValue0BySchemaType(value0, schema, type, data, name);
	}
	else {
		// Try each type alternative:
		const types = _.flatten([schema.type]);
		let es = [];
		for (const t of types) {
			try {
				return processValue0BySchemaType(value0, schema, t, data, name);
			}
			catch (e) {
				es.push(e);
			}
		}

		if (!_.isEmpty(es))
			throw es[0];
	}

	return undefined;
}

function processValue0AsEnum(value0, schema, data, name) {
	const value1 = lookupValue0(value0, data);
	expect.truthy({paramName: name}, schema.enum.includes(value1), "expected one of "+schema.enum+": "+JSON.stringify(value0));
	return value1;
}

function processValue0BySchemaType(value0, schema, type, data, name) {
	if (type === 'name')
		return value0;

	const value = lookupValue0(value0, data);
	switch (type) {
		case "array": return processValueAsArray(value, schema.items, data, name);
		case "boolean":
			expect.truthy({paramName: name}, _.isBoolean(value), "expected boolean: "+value);
			return value;
		case "integer":
			expect.truthy({paramName: name}, _.isNumber(value) && (value % 1) === 0, "expected integer: "+value);
			return value;
		case "number":
			expect.truthy({paramName: name}, _.isNumber(value), "expected number: "+value);
			return value;
		case "null":
			expect.truthy({paramName: name}, _.isNull(value), "expected null: "+value);
			return value;
		case "string": return processString(value, data, name);
		case "object": return processValueAsObject(value, data, schema);
		case "Agent":
			// TODO: need to check a list of which types are Agent types
			expect.truthy({paramName: name}, _.isPlainObject(value), "expected object: "+value);
			return value;
		case "Any": return value;
		case "Duration": return processDuration(value, data, name);
		case "Equipment":
			// TODO: need to check a list of which types are Equipment types
			expect.truthy({paramName: name}, _.isPlainObject(value), "expected object: "+value);
			return value;
		case "Plate": return processObjectOfType(value, data, name, type);
		case "Site": return processObjectOfType(value, data, name, type);
		case "Source": return processSource(value, data, name);
		case "Sources": return processSources(value, data, name);
		case "String": return processString(value, data, name);
		case "Volume": return processVolume(value, data, name);
		case "Volumes": return processOneOrArray(value, data, name, (x) => processVolume(x, data, name));
		case "Well": return processWell(value, data, name);
		case "Wells": return processWells(value, data, name);
		case "File":
			var filename = value;
			var filedata = data.files[filename];
			if (_.isUndefined(filedata))
				filedata = defaultValue;
			if (_.isUndefined(filedata) && _.isUndefined(filename))
				return undefined;
			expect.truthy({paramName: name, objectName: filename}, !_.isUndefined(filedata), "file not loaded: "+filename);
			//console.log({filedata})
			return filedata;
		default: {
			if (data.schemas.hasOwnProperty(type)) {
				const spec = data.schemas[type];
				//console.log({type, spec})
				const parsed = processValue0BySchema(value, spec, data, name);
				//console.log({type, parsed})
				return parsed;
			}
			else {
				const schema = roboliqSchemas[type];
				expect.truthy({paramName: name}, schema, "unknown type: "+type);
				const isValid = tv4.validate(value, schema);
				expect.truthy({paramName: name}, isValid, tv4.toString());
				return value;
			}
		}
	}
	return undefined;
	if (t === 'array') {
		result = processValueAsArray(value, schema.items, data, name);
	}
	else if (t === 'object' || !_.isEmpty(schema.properties)) {
		//console.log(JSON.stringify({t, value, schema}, null, '\t'))
		result = processValueAsObject(value, data, schema);
	}
	else {
		result = processValueByType(value, t, data, name);
	}

}

/**
 * Parse command parameters.
 *
 * @param  {object} params - the parameters passed to the command
 * @param  {object} data - protocol data
 * @param  {CommandSpec} specs - description of the expected parameters
 * @return {object} and objects whose keys are the expected parameters and whose
 *  values are `{objectName: ..., value: ...}` objects, or `undefined` if the paramter
 *  is optional and not presents in `params`..
 */
function processValueAsObject(params, data, specs) {
	//console.log(`processValueAsObject: ${JSON.stringify(params)} ${JSON.stringify(specs)}`)
	const required_l = specs.required || [];
	const l0 = _.pairs(specs.properties);
	// If no properties are specified, return the original parameters
	if (l0.length === 0) {
		return params;
	}
	// Otherwise, convert the parameters
	const l1 = l0.map(([propertyName, p]) => {
		const type = p.type;
		const required = required_l.includes(propertyName);
		const defaultValue = p.default;

		let info;
		if (type === 'name') {
			info = {objectName: _.get(params, propertyName, defaultValue)};
			// If not optional, require the variable's presence:
			if (required) {
				//console.log({propertyName, type, info, params})
				expect.truthy({propertyName}, !_.isUndefined(info.objectName), "missing required value");
			}
		}
		else {
			info = lookupValue(params, data, propertyName, defaultValue);
			if (info.value) {
				info.value = processValue0BySchema(info.value, p, data, propertyName);
				//console.log({propertyName, type, info})
				//console.log({value: info.value})
				//console.trace();
			}
			// If not optional, require the variable's presence:
			if (required) {
				//console.log({propertyName, type, info, params})
				expect.truthy({propertyName}, !_.isUndefined(info.value), "missing required value");
			}
		}

		return [propertyName, _.omit(info, _.isUndefined)];
	});

	const o = _.zipObject(_.compact(l1));
	return o;
}

function processValueAsArray(list0, schema, data, name) {
	expect.truthy({paramName: name}, _.isArray(list0), "expected an array: "+list0);
	//console.log({t2})
	const list1 = list0.map((x, index) => {
		//return processValueByType(x, t2, data, `${name}[${index}]`);
		const x2 = processValue0BySchema(x, schema, data, `${name}[${index}]`);
		//console.log({x, t2, x2})
		return x2;
	});
	//console.log({list1})
	return list1;
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
 *
 * @param  {any} value0 - The value from the user.
 * @param  {object} data - The data object.
 * @return {any} A new value, if value0 referred to something in data.objects.
 */
function lookupValue0(value0, data) {
	if (_.isString(value0) && !_.startsWith(value0, '"')) {
		const deref = dereferenceVariable(data, value0);
		if (deref) {
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

function processOneOrArray(value0, data, name, fn) {
	//console.log({value0, name})
	try {
		return [fn(value0)];
	} catch (e) {}

	expect.truthy({paramName: name}, _.isArray(value0), "expected an array: "+JSON.stringify(value0));
	return value0.map(x => fn(x));
}

function processString(value0, data, paramName) {
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

	return value1.toString();
}

function processObjectOfType(x, data, paramName, type) {
	expect.truthy({paramName}, _.isPlainObject(x), `expected an object of type ${type}: `+JSON.stringify(x));
	expect.truthy({paramName}, _.get(x, 'type') === type, `expected an object of type ${type}: `+JSON.stringify(x));
	return x;
}

function processSource(x, data, paramName) {
	const l = processSources(x, data, paramName);
	expect.truthy({paramName: paramName}, _.isArray(l) && l.length === 1, "expected a single liquid source: "+JSON.stringify(x));
	return l[0];
}

function processSources(x, data, paramName) {
	//console.log({before: x, paramName})
	if (_.isString(x)) {
		x = wellsParser.parse(x, data.objects);
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
				return processSource(x2, data, paramName2);
			});
		});
	}
	//console.log({after: x})
	return x;
}

function processVolume(x, data, paramName) {
	if (_.isNumber(x)) {
		x = math.unit(x, 'l');
	}
	else if (_.isString(x)) {
		x = math.eval(x);
	}
	expect.truthy({paramName: paramName}, math.unit('l').equalBase(x), "expected a volume with liter units (l, ul, etc.): "+JSON.stringify(x));
	return x;
}

function processWell(x, data, paramName) {
	if (_.isString(x)) {
		x = wellsParser.parse(x, data.objects);
	}
	expect.truthy({paramName: paramName}, _.isArray(x) && x.length === 1, "expected a single well indicator: "+JSON.stringify(x));
	return x[0];
}

function processWells(x, data, paramName) {
	if (_.isString(x)) {
		x = wellsParser.parse(x, data.objects);
	}
	expect.truthy({paramName: paramName}, _.isArray(x), "expected a list of wells: "+JSON.stringify(x));
	return x;
}

function processDuration(x0, data, paramName) {
	let x = x0;
	if (_.isNumber(x)) {
		x = math.unit(x, 's');
	}
	else if (_.isString(x)) {
		x = math.eval(x);
	}
	//console.log({a: math.unit('s'), value: x, x0})
	expect.truthy({paramName: paramName}, math.unit('s').equalBase(x), "expected a value with time units (s, second, seconds, minute, minutes, h, hour, hours, day, days): "+JSON.stringify(x0));
	return x;
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
	_dereferenceVariable: dereferenceVariable,
	getParsedValue,
	parseParams: processValueAsObject,
	queryLogic,
	_lookupValue: lookupValue
}
