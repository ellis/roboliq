var _ = require('lodash');
var assert = require('assert');
var fs = require('fs');
var math = require('mathjs');
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
		{"#merge": [
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

/*
- gradientSources1: [hepes_850, pipes_750, mes_710, acetate_575]
  gradientSources2: [hepes_650, pipes_575, mes_510, acetate_375]
  counts: [5, 5, 7, 8]
  volume: 30ul
*/

var spec9 = {"#table": [
    {volume: '30ul'},
    ['source1',     'source2',     'count'],
    ['hepes_850',   'hepes_650',   5],
    ['pipes_750',   'pipes_575',   5],
    ['mes_710',     'mes_510',     7],
    ['acetate_575', 'acetate_375', 8]
]};

var spec10 = {
	"#combinations": [
		{source: "saltwater", volume: "40ul"},
        {"#gradient": {"#table": [
            {volume: '30ul', decimals: 1},
            ['source1',     'source2',     'count'],
            ['hepes_850',   'hepes_650',   5],
            /*['pipes_750',   'pipes_575',   5],
            ['mes_710',     'mes_510',     7],
            ['acetate_575', 'acetate_375', 8]*/
        ]}},
        {"#expand": {
            source: ["sfGFP", "tdGFP"],
		    volume: "5ul"
		}}
	]
};

var spec11a = {
    "#replicate": {
        count: 2,
        value: [
            {a: 1},
            {a: 2}
        ]
    }
};
var spec11b = {
    "#replicate": {
        count: 2,
        depth: 1,
        value: [
            {a: 1},
            {a: 2}
        ]
    }
};

var spec12 = {
	"#zipMerge": [
		[{a: 1}, {a: 2}],
		[{b: 1}, {b: 2}]
	]
};

var spec13a = {
	"#merge": [
		{a: 1},
		{b: 2},
		{c: 3}
	]
};
var spec13b = {
	"#merge": [
		[{a: 0}, {a: 1}],
		{b: 2},
		{c: 3}
	]
};

// TODO: analyze wells string (call wellsParser)
// TODO: zipMerge to merge destinations into mix items (but error somehow if not enough destinations)
// TODO: extractKey list of destinations from items
// TODO: extractValue list of destinations from items
// TODO: extract unique list of destinations, make it a well string?

var genFunctions = {
	"#combinations": genList,
	"#expand": genExpand,
	"#gradient": genGradient,
	"#merge": genMerge,
	"#replicate": genReplicate,
	"#table": genTable,
	//"#wells": genWells,
	"#zipMerge": genZipMerge
}

function gen2(spec) {
	if (_.isObject(spec)) {
		for (var key in spec) {
			if (genFunctions.hasOwnProperty(key)) {
				var spec2 = spec[key];
				var spec3 = gen2(spec2);
				return genFunctions[key](spec3);
			}
		}
	}
	return spec;
}

function genList(spec) {
	//console.log("genList:", spec);
	assert(_.isArray(spec));
	var lists = _.map(spec, function(elem) { return gen2(elem); });
	return combineLists(lists);
}

function combineLists(elem) {
	//console.log("combineLists: ", elem);
	var list = [];
	if (_.isArray(elem) && !_.isEmpty(elem)) {
		list = elem[0];
		if (!_.isArray(list))
			list = [list];
		for (var i = 1; i < elem.length; i++) {
			//console.log("list@"+i, list);
			list = _(list).map(function(x) {
				//console.log("x", x);
				if (!_.isArray(x))
					x = [x];
				return elem[i].map(function(y) { return x.concat(y); });
			}).flatten().value();
		}
	}
	return list;
}

function genExpand(spec) {
    //console.log("genExpand:", spec);
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
    //console.log("genExpand result:", result);
    return result;
}

function genGradient(data) {
    if (!_.isArray(data))
        data = [data];

    var list = [];
    _.forEach(data, function(data) {
        assert(data.volume);
        assert(data.count);
        assert(data.count >= 2);
        var decimals = _.isNumber(data.decimals) ? data.decimals : 2;
        //console.log("decimals:", decimals);
        var volumeTotal = math.round(math.eval(data.volume).toNumber('ul'), decimals);
        // Pick volumes
        var volumes = Array(data.count);
        for (var i = 0; i < data.count / 2; i++) {
            volumes[i] = math.round(volumeTotal * i / (data.count - 1), decimals);
            volumes[data.count - i - 1] = math.round(volumeTotal - volumes[i], decimals);
        }
        if ((data.count % 2) === 1)
            volumes[Math.floor(data.count / 2)] = math.round(volumeTotal / 2, decimals);
        //console.log("volumes:", volumes);
        // Create items
        for (var i = 0; i < data.count; i++) {
            var volume2 = volumes[i];
            var volume1 = math.round(volumeTotal - volume2, decimals);
            var l = [];
            if (volume1 > 0)
                l.push({source: data.source1, volume: math.unit(volume1, 'ul').format({precision: 14})});
            if (volume2 > 0)
                l.push({source: data.source2, volume: math.unit(volume2, 'ul').format({precision: 14})});
            if (l.length > 0)
                list.push(l);
        }
    });
    return list;
}

/**
 * Merge an array of objects, or combinatorially merge an array of arrays of objects.
 *
 * @param  {Array} spec The array of objects or arrays of objects to merge.
 * @return {Object|Array} The object or array resulting from combinatorial merging.
 */
function genMerge(spec) {
	console.log("genMerge", spec);
	if (_.isEmpty(spec)) return spec;

	//console.log("genMerge lists:", lists);
	var result = genMerge2(spec, {}, 0, []);
	// If all elements were objects rather than arrays, return an object:
	/*if (_.every(spec, _.isObject)) {
		assert(result.length == 1);
		result = result[0];
	}*/
	//console.log("genMerge result:", result);
	return result;
}

function genMerge2(spec, obj0, index, acc) {
	console.log("genMerge2", spec, obj0, index, acc);
	assert(_.isArray(spec));

	var list = _.map(spec, function(elem) {
		if (!_.isArray(elem))
			elem = [elem];
		return gen2(elem);
	});

	if (index >= spec.length) {
		acc.push(obj0);
		return acc;
	}

	var elem = spec[index];
	if (!_.isArray(elem))
		elem = [elem];

	for (var j = 0; j < elem.length; j++) {
		var elem2 = gen2(elem[j]);
		if (_.isArray(elem2)) {
			genMerge2(elem2, obj1, 0, acc);
		}
		else {
			assert(_.isObject(elem2));
			var obj1 = _.merge({}, obj0, elem[j]);
			genMerge2(list, obj1, index + 1, acc);
		}
	}

	return acc;
}

function genReplicate(spec) {
    assert(_.isObject(spec));
    assert(_.isNumber(spec.count));
	assert(spec.value);
	var depth = spec.depth || 0;
	assert(depth >= 0);

	var step = function(x, count, depth) {
		console.log("step:", x, count, depth);
		if (_.isArray(x) && depth > 0) {
			if (depth === 1)
				return _.flatten(_.map(x, function(y) { return step(y, count, depth - 1); }));
			else
				return _.map(x, function(y) { return step(y, count, depth - 1); });
		}
		else {
			return _.flatten(_.fill(Array(count), x));
		}
	}
	return step(spec.value, spec.count, depth);
}

function genTable(table) {
    //console.log("genTable:", table)
    assert(_.isArray(table));

    var list = [];
    var names = [];
    var defaults = {};
    _.forEach(table, function(row) {
        // Object are for default values that will apply to the following rows
        if (_.isArray(row)) {
            // First array in table is the column names
            if (names.length === 0) {
                names = row;
            }
            else {
                assert(row.length === names.length);
                var obj = _.clone(defaults);
                for (var i = 0; i < names.length; i++) {
                    obj[names[i]] = row[i];
                }
                list.push(obj);
            }
        }
        else {
            assert(_.isObject(row));
            defaults = _.merge(defaults, row);
            //console.log("defaults:", defaults)
        }
    });
    return list;
}

function genWells(spec) {
	assert(false);
	// TODO: need protocol.objects for the call to wellsParser.parse(spec, objects)
}

function genZipMerge(spec) {
	assert(_.isArray(spec));
	if (spec.length <= 1)
		return spec;
	console.log('spec:', spec);
	var zipped = _.zip.apply(null, spec);
	console.log('zipped:', zipped);
	var merged = _.map(zipped, function(l) { return _.merge.apply(null, [{}].concat(l)); });
	//console.log('spec:', spec);
	return merged;
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

console.log();
console.log(9);
console.log(JSON.stringify(gen2(spec9), null, '  '));

console.log(); console.log(10); console.log(JSON.stringify(gen2(spec10), null, '  '));
console.log(); console.log("11a"); console.log(JSON.stringify(gen2(spec11a), null, '  '));
console.log(); console.log("11b"); console.log(JSON.stringify(gen2(spec11b), null, '  '));
console.log(); console.log(12); console.log(JSON.stringify(gen2(spec12), null, '  '));
console.log(); console.log("13a"); console.log(JSON.stringify(gen2(spec13a), null, '  '));
console.log(); console.log("13b"); console.log(JSON.stringify(gen2(spec13b), null, '  '));
