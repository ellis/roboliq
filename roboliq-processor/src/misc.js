/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017, ETH Zurich, Ellis Whitehead
 * @license GPL-3.0
 */

/**
 * A set of miscellaneous helper functions.
 *
 * Some of these are HACKs that should be moved to another module or removed entirely.
 *
 * @module
 */

const _ = require('lodash');
const assert = require('assert');
const Handlebars = require('handlebars');

Handlebars.registerHelper('toJSON', function(obj) {
	return JSON.stringify(obj);
});

/**
 * queryResults: value returned from llpl.query()
 * predicateName: name of the predicate that was used for the query
 * @returns {object} `{parameterName1: parameterValues1, ...}`
 * @static
 */
function extractValuesFromQueryResults(queryResults, predicateName) {
	var acc = _.reduce(queryResults, function(acc, x1) {
		var x2 = x1[predicateName];
		_.forEach(x2, function(value, name) {
			if (_.isEmpty(acc[name]))
				acc[name] = [value];
			else
				acc[name].push(value);
		});
		return acc;
	}, {});
	return acc;
}

function findObjectsValue(key, objects, effects, defaultValue, prefix) {
	if (effects) {
		var id = (prefix) ? prefix+"."+key : key;
		if (effects.hasOwnProperty(id))
			return effects[id];
	}
	return _.get(objects, key, defaultValue);
}

// NOTE: This is basically a copy of expect.objectsValue
function getObjectsValue(key, objects, effects, prefix) {
	assert(_.isString(key), "getObjectsValue expected a string key, received: "+JSON.stringify(key));
	if (effects) {
		var id = (prefix) ? prefix+"."+key : key;
		if (effects.hasOwnProperty(id))
			return effects[id];
	}
	var l = key.split('.');
	for (var i = 0; !_.isEmpty(objects) && i < l.length; i++) {
		if (!objects.hasOwnProperty(l[i])) {
			var objectName = _.take(l, i + 1).join('.');
			if (prefix) objectName = prefix + '.' + objectName;
			var message = "value `"+objectName+"`: undefined";
			// console.log({key, objects})
			//console.log("objects:", objects)
			//console.log(message);
			throw new Error(message);//{name: "ProcessingError", errors: [message]};
		}
		objects = objects[l[i]];
	}
	return objects;
}

function getVariableValue(spec, objects, effects, prefix) {
	if (_.isString(spec)) {
		if (_.startsWith(spec, '"'))
			return spec;
		var found = findObjectsValue(spec, objects, effects);
		if (!_.isUndefined(found)) {
			if (found.type === "Variable") {
				return found.value;
			}
			else {
				return found;
			}
		}
	}
	return spec;
}

function getObjectsOfType(objects, types, prefix) {
	if (_.isString(types)) types = [types];
	if (!prefix) prefix = [];

	var l = {};
	_.forEach(objects, function(o, name) {
		var prefix1 = prefix.concat([name]);
		if (_.has(o, "type") && _.isString(o.type) && types.indexOf(o.type) >= 0) {
			var id = prefix1.join('.');
			l[id] = o;
		}
		_.forEach(o, function(o2, name2) {
			if (_.isPlainObject(o2)) {
				var prefix2 = prefix1.concat([name2]);
				_.merge(l, getObjectsOfType(o2, types, prefix2));
			}
		});
	});
	return l;
}

/**
 * If spec is a directive, process it and return the result.
 *
 * @param  {Any} spec Any value.  If this is a directive, it will be an object with a single key that starts with '#'.
 * @param  {Object} data An object with properties: directiveHandlers, objects, events.
 * @return {Any} Return the object, or if it was a directive, the results of the directive handler.
 */
function handleDirective(spec, data) {
	// console.log(`handleDirective: `+JSON.stringify(spec))
	// console.log("data1: "+JSON.stringify(data));
	const directiveHandlers = data.directiveHandlers || (data.protocol || {}).directiveHandlers;
	if (_.isPlainObject(spec)) {
		const keys = _.keys(spec);
		if (keys.length === 1) {
			const key0 = keys[0];
			const key
				= (_.startsWith(key0, "#")) ? key0.substr(1)
				: (_.endsWith(key0, "()")) ? key0.substr(0, key0.length - 2)
				: undefined;
			// console.log({key0, key})
			if (key) {
				if (directiveHandlers.hasOwnProperty(key)) {
					var spec2 = spec[key0];
					// console.log({spec2, handler: directiveHandlers[key]})
					var spec3 = (_.isPlainObject(spec2))
						? _.omit(spec2, 'override')
						: spec2;
					const result = {
						x: directiveHandlers[key](spec3, data)
					};
					if (spec2.hasOwnProperty('override')) {
						//console.log({result0: result.x})
						_.merge(result, {x: spec2.override});
						//console.log({result1: result.x})
					}
					return result.x;
				}
				else {
					throw new Error("unknown directive object: "+key);
				}
			}
		}
	}
	else if (_.isString(spec)) {
	 	// Inline directives
		if (_.startsWith(spec, "#")) {
		 	const hash2 = spec.indexOf('#', 1);
			const key = (hash2 > 0) ? spec.substr(1, hash2 - 1) : spec.substr(1);
			// console.log({hash2, key})
			if (directiveHandlers.hasOwnProperty(key)) {
				const spec2 = (hash2 > 0) ? spec.substr(hash2 + 1) : undefined;
				const spec3 = handleDirective(spec2, data);
				const result = directiveHandlers[key](spec3, data);
				if (spec.hasOwnProperty('override')) {
					_.merge(result, spec.override);
				}
				return result;
			}
			else {
				throw new Error("unknown directive string: "+spec);
			}
		}
		// Protocol parameters
		else if (_.startsWith(spec, "$#")) {
			const key = spec.substr(2);
			if (_.has(data.objects.PARAMS, key)) {
				const result = data.objects.PARAMS[key];
				if (!_.isUndefined(result)) {
					return result;
				}
				else {
					throw new Error("undefined parameter value: "+spec);
				}
			}
			else {
				throw new Error("undefined parameter: "+spec);
			}
		}
	}
	return spec;
}

/**
 * Recurses into object properties and replaces them with the result of handleDirective.
 * It will, however, skip properties named 'steps'.
 *
 * @param  {Any} spec Any value.  If this is a directive, it will be an object with a single key that starts with '#'.
 * @param  {Object} data An object with properties: directiveHandlers, objects, events.
 * @return {Any} Return the object, or if it was a directive, the results of the directive handler.
 */
function handleDirectiveDeep(x, data) {
	//return mapDeep(spec, function(spec) { return handleDirective(spec, data); });
	if (_.isPlainObject(x)) {
		if (!x.hasOwnProperty('data')) {
			x = _.mapValues(x, function(value, key) {
				return (key === 'steps')
					? value
					: handleDirectiveDeep(value, data);
			});
		}
	}
	else if (_.isArray(x)) {
		x = _.map(x, function(value, i) {
			return handleDirectiveDeep(value, data);
		});
	}
	x = handleDirective(x, data);
	return x;
}

/**
 * Recurses into object properties and maps them to the result of fn.
 *
 * @static
 * @param  {Any} x Any value.
 * @param  {Function} fn A function (x, key, path) that returns a mapped value.
 * @return {Any} Return the deeply mapped object.
 */
function mapDeep(x, fn, key, path = []) {
	if (_.isPlainObject(x)) {
		x = _.mapValues(x, function(value, key) {
			return mapDeep(value, fn, key, path.concat(key));
		});
	}
	else if (_.isArray(x)) {
		x = _.map(x, function(value, i) {
			return mapDeep(value, fn, i, path.concat(i));
		});
	}
	x = fn(x, key, path);
	return x;
}

/**
 * Recurses into object properties and replaces them with the result of fn.
 * 'x' will be mutated.
 *
 * @static
 * @param  {Any} x Any value.
 * @param  {Function} fn A function that returns a transformed value.
 * @return nothing
 */
function mutateDeep(x, fn) {
	//console.log("x:", x)
	if (_.isPlainObject(x)) {
		for (var key in x) {
			//console.log("key:", key)
			x[key] = mutateDeep(x[key], fn);
		}
	}
	else if (_.isArray(x)) {
		for (let i = 0; i < x.length; i++) {
			x[i] = mutateDeep(x[i], fn);
		}
	}
	return fn(x);
}

function renderTemplate(template, scope, data) {
	//console.log("renderTemplate:", template)
	if (_.isString(template)) {
		return renderTemplateString(template, scope, data);
	}
	else if (_.isArray(template)) {
		return _.map(template, function(x) { return renderTemplate(x, scope, data); });
	}
	else if (_.isPlainObject(template)) {
		return _.mapValues(template, function(x) { return renderTemplate(x, scope, data); });
	}
	else {
		return template;
	}
}

function renderTemplateString(s, scope, data) {
	//console.log("renderTemplateString:", s)

	assert(_.isString(s));
	if (_.startsWith(s, "${") && _.endsWith(s, "}")) {
		var name = s.substr(2, s.length - 3);
		return scope[name];
	}
	else if (_.startsWith(s, "{{") && _.endsWith(s, "}}")) {
		const s2 = Handlebars.compile(s)(scope);
		try {
			return JSON.parse(s2);
		}
		catch (e) {

		}
		return s2;
	}
	else {
		return Handlebars.compile(s)(scope);
	}
}

module.exports = {
	extractValuesFromQueryResults,
	getObjectsOfType,
	getObjectsValue,
	getVariableValue,
	handleDirective,
	handleDirectiveDeep,
	findObjectsValue,
	mutateDeep,
	mapDeep,
	renderTemplate
}
