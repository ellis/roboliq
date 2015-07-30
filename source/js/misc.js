var _ = require('lodash');

/**
 * queryResults: value returned from llpl.query()
 * predicateName: name of the predicate that was used for the query
 * return: {parameterName1: parameterValues1, ...}
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
	if (effects) {
		var id = (prefix) ? prefix+"."+key : key;
		if (effects.hasOwnProperty(id))
			return effects[id];
	}
	var l = key.split('.');
	for (var i = 0; !_.isEmpty(objects) && i < l.length; i++) {
		if (!objects.hasOwnProperty(l[i])) {
			var valueName = _.take(l, i + 1).join('.');
			if (prefix) valueName = prefix + '.' + valueName;
			var message = "value `"+valueName+"`: undefined";
			//console.log(message);
			throw new Error(message);//{name: "ProcessingError", errors: [message]};
		}
		objects = objects[l[i]];
	}
	return objects;
}

function getObjectsOfType(objects, types, prefix) {
	if (_.isString(types)) types = [types];
	if (!prefix) prefix = [];

	var l = {};
	_.forEach(objects, function(o, name) {
		var prefix1 = prefix.concat([name]);
		if (_.isString(o.type) && types.indexOf(o.type) >= 0) {
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
	if (_.isPlainObject(spec)) {
		for (var key in spec) {
			if (data.directiveHandlers.hasOwnProperty(key)) {
				var spec2 = spec[key];
				var spec3 = handleDirective(spec2, data);
				return data.directiveHandlers[key](spec3, data);
			}
		}
	}
	else if (_.isString(spec) && _.startsWith(spec, "#")) {
		var hash2 = spec.indexOf('#', 1);
		if (hash2 > 0) {
			key = spec.substr(0, hash2);
			if (data.directiveHandlers.hasOwnProperty(key)) {
				var spec2 = spec.substr(hash2 + 1);
				var spec3 = handleDirective(spec2, data);
				return data.directiveHandlers[key](spec3, data);
			}
		}
	}
	return spec;
}

/**
 * Recurses into object properties and replaces them with the result of handleDirective.
 *
 * @param  {Any} spec Any value.  If this is a directive, it will be an object with a single key that starts with '#'.
 * @param  {Object} data An object with properties: directiveHandlers, objects, events.
 * @return {Any} Return the object, or if it was a directive, the results of the directive handler.
 */
function handleDirectiveDeep(spec, data) {
	spec = handleDirective(spec, data);
	if (_.isPlainObject(spec)) {
		spec = _.mapValues(spec, function(value) {
			return handleDirectiveDeep(value, data);
		});
	}
	else if (_.isArray(spec)) {
		spec = _.map(spec, function(value) {
			return handleDirectiveDeep(value, data);
		});
	}
	return spec;
}

module.exports = {
	extractValuesFromQueryResults: extractValuesFromQueryResults,
	getObjectsOfType: getObjectsOfType,
	getObjectsValue: getObjectsValue,
	handleDirective: handleDirective,
	handleDirectiveDeep: handleDirectiveDeep,
	findObjectsValue: findObjectsValue
}
