var _ = require('lodash');
var assert = require('assert');
var expect = require('./expectCore.js');
var wellsParser = require('./parsers/wellsParser.js');

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
	sourceWells: wells,
});
