var _ = require('lodash');
var assert = require('assert');
var math = require('mathjs');
var misc = require('../misc.js');
var wellsParser = require('../parsers/wellsParser.js');

// TODO: analyze wells string (call wellsParser)
// TODO: zipMerge to merge destinations into mix items (but error somehow if not enough destinations)
// TODO: extractKey list of destinations from items
// TODO: extractValue list of destinations from items
// TODO: extract unique list of destinations, make it a well string?

function handleDirective(spec, data) {
	return misc.handleDirective(spec, data);
}

function directive_factorialArrays(spec, data) {
	//console.log("genList:", spec);
	assert(_.isArray(spec));
	var lists = _.map(spec, function(elem) { return handleDirective(elem, data); });
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

function directive_factorialCols(spec, data) {
    //console.log("genFactorialCols:", spec);
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
    var result = genMerge(lists, data);
    //console.log("genFactorialCols result:", result);
    return result;
}

function directive_gradient(data, data_) {
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
function directive_merge(spec, data) {
	console.log("#merge", spec);
	if (_.isEmpty(spec)) return spec;

	//console.log("genMerge lists:", lists);
	var result = genMerge2(spec, data, {}, 0, []);
	// If all elements were objects rather than arrays, return an object:
	/*if (_.every(spec, _.isObject)) {
		assert(result.length == 1);
		result = result[0];
	}*/
	//console.log("genMerge result:", result);
	return result;
}

function genMerge2(spec, data, obj0, index, acc) {
	console.log("genMerge2", spec, obj0, index, acc);
	assert(_.isArray(spec));

	var list = _.map(spec, function(elem) {
		if (!_.isArray(elem))
			elem = [elem];
		return handleDirective(elem);
	});

	if (index >= spec.length) {
		acc.push(obj0);
		return acc;
	}

	var elem = spec[index];
	if (!_.isArray(elem))
		elem = [elem];

	for (var j = 0; j < elem.length; j++) {
		var elem2 = handleDirective(elem[j], data);
		if (_.isArray(elem2)) {
			genMerge2(elem2, data, obj1, 0, acc);
		}
		else {
			assert(_.isObject(elem2));
			var obj1 = _.merge({}, obj0, elem[j]);
			genMerge2(list, data, obj1, index + 1, acc);
		}
	}

	return acc;
}

function directive_replicate(spec) {
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

function directive_tableCols(table, data) {
    //console.log("genTableCols:", table)
    assert(_.isObject(table));
	assert(!_.isEmpty(table));

	var ns1 = _.uniq(_.map(table, function(x) { return _.isArray(x) ? x.length : 1; }));
	var ns = _.filter(ns1, function(n) { return n > 1; });
	assert(ns1.length > 0);
	assert(ns.length <= 1);
	var n = (ns.length === 1) ? ns[0] : 1;

    var list = Array(n);
	for (var i = 0; i < n; i++) {
		list[i] = _.mapValues(table, function(value) {
			return (_.isArray(value)) ? value[i] : value;
		});
	}
    return list;
}

function directive_tableRows(table, data) {
    //console.log("genTableRows:", table)
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

function directive_wells(spec, data) {
	return wellsParser.parse(spec, data.objects);
}

function directive_zipMerge(spec, data) {
	assert(_.isArray(spec));
	if (spec.length <= 1)
		return spec;
	//console.log('spec:', spec);
	var zipped = _.zip.apply(null, spec);
	//console.log('zipped:', zipped);
	var merged = _.map(zipped, function(l) { return _.merge.apply(null, [{}].concat(l)); });
	//console.log('spec:', spec);
	return merged;
}

module.exports = {
	"#factorialArrays": directive_factorialArrays,
	"#factorialCols": directive_factorialCols,
	"#gradient": directive_gradient,
	"#merge": directive_merge,
	"#replicate": directive_replicate,
	"#tableCols": directive_tableCols,
	"#tableRows": directive_tableRows,
	//"#wells": genWells,
	"#zipMerge": directive_zipMerge
};
