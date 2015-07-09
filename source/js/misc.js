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

function getObjectsValue(objects, key) {
	var l = key.split('.');
	while (l.length > 1 && !_.isEmpty(objects)) {
		objects = objects[l[0]];
		l = _.rest(l);
	}
	return (_.isEmpty(objects)) ? null : objects[l[0]];
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
			if (_.isObject(o2)) {
				var prefix2 = prefix1.concat([name2]);
				_.merge(l, getObjectsOfType(o2, types, prefix2));
			}
		});
	});
	return l;
}

module.exports = {
	extractValuesFromQueryResults: extractValuesFromQueryResults,
	getObjectsOfType: getObjectsOfType,
	getObjectsValue: getObjectsValue
}
