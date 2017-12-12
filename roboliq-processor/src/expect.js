/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017 Ellis Whitehead
 * @license GPL-3.0
 */

/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017 Ellis Whitehead
 * @license GPL-3.0
 */

var _ = require('lodash');
var assert = require('assert');
var expect = require('./expectCore.js');
var misc = require('./misc.js');
var wellsParser = require('./parsers/wellsParser.js');

function objectsValue(context, key, objects, effects, prefix) {
	var value = misc.findObjectsValue(key, objects, effects, undefined, prefix);
	if (_.isUndefined(value)) {
		var id = (prefix) ? prefix+"."+key : key;
		//console.trace();
		expect.throw(_.defaults({objectName: id}, context), "missing value.");
	}
	return value;
}

function wells(context, value, data) {
	assert(value);
	assert(data);
	assert(data.objects);
	var destinations = expect.try(context, function () {
		//console.dir(wellsParser.parse);
		if (_.isString(value))
			return wellsParser.parse(value, data.objects);
		else
			return value;
	});
	return destinations;
}

module.exports = _.merge(expect, {
	destinationWells: wells,
	objectsValue: objectsValue,
	sourceWells: wells,
});
