/**
 * A collection of helper functions for command handlers.
 * @module commandHelper
 */

var _ = require('lodash');
var assert = require('assert');
var expect = require('./expect.js');
var jmespath = require('jmespath');
import math from 'mathjs';
import naturalSort from 'javascript-natural-sort';
import tv4 from 'tv4';
const Design = require('./design2.js');
import misc from './misc.js';
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
 * Create the 'data' object that gets passed into many commandHelper functions.
 * @param  {Protocol} protocol
 * @param  {object} objects  =             {} - current objects
 * @param  {object} SCOPE    =             {} - current SCOPE
 * @param  {array} DATA     =             [] - current DATA table
 * @param  {array} path = [] - current processing path (usually a step ID, e.g. step 1.2 would be given by `[1, 2]`)
 * @param  {object} files = {} - map of filename to loaded filedata
 * @return {object} the 'data' object that gets passed into many commandHelper functions
 */
function createData(protocol, objects = {}, SCOPE = {}, DATA = [], path = [], files = {}) {
	const common = Design.getCommonValues(DATA);
	const SCOPE2 = _.defaults(common, SCOPE);

	// Process any directives in this step
	const objects2 = _.clone(objects);
	// TODO: consider changing this so that DATA and SCOPE are not a part of `objects`,
	// but are their own separate properties.
	objects2.DATA = DATA;
	objects2.SCOPE = SCOPE2;
	const data = {
		objects: objects2,
		schemas: protocol.schemas,
		accesses: [],
		files,
		protocol,
		path
	};
	return data;
}

/**
 * Recursively replace $-SCOPE, $$-DATA, and template strings in `x`.
 *
 * The recursion has the following exceptions:
 * - skip objects with any of these properties: `data`, `@DATA`, `@SCOPE`
 * - skip `steps` properties
 * - skip directives
 *
 * @param  {any} x - the variable to perform substitutions on
 * @param  {object} data - protocol data
 * @return {any} the value with possible substitutions
 */
function substituteDeep(x, data, SCOPE, DATA) {
	// console.log("substituteDeep: "); console.log({x, SCOPE, DATA, x})
	let x2 = x;
	if (_.isString(x)) {
		// DATA substitution
		if (_.startsWith(x, "$$")) {
			if (_.isArray(DATA)) {
				const propertyName = x.substr(2);
				x2 = _(DATA).map(propertyName).filter(x => !_.isUndefined(x)).value();
				// console.log("DATA: "+JSON.stringify(DATA, null, '\t'));
				// console.log({map: _(DATA).map(propertyName).value()});
			}
		}
		// SCOPE substitution
		else if (_.startsWith(x, "$")) {
			const propertyName = x.substr(1);
			x2 = _.get(SCOPE, propertyName, x);
		}
		// Template substitution
		else if (_.startsWith(x, "`") && _.endsWith(x, "`")) {
			const template = x.substr(1, x.length - 2);
			const scope = _.mapKeys(SCOPE, (value, name) => "$"+name);
			//console.log({x, template, scope})
			x2 = misc.renderTemplate(template, scope, data);
		}
	}
	else if (_.isArray(x)) {
		x2 = _.map(x, y => substituteDeep(y, data, SCOPE, DATA));
	}
	else if (_.isPlainObject(x)) {
		const {DATAs, SCOPEs, foreach} = updateSCOPEDATA(x, data, SCOPE, DATA);

		// Skip objects with one of these properties:
		if (foreach) {
			// TODO: handle this case?
		}
		else {
			const DATA2 = DATAs[0];
			const SCOPE2 = SCOPEs[0];
			x2 = _.mapValues(x, (value, name) => {
				// Skip over @DATA, @SCOPE, directives and 'steps' properties
				if (_.startsWith(name, "#") || name === "data" || name === "@DATA" || name === "@SCOPE" || name === "steps") {
					return value;
				}
				else {
					return substituteDeep(value, data, SCOPE2, DATA2);
				}
			});
		}
	}
	return x2;
}

/**
 * Parse command parameters according to a schema.
 *
 * If parsing fails, an exception will be thrown.
 *
 * Otherwise, the returned result contains two properties: `value` and `objectName`.
 * Both properties are maps that reflect the structure of the given schema.
 * The `value` map contains the parsed values -- object references are replaced
 * by the actual object (in `data`), quantities are replaced by mathjs objects,
 * well specifications are replaced by an array of well references, etc.
 *
 * The `objectName` map contains any object names that were referenced;
 * in contrast to the `value` map (which is a tree of properties like `params`),
 * `objectName` is a flat map, where the keys are string representations of the
 * object paths (separated by '.').
 * Any object names that were looked up will also be added to the `data.accesses`
 * list.
 *
 * @param  {object} params - the parameters passed to the command
 * @param  {object} data - protocol data
 * @param  {object} schema - JSON Schema description, with roboliq type extensions
 * @return {object} the parsed parameters, if successfully parsed.
 */
function parseParams(params, data, schema) {
	//console.log("SCOPE: "+JSON.stringify(data.objects.SCOPE, null, '\t'))
	const result = {orig: params, value: {}, objectName: {}, unknown: []};
	processParamsBySchema(result, [], params, schema, data);
	if (_.isEmpty(result.unknown)) {
		delete result.unknown;
	}
	return result;
}

/**
 * Try to process the given params with the given schema.
 *
 * Updates the `result` object.
 * Updates `data.accesses` if object lookups are performed.
 *
 * @param {object} result - the resulting object to return, containing objectName and value representations of params.
 * @param {array} path - path in the original params object
 * @param {object} params - the part of the original parameters refered to by `path`
 * @param {object} schema - JSON Schema description, with roboliq extensions
 * @param {object} data - protocol data
 */
function processParamsBySchema(result, path, params, schema, data) {
	//console.log(`processParamsBySchema: ${JSON.stringify(params)} ${JSON.stringify(schema)}`)
	const required_l = schema.required || [];
	const l0 = _.toPairs(schema.properties);
	// If no properties are schemafied, return the original parameters
	if (l0.length === 0) {
		_.set(result.value, path, params);
		return result;
	}
	// Otherwise, convert the parameters
	for (const [propertyName, p] of l0) {
		const type = p.type;
		const required = _.includes(required_l, propertyName);
		const defaultValue = p.default;
		const path1 = path.concat(propertyName);
		const value0 = _.cloneDeep(_.get(params, propertyName, defaultValue));

		if (type === "name") {
			// Normally, we don't want to process "name" parameters at all, but we
			// still need to dereference "$"-scope variables
			const value1 = (_.startsWith(value0, "$"))
				? _.get(data.objects.SCOPE, value0.substring(1), value0)
				: value0;
			if (!_.isUndefined(value1)) {
				_.set(result.value, path1, value1);
			}
			// If not optional, require the variable's presence:
			if (required) {
				//console.log({propertyName, type, info, params})
				expect.truthy({paramName: propertyName}, !_.isUndefined(value1), "missing required value [CODE 95]");
			}
		}
		else {
			const value1 = _.clone(lookupValue0(result, path1, value0, data));
			if (!_.isUndefined(value1)) {
				processValue0BySchema(result, path1, value1, p, data, propertyName);
			}
			// If not optional, require the variable's presence:
			else if (required) {
				//console.log({propertyName, type, info, params})
				expect.truthy({paramName: path1.join(".")}, false, "missing required value [CODE 106]");
			}
		}
	}

	return result;
}

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
 * @param {object} result - the resulting object to return, containing objectName and value representations of params.
 * @param {array} path - path in the original params object
 * @param {any} value0 - the value to process
 * @param {object} schema - JSON Schema description, with roboliq extensions
 * @param {object} data - protocol data
 */
function processValue0BySchema(result, path, value0, schema, data) {
	//console.log(`processValue0BySchema(${path.join('.')}, ${JSON.stringify(value0)})`)
	//const valuePre = _.cloneDeep(value0);
	if (_.isUndefined(schema)) {
		_.set(result.value, path, value0);
	}

	else if (schema.hasOwnProperty('enum')) {
		return processValue0AsEnum(result, path, value0, schema, data);
	}

	else {
		const type = (_.isUndefined(schema.type) && !_.isEmpty(schema.properties))
			? "object"
			: schema.type;

		if (_.isEmpty(type)) {
			_.set(result.value, path, value0);
		}

		else if (_.isString(type)) {
			processValue0BySchemaType(result, path, value0, schema, type, data);
		}

		// Otherwise, we should have an array of types
		else {
			// Try each type alternative:
			const types = _.flatten([schema.type]);
			// console.log({types})
			let es = [];
			for (const t of types) {
				try {
					return processValue0BySchemaType(result, path, value0, schema, t, data);
				}
				catch (e) {
					es.push(e);
				}
			}

			if (!_.isEmpty(es))
				throw es[0];
		}
	}
	/*if (!_.isEqual(value0, valuePre)) {
		console.log("VALUE CHANGED");
		throw "error";
	}*/
}

/**
 * Try to process the value as an enum.
 * @param  {object} result - result structure for values and objectNames
 * @param  {array} path - path in params
 * @param  {any} value0 - the value to process
 * @param  {object} schema - schema
 * @param  {object} data - protocol data
 */
function processValue0AsEnum(result, path, value0, schema, data) {
	const value1 = lookupValue0(result, path, value0, data);
	expect.truthy({paramName: path.join(".")}, _.includes(schema.enum, value1), "expected one of "+schema.enum+": "+JSON.stringify(value0));
	_.set(result.value, path, value1);
}

/**
 * A sub-function of processValue0BySchema().
 * Try to process the value as a named type.
 * @param  {object} result - result structure for values and objectNames
 * @param  {array} path - path in params
 * @param  {any} value0 - the value to process
 * @param  {object} schema - schema
 * @param  {object} data - protocol data
 */
function processValue0BySchemaType(result, path, value0, schema, type, data) {
	// console.log(`processValue0BySchemaType(${path.join('.')}, ${value0}, ${type})`)
	if (type === 'name') {
		return 	_.set(result.value, path, value0);
	}

	const value = _.cloneDeep(lookupValue0(result, path, value0, data));
	// By default, set result.value@path = value
	_.set(result.value, path, value);

	const name = path.join(".");

	switch (type) {
		case "array": return processValueAsArray(result, path, value, schema.items, data);
		case "boolean":
			expect.truthy({paramName: name}, _.isBoolean(value), "expected boolean: "+value);
			return;
		case "integer":
			expect.truthy({paramName: name}, _.isNumber(value) && (value % 1) === 0, "expected integer: "+value);
			return;
		case "number":
			expect.truthy({paramName: name}, _.isNumber(value), "expected number: "+JSON.stringify(value));
			return;
		case "null":
			expect.truthy({paramName: name}, _.isNull(value), "expected null: "+value);
			return;
		case "string": return processString(result, path, value, data);
		case "object": return processParamsBySchema(result, path, value, schema, data);
		case "Agent":
			// TODO: need to check a list of which types are Agent types
			expect.truthy({paramName: name}, _.isPlainObject(value), "expected object: "+value);
			return;
		case "Any": return;
		case "Duration": return processDuration(result, path, value, data);
		case "Equipment":
			// TODO: need to check a list of which types are Equipment types
			expect.truthy({paramName: name}, _.isPlainObject(value), "expected object: "+value);
			return;
		case "Plate": return processObjectOfType(result, path, value, data, type);
		case "Site": return processObjectOfType(result, path, value, data, type);
		case "SiteOrStay": return processSiteOrStay(result, path, value, data);
		case "Source": return processSource(result, path, value, data);
		case "Sources": return processSources(result, path, value, data);
		case "String": return processString(result, path, value, data);
		case "Temperature": return processTemperature(result, path, value, data);
		case "Temperatures": return processOneOrArray(result, path, value, (result, path, x) => processTemperature(result, path, x, data));
		case "Volume": return processVolume(result, path, value, data);
		case "Volumes": return processOneOrArray(result, path, value, (result, path, x) => processVolume(result, path, x, data));
		case "Well": return processWell(result, path, value, data);
		case "Wells": return processWells(result, path, value, data);
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
				// console.log({type, schema})
				processValue0BySchema(result, path, value, schema, data);
				// console.log("result: "+JSON.stringify(result, null, '\t'))
				return;
			}
			else {
				const schema = roboliqSchemas[type];
				if (!schema) console.log("known types: "+_.keys(data.schemas).concat(_.keys(roboliqSchemas)))
				expect.truthy({paramName: name}, schema, "unknown type: "+JSON.stringify(type));
				const isValid = tv4.validate(value, schema);
				expect.truthy({paramName: name}, isValid, tv4.toString());
				return;
			}
		}
	}
}

/**
 * Try to process a value as an array.
 * @param  {object} result - result structure for values and objectNames
 * @param  {array} path - path in params
 * @param  {any} value0 - the value to process
 * @param  {object} schema - schema of the array items
 * @param  {object} data - protocol data
 */
function processValueAsArray(result, path, list0, schema, data) {
	//console.log(`processValueAsArray(${path}, ${list0})`)
	expect.truthy({paramName: path.join(".")}, _.isArray(list0), "expected an array: "+list0);
	//console.log({t2})
	list0.forEach((x, index) => {
		//return processValueByType(x, t2, data, `${name}[${index}]`);
		processValue0BySchema(result, path.concat(index), x, schema, data);
		//console.log({x, t2, x2})
		//return x2;
	});
	//console.log({list1})
	//return list1;
}

/**
 * Try to get a value from data.objects with the given name.
 * @param  {object} data - Data object with 'objects' property
 * @param  {array|string} path - Name of the object value to lookup
 * @param  {any} dflt - default value to return
 * @return {Any} The value at the given path, if any
 */
function g(data, path, dflt) {
	const name = (_.isArray(path)) ? path.join('.') : path;

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
 * @param {object} result - the resulting object to return, containing objectName and value representations of params.
 * @param {array} path - path in the original params object
 * @param {object} data - protocol data
 * @param {any} value0 - The value from the user.
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

/**
 * Recursively lookup variable by name or path and return the final value.
 * @param {object} data - protocol data
 * @param {string} name - name or path of object to lookup in `data.objects`
 * @return {any} result of the lookup, if successful; otherwise undefined.
 */
function dereferenceVariable(data, name) {
	const result = {};

	// Query DATA
	if (_.startsWith(name, "$$")) {
		if (_.isArray(data.objects.DATA)) {
			const propertyName = name.substr(2);
			result.value = _(data.objects.DATA).map(propertyName).filter(x => !_.isUndefined(x)).value();
			//console.log("data.objects.DATA: "+JSON.stringify(data.objects.DATA, null, '\t'));
			//console.log({map: _(data.objects.DATA).map(propertyName).value()});

			const accessName = "DATA."+propertyName;
			if (_.isArray(data.accesses))
				data.accesses.push(accessName);
			else
				data.accesses = [accessName];
		}
	}
	else {
		// Handle SCOPE abbreviation
		if (_.startsWith(name, "$"))
			name = "SCOPE."+name.substr(1);

		while (_.has(data.objects, name)) {
			const value = g(data, name);
			if (!_.startsWith(name, "SCOPE.") && !_.startsWith(name, "DATA.")) {
				result.objectName = name;
			}
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
	}
	return (_.isEmpty(result)) ? undefined : result;
}

/**
 * Try to call fn on value0.  If that works, return the value is be made into
 * a singleton array.  Otherwise try to process value0 as an array.
 * fn should accept parameters (result, path, value0) and set the value in
 * result.value at the given path.

 * @param {object} result - the resulting object to return, containing objectName and value representations of params.
 * @param {array} path - path in the original params object
*/
function processOneOrArray(result, path, value0, fn) {
	//console.log("processOneOrArray:")
	//console.log({path, value0})
	// Try to process value0 as a single value, then turn it into an array
	try {
		_.set(result.value, path, undefined);
		const path2 = path.concat(0);
		fn(result, path2, value0);
		//console.log(JSON.stringify(result));
		//console.log(JSON.stringify(_.get(result.value, path)));
		const x = _.get(result.value, path2);
		//console.log({path2, x: JSON.stringify(x)})
		_.set(result.value, path2, x);
		//console.log({result})
		return;
	} catch (e) {
		//console.log(e)
	}

	expect.truthy({paramName: path.join('.')}, _.isArray(value0), "expected an array: "+JSON.stringify(value0));
	value0.forEach((x, i) => fn(result, path.concat(i), x));
}

/**
 * Try to process a value as a string.
 *
 * @param {object} result - the resulting object to return, containing objectName and value representations of params.
 * @param {array} path - path in the original params object
 * @param {object} params - the part of the original parameters refered to by `path`
 * @param {object} data - protocol data
 */
function processString(result, path, value0, data) {
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

/**
 * Tries to process and object with the given type,
 * whereby this simply means checking that the value
 * is a plain object with a property `type` whose value
 * is the given type.
 *
 * @param {object} result - the resulting object to return, containing objectName and value representations of params.
 * @param {array} path - path in the original params object
 * @param {object} x - the value to process
 * @param {object} data - protocol data
 * @param {string} type - type of object expected
 */
function processObjectOfType(result, path, x, data, type) {
	//console.log("processObjectOfType:")
	//console.log({result, path, x, type})
	const paramName = path.join(".");
	expect.truthy({paramName}, _.isPlainObject(x), `expected an object of type ${type}: `+JSON.stringify(x));
	expect.truthy({paramName}, _.get(x, 'type') === type, `expected an object of type ${type}: `+JSON.stringify(x));
}

/**
 * Try to process a value as the keyword "stay" or as a Site reference.
 *
 * @param {object} result - the resulting object to return, containing objectName and value representations of params.
 * @param {array} path - path in the original params object
 * @param {object} x - the value to process
 * @param {object} data - protocol data
 */
function processSiteOrStay(result, path, x, data) {
	if (x === "stay") {
		// do nothing, leave the value as "stay"
	}
	else {
		processObjectOfType(result, path, x, data, "Site");
	}
}

/**
 * Try to process a value as a source reference.
 *
 * @param {object} result - the resulting object to return, containing objectName and value representations of params.
 * @param {array} path - path in the original params object
 * @param {object} x - the value to process
 * @param {object} data - protocol data
 */
function processSource(result, path, x, data) {
	// console.log(`processSource: ${JSON.stringify(path)}, ${JSON.stringify(x)}`)
	const l = processSources(result, path, x, data);
	expect.truthy({paramName: path.join('.')}, _.isArray(l) && l.length === 1, "expected a single liquid source: "+JSON.stringify(x));
	_.set(result.value, path, l[0]);
}

/**
 * Try to process a value as an array of source references.
 *
 * @param {object} result - the resulting object to return, containing objectName and value representations of params.
 * @param {array} path - path in the original params object
 * @param {object} x - the value to process
 * @param {object} data - protocol data
 */
function processSources(result, path, x, data) {
	//console.log({before: x, paramName})
	if (_.isString(x)) {
		x = wellsParser.parse(x, data.objects);
		//console.log({x})
		expect.truthy({paramName: path.join('.')}, _.isArray(x), "expected a liquid source: "+JSON.stringify(x));
		//x = [x];
	}
	else if (_.isPlainObject(x) && x.type === 'Liquid') {
		x = [x.wells];
	}
	else if (_.isArray(x)) {
		x = x.map((x2, index) => {
			const path2 = path.concat(index)
			return expect.try({paramName: path2.join('.')}, () => {
				const result2 = {value: {}, objectName: {}};
				processSource(result2, path2, x2, data);
				// console.log(`result2: ${JSON.stringify(result2)}`)
				return _.get(result2.value, path2);
			});
		});
	}
	// console.log({x})
	_.set(result.value, path, x);
	return x;
}

/**
 * Try to process a value as a temperature.
 *
 * @param {object} result - the resulting object to return, containing objectName and value representations of params.
 * @param {array} path - path in the original params object
 * @param {object} x - the value to process
 * @param {object} data - protocol data
 */
function processTemperature(result, path, x, data) {
	if (_.isString(x)) {
		x = math.eval(x);
	}
	//console.log({function: "processVolume", path, x})
	expect.truthy({paramName: path.join('.')}, math.unit('degC').equalBase(x), "expected a temperature with units degC, degF, or K: "+JSON.stringify(x));
	_.set(result.value, path, x);
	//console.log("set in result.value")
}

/**
 * Try to process a value as a volume.
 *
 * @param {object} result - the resulting object to return, containing objectName and value representations of params.
 * @param {array} path - path in the original params object
 * @param {object} x - the value to process
 * @param {object} data - protocol data
 */
function processVolume(result, path, x, data) {
	if (_.isNumber(x)) {
		x = math.unit(x, 'l');
	}
	else if (_.isString(x)) {
		x = math.eval(x);
	}
	//console.log({function: "processVolume", path, x})
	expect.truthy({paramName: path.join('.')}, math.unit('l').equalBase(x), "expected a volume with liter units (l, ul, etc.): "+JSON.stringify(x));
	_.set(result.value, path, x);
	//console.log("set in result.value")
}

/**
 * Try to process a value as a well reference.
 *
 * @param {object} result - the resulting object to return, containing objectName and value representations of params.
 * @param {array} path - path in the original params object
 * @param {object} x - the value to process
 * @param {object} data - protocol data
 */
function processWell(result, path, x, data) {
	if (_.isString(x)) {
		//console.log("processWell:")
		//console.log({result, path, x})
		x = wellsParser.parse(x, data.objects);
	}
	expect.truthy({paramName: path.join('.')}, _.isArray(x) && x.length === 1, "expected a single well indicator: "+JSON.stringify(x));
	_.set(result.value, path, x[0]);
}

/**
 * Try to process a value as an array of wells.
 *
 * @param {object} result - the resulting object to return, containing objectName and value representations of params.
 * @param {array} path - path in the original params object
 * @param {object} x - the value to process
 * @param {object} data - protocol data
 */
function processWells(result, path, x, data) {
	if (_.isString(x)) {
		x = wellsParser.parse(x, data.objects);
	}
	expect.truthy({paramName: path.join('.')}, _.isArray(x), "expected a list of wells: "+JSON.stringify(x));
	_.set(result.value, path, x);
}

/**
 * Try to process a value as a time duration.
 *
 * @param {object} result - the resulting object to return, containing objectName and value representations of params.
 * @param {array} path - path in the original params object
 * @param {object} x0 - the value to process
 * @param {object} data - protocol data
 */
function processDuration(result, path, x0, data) {
	let x = x0;
	if (_.isNumber(x)) {
		x = math.unit(x, 's');
	}
	else if (_.isString(x)) {
		x = math.eval(x);
	}
	//console.log({a: math.unit('s'), value: x, x0})
	expect.truthy({paramName: path.join('.')}, math.unit('s').equalBase(x), "expected a value with time units (s, second, seconds, minute, minutes, h, hour, hours, day, days): "+JSON.stringify(x0));
	_.set(result.value, path, x);
}

/**
 * Get a property value from an object in the parsed parameters.
 * If no value could be found (and no default was given) then an exception
 * will be thrown.
 *
 * @param {object} parsed - the parsed parameters object, as passed into a command handler
 * @param {object} data - protocol data
 * @param {string} paramName - parameter name (which should reference an object)
 * @param {string} propertyName - name of the object's property to retrieve
 * @param {any} defaultValue - default value if property not found
 * @return {any} the property value
 */
function getParsedValue(parsed, data, paramName, propertyName, defaultValue) {
	const value = _.get(parsed.value[paramName], propertyName, defaultValue);
	const objectName = parsed.objectName[paramName];
	//console.log({parsed, x: parsed[paramName], paramName, propertyName})
	if (!_.isUndefined(value)) {
		const objectName1 = (objectName) ? objectName+"."+propertyName : paramName+"/"+propertyName;
		//console.trace();
		expect.truthy({objectName1}, !_.isUndefined(value), "missing value");
		return value;
	}
	else {
		expect.truthy({paramName: paramName}, !_.isUndefined(defaultValue), "missing parameter value");
		return defaultValue;
	}
}

/**
 * Query the logic database with the given predicates and return the values
 * of interest.
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

/**
 * Helper function for queryLogic() that replaces undefined property values with
 * the name of the property prefixed by '?'.
 * @param  {Array} predicates    Array of llpl predicates
 */
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
 * Lookup nested paths.
 *
 * This example will first lookup `object` in `params`,
 * then lookup the result in `data.objects`,
 * then get the value of `model`,
 * then lookup it value for `evowareName`:
 *
 * ```
 * [["@object", "model"], "evowareName"]
 * ```
 *
 * @param  {array} path   [description]
 * @param  {object} params [description]
 * @param  {object} data   [description]
 * @return {any}        [description]
 */
function lookupPath(path, params, data) {
	//console.log({path, params, data})
	let prev;
	_.forEach(path, elem => {
		//console.log({elem})
		let current = elem;
		if (_.isArray(elem))
			current = lookupPath(elem, params, data);
		else {
			assert(_.isString(current));
			if (_.startsWith(current, "@")) {
				//console.log({current, tail: current.substring(1)})
				current = current.substring(1);
				//console.log({current})
				assert(_.has(params, current));
				current = _.get(params, current);
			}
		}

		//console.log({prev, current})
		if (_.isUndefined(prev)) {
			if (_.isString(current)) {
				const result = {value: {}, objectName: {}};
				const path2 = []; // FIXME: figure out a sensible path in case of errors
				current = lookupValue0(result, path2, current, data);
			}
			prev = current;
		}
		else {
			assert(_.isString(current));
			assert(_.has(prev, current));
			prev = _.get(prev, current);
		}
	});
	return prev;
}

function lookupPaths(paths, params, data) {
	return _.mapValues(paths, path => lookupPath(path, params, data));
}

/**
 * Return array of step keys in order.
 * Any keys that begin with a number will be included,
 * and they will be sorted in natural order.
 *
 * @param  {object|array} o - an object or array of steps
 * @return {array} an ordered array of keys that represent steps
 */
function getStepKeys(steps) {
	if (_.isPlainObject(steps)) {
		// Find all sub-steps (properties that start with a digit)
		const rx = /^[0-9]/;
		const keys = _.keys(steps).filter(x => rx.test(x));
		// Sort them in "natural" order
		keys.sort(naturalSort);
		return keys;
	}
	else if (_.isArray(steps)) {
		return _.range(steps.length);
	}
	else {
		return [];
	}
}

/**
 * Return an object that conforms to the expected format for steps.
 *
 * @param  {array|object} steps - input in format of an array of steps, a single step, or propertly formatted steps.
 * @return {object} an object with only numeric keys, representing a sequence of steps.
 */
function stepify(steps) {
	if (_.isPlainObject(steps)) {
		const rx = /^[0-9]/;
		const hasOnlyStepKeys = _.keys(steps).every(x => rx.test(x));
		if (hasOnlyStepKeys) {
			return steps;
		}
		else {
			return {"1": steps};
		}
	}
	else if (_.isArray(steps)) {
		steps = _.compact(_.flattenDeep(steps));
		return _.zipObject(_.range(1, steps.length+1), steps);
	}
	else {
		assert(false, "expected an array or a plain object: "+JSON.stringify(steps));
	}
}

/**
 * Process '@DATA', '@SCOPE', and 'data' properties for a step,
 * and return updated {DATAs, SCOPEs, foreach}.
 */
function updateSCOPEDATA(step, data, SCOPE, DATA) {
	// console.log("data2: "+JSON.stringify(data));
	if (step.hasOwnProperty("@DATA")) {
		DATA = step["@DATA"];
		// console.log("DATA: "+JSON.stringify(DATA))
	}

	let DATAs = [DATA];
	let foreach = false; // whether we need to replicate the step contents
	// Handle `data` parameter by loading Design data SCOPE and possibly
	// repeating the command for each group or each row
	if (step["data"]) {
		const dataInfo = misc.handleDirectiveDeep(step.data, data);
		// console.log({dataInfo})
		let table = DATA;
		if (dataInfo.source) {
			const source = _.get(data.objects, [dataInfo.source]);
			assert(source);

			if (_.isArray(source)) {
				table = source;
			}
			else if (source.type === "Design") {
				table = Design.flattenDesign(source);
			}
			else {
				assert(false, "unrecognized data source: "+JSON.stringify(dataInfo.source)+" -> "+JSON.stringify(source));
			}
		}

		// Replicate the command for each group
		const groups = Design.query(table, dataInfo);
		// console.log("groups: "+JSON.stringify(groups))

		if (dataInfo.forEach === "row") {
			// Turn each row into its own group
			DATAs = _(groups).flatten().map(x => [x]).value();
			//console.log("forEach row DATAs: "+JSON.stringify(DATAs, null, '\t'));
			foreach = true;
		}
		else if (dataInfo.forEach === "group") {
			DATAs = groups;
			//console.log("forEach group DATAs: "+JSON.stringify(DATAs, null, '\t'));
			foreach = true;
		}
		else {
			DATAs = [_.flatten(groups)];
		}
	}
	//console.log("DATAs: "+JSON.stringify(DATAs, null, '\t'));

	//console.log({step, params})

	// Add `@SCOPE` variables to SCOPE, which are automatically inserted into `protocol.objects.SCOPE` before a command handler is called.
	//console.log({_scope: params["@SCOPE"]})
	if (!_.isEmpty(step["@SCOPE"])) {
		SCOPE = _.defaults(step["@SCOPE"], SCOPE);
		//console.log("SCOPE: "+JSON.stringify(SCOPE))
	}

	const SCOPEs = DATAs.map(DATA => {
		const common = Design.getCommonValues(DATA);
		return _.defaults(common, SCOPE);
	});

	return {DATAs, SCOPEs, foreach};
}

module.exports = {
	asArray,
	createData,
	_dereferenceVariable: dereferenceVariable,
	getParsedValue,
	getStepKeys,
	lookupPath,
	lookupPaths,
	parseParams,
	queryLogic,
	stepify,
	substituteDeep,
	updateSCOPEDATA,
}
