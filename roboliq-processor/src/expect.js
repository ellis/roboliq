/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017, ETH Zurich, Ellis Whitehead
 * @license GPL-3.0
 */

const _ = require('lodash');
const assert = require('assert');
const expect = require('./expectCore.js');
const misc = require('./misc.js');
const wellsParser = require('./parsers/wellsParser.js');

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
