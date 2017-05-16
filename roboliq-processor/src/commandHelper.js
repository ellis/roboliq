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
const Design = require('./design.js');
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
 *
 * TODO: Rather than calling it 'data', we should probably rename it to 'context'.
 *
 * @param  {Protocol} protocol
 * @param  {object} objects  =             {} - current objects
 * @param  {object} SCOPE    =             {} - current SCOPE
 * @param  {array} DATA     =             [] - current DATA table
 * @param  {array} path = [] - current processing path (usually a step ID, e.g. step 1.2 would be given by `[1, 2]`)
 * @param  {object} files = {} - map of filename to loaded filedata
 * @return {object} the 'data' object that gets passed into many commandHelper functions
 */
function createData(protocol, objects = {}, SCOPE = {}, DATA = [], path = [], files = {}, step = {}) {
	const updatedSCOPEDATA = updateSCOPEDATA(step, {objects: _.defaults({SCOPE, DATA}, objects)}, SCOPE, DATA);
	// console.log({step, SCOPE: updatedSCOPEDATA.SCOPE, DATA: updatedSCOPEDATA.DATA})

	// Process any directives in this step
	const objects2 = _.clone(objects);
	// TODO: consider changing this so that DATA and SCOPE are not a part of `objects`,
	// but are their own separate properties.
	objects2.DATA = updatedSCOPEDATA.DATA;
	objects2.SCOPE = _.defaults(
		{
			// access the raw objects
			__objects: objects2,
			// access the current raw data table
			__data: updatedSCOPEDATA.DATA,
			// access raw protocol parameters
			__parameters: protocol.parameters || {},
			// access parameters of the current step
			__step: step,
			// access parameters from any step in the current step stack (0 = current step)
			__stepStack: null,
			// function to return a column from the current data table
			__column: (name) => { _(updatedSCOPEDATA.DATA).map(name).value() }
		},
		updatedSCOPEDATA.SCOPE,
		_.mapValues(protocol.parameters || {}, x => x.value)
	);

	const context = {
		objects: objects2,
		schemas: protocol.schemas,
		accesses: [],
		files,
		protocol,
		path
	};
	// console.log("SCOPE:")
	// console.log(context.objects.SCOPE);

	return context;
}

function getDesignFactor(propertyName, DATA) {
	return _(DATA).map(propertyName).filter(x => !_.isUndefined(x)).value();
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
function substituteDeep(x, data, SCOPE, DATA, addCommonValuesToScope=true) {
	// console.log("substituteDeep: "); console.log({x, SCOPE, DATA, x})
	let x2 = x;
	if (_.isString(x)) {
		/*// DATA substitution
		if (_.startsWith(x, "$$")) {
			if (_.isArray(DATA)) {
				const propertyName = x.substr(2);
				x2 = getDesignFactor(propertyName, DATA);
				assert(x2.length > 0 || DATA.length == 0, `factor ${x} not found in data`);
				// console.log({x2})
				// console.log("DATA: "+JSON.stringify(DATA, null, '\t'));
				// console.log({map: _(DATA).map(propertyName).value()});
			}
			else {
				assert(false, `invalid factor ${x}, because no data source is currently selected`);
			}
		}
		// Javascript
		else*/ if (_.startsWith(x, "${") && _.endsWith(x, "}")) {
			const safeEval = require('safe-eval');
			const code = x.substr(2, x.length - 3);
			const scope = _.defaults({_, math}, SCOPE, data.objects.PARAMS);
			// console.log({code, scope})
			x2 = safeEval(code, scope);
			// console.log({x2})
		}
		// Mathjs calculation
		else if (_.startsWith(x, "$(") && _.endsWith(x, ")")) {
			const expr = x.substr(2, x.length - 3);
			// console.log({expr})
			const context = _.defaults({}, SCOPE, data.objects.PARAMS)
			// console.log({expr, context})
			x2 = Design.calculate(expr, context);
			// console.log({x2, expr})
		}
		// Mathjs calculation (deprecated)
		else if (x.length > 2 && _.startsWith(x, "$`") && _.endsWith(x, "`")) {
			const expr = x.substr(2, x.length - 3);
			// console.log({expr})
			// process.exit()
			x2 = Design.calculate(expr, SCOPE);
			// console.log({x2, expr})
		}
		// Variable substitution
		else if (_.startsWith(x, "$@")) {
			const propertyName = x.substr(2);
			x2 = _.get(data.objects, [propertyName, "value"], x);
		}
		// SCOPE substitution
		else if (_.startsWith(x, "$")) {
			const propertyName = x.substr(1);
			assert(_.has(SCOPE, propertyName), `${x} not in scope`);
			x2 = _.get(SCOPE, propertyName, x);
		}
		// Template substitution
		else if (_.startsWith(x, "`") && _.endsWith(x, "`")) {
			const template = x.substr(1, x.length - 2);
			const scope = SCOPE; //_.mapKeys(SCOPE, (value, name) => "$"+name);
			//console.log({x, template, scope})
			x2 = misc.renderTemplate(template, scope, data);
		}
	}
	else if (_.isArray(x)) {
		x2 = _.map(x, y => substituteDeep(y, data, SCOPE, DATA, addCommonValuesToScope));
	}
	else if (_.isPlainObject(x)) {
		const updatedSCOPEDATA = updateSCOPEDATA(x, data, SCOPE, DATA, addCommonValuesToScope);
		x2 = _.mapValues(x, (value, name) => {
			// Skip over @DATA, @SCOPE, directives and 'steps' properties
			if (_.startsWith(name, "#") || _.endsWith(name, "()") || name === "data" || name === "@DATA" || name === "@SCOPE" || name === "steps") {
				return value;
			}
			else {
				return substituteDeep(value, data, updatedSCOPEDATA.SCOPE, updatedSCOPEDATA.DATA, addCommonValuesToScope);
			}
		});
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
	const substituted = substituteDeep(params, data, data.objects.SCOPE, data.objects.DATA);
	//console.log("SCOPE: "+JSON.stringify(data.objects.SCOPE, null, '\t'))
	const result = {orig: substituted, value: {}, objectName: {}, unknown: []};
	processParamsBySchema(result, [], substituted, schema, data);
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
	// console.log(`processParamsBySchema: ${JSON.stringify(params)} ${JSON.stringify(schema)}`)
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
			const value1
				= (_.startsWith(value0, "$@"))
						? _.get(data.objects, value0.substring(2), value0)
					: (_.startsWith(value0, "$"))
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
		else if (_.startsWith(type, "nameOf ")) {
			const type1 = type.substr(7);
			const value1 = lookupValue0(result, path1, value0, data);
			expect.truthy({paramName: path1.join(".")}, value1.type === type1, `expect the name of an object of type ${type1}: ${JSON.stringify(value1)}`);
			_.set(result.value, path1, result.objectName[path1.join(".")]);
			// console.log("nameOf result: "+JSON.stringify(result));
		}
		else {
			const value1 = _.clone(lookupValue0(result, path1, value0, data));
			if (!_.isUndefined(value1)) {
				processValue0BySchema(result, path1, value1, p, data, propertyName);
			}
			// If not optional, require the variable's presence:
			else if (required) {
				// console.log({propertyName, type, result, path, params, schema})
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
	// console.log(`processValue0BySchema(${path.join('.')}, ${JSON.stringify(value0)})`)
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
			return processValue0OnTypes(result, path, value0, schema, types, data);
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
	if (type === "name") {
		_.set(result.value, path, value0);
		return;
	}
	else if (_.startsWith(type, "nameOf ")) {
		// REFACTOR: this duplicates code in processValue0BySchema()
		const type1 = type.substr(7);
		const value1 = lookupValue0(result, path, value0, data);
		expect.truthy({paramName: path.join(".")}, value1.type === type1, `expect the name of an object of type ${type1}: ${JSON.stringify(value1)}`);
		_.set(result.value, path, result.objectName[path.join(".")]);
		// console.log("nameOf result : "+JSON.stringify(result));
		return;
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
			expect.truthy({paramName: name}, _.isInteger(value), "expected integer: "+value);
			return;
		case "markdown": return processString(result, path, value, data);
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
		case "Labware": return processValue0OnTypes(result, path, value0, schema, ["Lid", "Plate", "Trough", "Tube"], data);
		case "Length": return processLength(result, path, value, data);
		case "Lid": return processObjectOfType(result, path, value, data, type);
		case "Plate": return processObjectOfType(result, path, value, data, type);
		case "Plates": return processOneOrArray(result, path, value, (result, path, x) => processObjectOfType(result, path, x, data, "Plate"));
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
 * A sub-function of processValue0BySchema().
 * Try to process the value as a named type.
 * @param  {object} result - result structure for values and objectNames
 * @param  {array} path - path in params
 * @param  {any} value0 - the value to process
 * @param  {object} schema - schema
 * @param  {array} types - a list of types to try
 * @param  {object} data - protocol data
 */
function processValue0OnTypes(result, path, value0, schema, types, data) {
	// console.log({types})
	let es = [];
	for (const t of types) {
		try {
			// console.log({t, path, value0})
			return processValue0BySchemaType(result, path, value0, schema, t, data);
		}
		catch (e) {
			es.push(e);
		}
	}

	if (!_.isEmpty(es))
		// throw es[0];
		throw es.join("; ");
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

	return _.get(data.objects, path, dflt);
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
			result.value = getDesignFactor(propertyName, data.objects.DATA);
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
		// Handle Variable reference
		if (_.startsWith(name, "$@")) {
			// console.log({name})
			name = name.substr(2);
		}
		// Handle SCOPE abbreviation
		else if (_.startsWith(name, "$")) {
			name = "SCOPE."+name.substr(1);
		}

		while (_.has(data.objects, name)) {
			const value = g(data, name);
			// console.log({value})
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
	// console.log("processOneOrArray:")
	// console.log({path, value0})
	// Try to process value0 as a single value, then turn it into an array
	try {
		_.set(result.value, path, undefined);
		const path2 = path.concat(0);
		fn(result, path2, value0);
		// console.log(JSON.stringify(result));
		// console.log(JSON.stringify(_.get(result.value, path)));
		const x = _.get(result.value, path2);
		// console.log({path2, x: JSON.stringify(x)})
		_.set(result.value, path2, x);
		// console.log(JSON.stringify(result, null, '\t'))
		return;
	} catch (e) {
		// console.log(e)
	}

	expect.truthy({paramName: path.join('.')}, _.isArray(value0), "expected an array: "+JSON.stringify(value0));
	value0.forEach((x, i) => fn(result, path.concat(i), x));
}

/**
 * Try to process a value as a length.
 *
 * @param {object} result - the resulting object to return, containing objectName and value representations of params.
 * @param {array} path - path in the original params object
 * @param {object} x - the value to process
 * @param {object} data - protocol data
 */
function processLength(result, path, x, data) {
	if (_.isString(x)) {
		x = math.eval(x);
	}
	//console.log({function: "processLength", path, x})
	expect.truthy({paramName: path.join('.')}, math.unit('m').equalBase(x), "expected a volume with meter units (m, mm, nm, etc.): "+JSON.stringify(x));
	_.set(result.value, path, x);
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
				// console.log({x2})
				if (_.isPlainObject(x2) && x2.type === 'Liquid') {
					return [x.wells];
				}
				else {
					const result2 = {value: {}, objectName: {}};
					// console.log({result2, path2, x2})
					processSource(result2, path2, x2, data);
					// console.log(`result2: ${JSON.stringify(result2)}`)
					return _.get(result2.value, path2);
				}
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
	if (_.isString(x)) {
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
function queryLogicGeneral(data, predicates, queryExtract) {
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
 * Query the logic database with the given predicates.  If solutions are found,
 * choose one of the alternatives.
 *
 * @param  {Object} data         Command data
 * @param  {Array} predicates    Array of llpl predicates
 * @param  {String} predicateName Name of the predicate we're interested in
 * @return {Array} - an array where the first item is the chosen solution, and the second item includes all alternatives.  If no solution was found, then both items will be undefined.
 */
function queryLogic(data, predicates, predicateName) {
	const resultList = queryLogicGeneral(data, predicates, undefined);
	if (_.isEmpty(resultList)) {
		return [undefined, undefined];
	}

	const queryExtract = `[].and[]."${predicateName}"`
	const alternatives = jmespath.search(resultList, queryExtract);
	assert(!_.isEmpty(alternatives), `${predicateName} not found in resultList ${JSON.stringify(resultList)} for predicates ${JSON.stringify(predicates)}`);

	// Pick a plan
	let chosen = alternatives[0];
	if (data.planAlternativeChoosers.hasOwnProperty(predicateName)) {
		chosen = data.planAlternativeChoosers[predicateName](alternatives, data);
		// console.log({chosen})
	}

	return [chosen, alternatives];
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
 * @example
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
 * Parse input spec and return object with the same properties as the spec,
 * but with values looked up.
 */
function parseInputSpec(inputSpec, parsed, data) {
	return _.mapValues(inputSpec, (item, key) => {
		return lookupInputPath(item, parsed, data);
	});
}

/**
 * Lookup nested paths.
 *
 * @example
 *
 * * "object": gets parameter value.
 * * "?object": optionally gets parameter value.
 * * "object*": looks up object.
 * * "object*location": looks up object, gets `location` property.
 * * "object*location*": looks up object, gets `location` property, looks up location.
 * * "object*location*type": looks up object, looks up its `location` property, gets type property.
 * * "something**": double de-reference
 * * "object*(someName)": looks up object, gets someName value, gets object's property with that value. (this is not currently implemented)
  *
 * @param  {array} path   [description]
 * @param  {object} parsed [description]
 * @param  {object} data   [description]
 * @return {any}        [description]
 */
function lookupInputPath(path, parsed, data) {
	// console.log("lookupInputPath:"); console.log({path, parsed})
	assert(_.isString(path) && !_.isEmpty(path));

	// Check whether success is required
	let required = true;
	if (path[0] == "?") {
		required = false;
		path = path.substring(1);
	}

	const elems = _.filter(path.split(/([*])/), s => !_.isEmpty(s));
	// console.log({elems});

	let current;
	try {
		for (let i = 0; i < elems.length; i++) {
			const elem = elems[i];
			if (elem == "*") {
				assert(_.isString(current), "cannot dereference: "+JSON.stringify({path, i, elem, current}));
				current = lookupInputPath_dereference(current, data);
			}
			else {
				if (_.isUndefined(current)) {
					current = parsed.objectName[elem] || parsed.orig[elem];
				}
				else {
					current = _.get(current, elem);
				}
			}
			assert(current, `${elem} not found in path ${path}`);
		}
	} catch (e) {
		if (!required)
			return undefined;
		throw e;
	}

	return current;
}

function lookupInputPath_dereference(current, data) {
	const result = {value: {}, objectName: {}};
	const path2 = []; // FIXME: figure out a sensible path in case of errors
	const current2 = lookupValue0(result, path2, current, data);
	return current2;
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
 * The returned data table will be the first to exist of '@DATA', 'DATA', and 'objects.DATA'
 * The returned scope will be the merger of data.objects.SCOPE, SCOPE, '@SCOPE', and common DATA values.
 * and return updated {DATA, SCOPE}.
 */
function updateSCOPEDATA(step, data, SCOPE = undefined, DATA = undefined, addCommonValuesToScope=true) {
	// console.log("updateSCOPEDATA");
	// console.log({step})
	// FIXME: Debug only
	// if (step.value) {
	// 	assert(false);
	// }
	// ENDFIX
	// console.log("data2: "+JSON.stringify(data));
	// console.log({SCOPE})
	DATA
		= (step.hasOwnProperty("@DATA")) ? step["@DATA"]
		: (!_.isUndefined(DATA)) ? DATA
		: data.objects.DATA || [];
	// console.log({DATA})

	// Handle `data` parameter by loading Design data SCOPE and possibly
	// repeating the command for each group or each row
	if (step.hasOwnProperty("data")) {
		const dataInfo = misc.handleDirectiveDeep(step.data, data);
		// console.log({dataInfo})
		let table = DATA;
		if (_.isString(dataInfo) || dataInfo.source) {
			const dataId = _.isString(dataInfo) ? dataInfo : dataInfo.source;
			const source = _.get(data.objects, dataId);
			// console.log({source})
			// console.log("data.objects:")
			// console.log(data.objects)
			assert(source, `Data source not found: ${dataId}`);

			if (_.isArray(source)) {
				table = source;
			}
			else if (source.type === "Data") {
				if (!_.isUndefined(source.value)) {
					table = source.value;
				}
				else {
					const design = substituteDeep(source, data, SCOPE, DATA);
					table = Design.flattenDesign(design);
				}
			}
			else {
				assert(false, "unrecognized data source: "+JSON.stringify(dataId)+" -> "+JSON.stringify(source));
			}
		}

		if (_.isPlainObject(dataInfo)) {
			table = _.flatten(Design.query(table, dataInfo));
			// console.log({dataInfo, table})
		}
		DATA = table;
	}
	//console.log("DATAs: "+JSON.stringify(DATAs, null, '\t'));

	const always = {
		// access the raw objects
		__objects: data.objects,
		// access the current raw data table
		__data: DATA,
		// access raw protocol parameters
		__parameters: _.get(data, ["protocol", "parameters"], {}),
		// access parameters from any step in the current step stack (0 = current step)
		// __stepStack: null,
		// function to return a column from the current data table
		__column: (name) => { _(DATA).map(name).value() }
	};
	if (step.hasOwnProperty("command")) {
		// access parameters of the current step
		always.__step = step;
	}
	// console.log({isEmpty: _.isEmpty(DATA)})
	const columns = (_.isEmpty(DATA))
		? {}
		: _.fromPairs(_.map(_.keys(DATA[0]), key => [key, _.map(DATA, key)]));
	// console.log({DATA, columns, strange: _.map(DATA, "n")});
	const common = _.mapKeys(
		(addCommonValuesToScope) ? Design.getCommonValues(DATA) : {},
		(value, key) => key + "_ONE"
	);
	const ATSCOPE = (step.hasOwnProperty("@SCOPE")) ? step["@SCOPE"] : {};
	SCOPE = _.defaults(always, columns, common, ATSCOPE, SCOPE, data.objects.SCOPE);

	return {DATA, SCOPE};
}

function copyItemsWithDefaults(items, defaults) {
	// console.log("copyItemsWithDefaults: "+JSON.stringify(items)+", "+JSON.stringify(defaults))
	if (_.isArray(items)) {
		items = _.cloneDeep(items);
	}
	// Create a new array with the appropriate size
	else {
		const defaultCounts = _.mapValues(defaults, (value) => (_.isArray(value)) ? value.length : 1);
		let counts = _.uniq(_.values(defaultCounts));
		counts.sort();
		let size;
		if (counts.length === 1) {
			size = counts[0];
		}
		else {
			counts = _.filter(counts, n => n != 1);
			assert(counts.length === 1, "unequal array sizes: "+JSON.stringify({items, defaults}));
			size = counts[0];
		}
		items = _.map(_.range(size), () => ({}));
	}

	for (let i = 0; i < items.length; i++) {
		const item = items[i];
		_.forEach(defaults, (value, name) => {
			if (_.isUndefined(item[name]) && !_.isUndefined(value)) {
				if (_.isArray(value)) {
					if (value.length === 1) {
						item[name] = value[0];
					}
					else {
						assert(i < value.length, "value array not long enough for target: "+JSON.stringify({name, i, value, target: items}));
						item[name] = value[i];
					}
				}
				else {
					item[name] = value;
				}
			}
		});
	}

	return items;
}

function splitItemsAndDefaults(items, keysToSkip) {
	// console.log("splitItemsAndDefaults: "+JSON.stringify(items)+", "+JSON.stringify(keysToSkip))
	let defaults = {};

	if (_.size(items) > 1) {
		defaults = Design.getCommonValues(items);
		if (_.isArray(keysToSkip) && !_.isEmpty(keysToSkip)) {
			defaults = _.omit(defaults, keysToSkip);
		}
		// console.log({defaults})

		if (_.size(defaults) > 0) {
			const keysToOmit = _.keys(defaults);
			items = _.map(items, item => _.omit(item, keysToOmit));
		}
	}

	return {items, defaults};
}

/*
function setDefaultInArrayOfObjects(name, value, l) {
	assert(_.isArray(l), "expected and array: "+JSON.stringify(l));
	for (let i = 0; i < l.length; i++) {
		const item = l[i];
		if (_.isUndefined(item[name])) {
			if (_.isArray(value)) {
				assert(i < value.length, "value array not long enough for target: "+JSON.stringify({value, target: l}));
				item[name] = value[i];
			}
			else {
				item[name] = value;
			}
		}
	}
}
*/

module.exports = {
	asArray,
	copyItemsWithDefaults,
	createData,
	getDesignFactor,
	_dereferenceVariable: dereferenceVariable,
	_g: g,
	// getCommonValues: Design.getCommonValues,
	getParsedValue,
	getStepKeys,
	lookupPath,
	lookupPaths,
	parseInputSpec,
	_lookupInputPath: lookupInputPath,
	parseParams,
	queryLogic,
	// setDefaultInArrayOfObjects,
	splitItemsAndDefaults,
	stepify,
	substituteDeep,
	updateSCOPEDATA,
}
