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
	"#combinations": [
		{source: "saltwater", volume: "40ul"},
		{"#combinationsAndMerge": [
			[{source: "sfGFP"}, {source: "tdGFP"}],
			{volume: "5ul"}
		]}
	]
};

var spec7 = {
	type: "#combinations",
    lists: [
		{source: "saltwater", volume: "40ul"},
		{
            type: "#combinations",
            lists: [
    			[{source: "sfGFP"}, {source: "tdGFP"}],
    			{volume: "5ul"}
    		],
            map: "#merge"
        }
	]
};

var spec8 = {
	"#combinations": [
		{source: "saltwater", volume: "40ul"},
        {"#expand": {
            source: ["sfGFP", "tdGFP"],
		    volume: "5ul"
		}}
	]
};

function gen2(spec) {
	console.log("gen2", spec);
    if (spec.hasOwnProperty("#combinations")) {
		return genList(spec["#combinations"]);
	}
	else if (spec.hasOwnProperty("#combinationsAndMerge")) {
		return genMerge(spec["#combinationsAndMerge"]);
	}
    else if (spec.hasOwnProperty("#expand")) {
		return genExpand(spec["#expand"]);
	}
	else {
		return spec;
	}
}

function genMerge(merges) {
	assert(_.isArray(merges));
	var lists = _.map(merges, function(elem) {
		if (!_.isArray(elem))
			elem = [elem];
		return gen2(elem);
	});
	console.log("genMerge lists:", lists);
	var result = gen1(lists, {}, 0, []);
	console.log("genMerge result:", result);
	return result;
}

function genList(spec) {
	console.log("genList:", spec);
	assert(_.isArray(spec));
	var lists = _.map(spec, function(elem) { return gen2(elem); });
	return combineLists(lists);
}

function combineLists(elem) {
	console.log("combineLists: ", elem);
	var list = [];
	if (_.isArray(elem) && !_.isEmpty(elem)) {
		list = elem[0];
		if (!_.isArray(list))
			list = [list];
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

function genExpand(spec) {
    console.log("genExpand:", spec);
    assert(_.isObject(spec));
    var lists = _.map(spec, function(values, key) {
        if (!_.isArray(values)) {
            var obj1 = {};
            obj1[key] = values;
            return [obj1];
        }
        else {
            return values.map(function(value) {
                var obj1 = {};
                obj1[key] = value;
                return obj1;
            });
        }
    })
    var result = gen1(lists, {}, 0, []);
    console.log("genExpand result:", result);
    return result;
}

/*console.log();
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
*/

console.log();
console.log(6);
console.log(JSON.stringify(gen2(spec6), null, '  '));

console.log();
console.log(8);
console.log(JSON.stringify(gen2(spec8), null, '  '));
