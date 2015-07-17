var _ = require('lodash');
var assert = require('assert');
var fs = require('fs');
var naturalSort = require('javascript-natural-sort');
var path = require('path');
var yaml = require('yamljs');

var spec1 = [
    [{source: "sfGFP"}, {source: "tdGFP"}],
    [{volume: "5ul"}, {volume: "10ul"}]
];

var spec2 = [
    [{source: "sfGFP", volume: "5ul"}, {source: "tdGFP", volume: "10ul"}]
];

function gen1(spec, obj0, index, acc) {
	console.log("gen1", spec, obj0, index, acc);
	assert(_.isArray(spec));
	if (index >= spec.length) {
		acc.push(obj0);
		return acc;
	}

	var elem = spec[index];
	console.log("elem:", elem);
	if (_.isArray(elem)) {
		for (var j = 0; j < elem.length; j++) {
			var obj1 = _.merge({}, obj0, elem[j]);
			gen1(spec, obj1, index + 1, acc);
		}
	}

	return acc;
}

console.log(1);
console.log(JSON.stringify(gen1(spec1, {}, 0, []), null, '  '));

console.log(2);
console.log(JSON.stringify(gen1(spec2, {}, 0, []), null, '  '));
