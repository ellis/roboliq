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
	//console.log("gen1", spec, obj0, index, acc);
	assert(_.isArray(spec));
	if (index >= spec.length) {
		acc.push(obj0);
		return acc;
	}

	var elem = spec[index];
	//console.log("elem:", elem);
	if (_.isArray(elem)) {
		for (var j = 0; j < elem.length; j++) {
			var obj1 = _.merge({}, obj0, elem[j]);
			gen1(spec, obj1, index + 1, acc);
		}
	}

	return acc;
}

var spec3 = {
	objects: [{1: {source: "saltwater", volumes: "40ul"}}]
};

var spec4 = {
	merge: [
		{objects: [{1: {source: "saltwater", volumes: "40ul"}}]},
		{objects: [{2: {source: "sfGFP", volumes: "5ul"}}, {2: {source: "tdGFP", volumes: "5ul"}}]}
	]
};

var spec5 = {
	merge: [
		{objects: [{1: {source: "saltwater", volumes: "40ul"}}]},
		{merge: [
			{objects: [{2: {source: "sfGFP"}}, {2: {source: "tdGFP"}}]},
			{objects: [{2: {volumes: "5ul"}}]}
		]}
	]
};

var spec6 = {
	list: [
		{objects: [{source: "saltwater", volumes: "40ul"}]},
		{merge: [
			{objects: [{source: "sfGFP"}, {source: "tdGFP"}]},
			{objects: [{volumes: "5ul"}]}
		]}
	]
};

function gen2(spec) {
	console.log("gen2", spec);
	if (spec.merge) {
		return genMerge(spec.merge);
	}
	else if (spec.list) {
		return genList(spec.list);
	}
	else if (spec.objects) {
		assert(_.isArray(spec.objects));
		return spec.objects;
		/*_.forEach(spec.objects, function(obj1) {
			var obj2 = _.merge({}, obj0, obj1);
			gen1(spec, obj1, index + 1, acc);
		}*/
	}
}

function genMerge(merges) {
	assert(_.isArray(merges));
	var lists = _.map(merges, function(elem) { return gen2(elem); });
	return gen1(lists, {}, 0, []);
}

function genList(spec) {
	assert(_.isArray(spec));
	var lists = _.map(spec, function(elem) { return gen2(elem); });
	return combineLists(lists);
}

function combineLists(elem) {
	console.log("combineLists: ", elem);
	var list = [];
	if (_.isArray(elem) && !_.isEmpty(elem)) {
		list = elem[0];
		for (var i = 1; i < elem.length; i++) {
			console.log("list@"+i, list);
			list = _(list).map(function(x) {
				console.log("x", x);
				if (!_.isArray(x))
					x = [x];
				return elem[i].map(function(y) { return x.concat(y); });
			}).flatten().value();
		}
	}
	return list;
}

function genMergex(mergeSpec, obj0, index, acc) {
	console.log("genMerge", mergeSpec, obj0, index, acc);
	assert(_.isArray(mergeSpec));
	if (index >= mergeSpec.length) {
		acc.push(obj0);
		return acc;
	}

	var elem = mergeSpec[index];
	console.log("elem:", elem);
	if (_.isArray(elem)) {
		for (var j = 0; j < elem.length; j++) {
			var obj1 = gen2(elem, obj0, acc);
			var obj1 = _.merge({}, obj0, elem[j]);
			gen1(mergeSpec, obj1, index + 1, acc);
		}
	}

	return acc;
}

console.log();
console.log(1);
console.log(JSON.stringify(gen1(spec1, {}, 0, []), null, '  '));

console.log();
console.log(2);
console.log(JSON.stringify(gen1(spec2, {}, 0, []), null, '  '));

console.log();
console.log(3);
console.log(JSON.stringify(gen2(spec3), null, '  '));

console.log();
console.log(4);
console.log(JSON.stringify(gen2(spec4), null, '  '));

console.log();
console.log(5);
console.log(JSON.stringify(gen2(spec5), null, '  '));

console.log();
console.log(6);
console.log(JSON.stringify(gen2(spec6), null, '  '));
