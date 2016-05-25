/**
 * Roboliq's default directives.
 * @module config/roboliqDirectiveHandlers
 */

var _ = require('lodash');
var assert = require('assert');
var math = require('mathjs');
var random = require('random-js');
import commandHelper from '../commandHelper.js';
var Design = require('../design2.js');
var expect = require('../expect.js');
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

function directive_createWellAssignments(spec, data) {
	return expect.try("#createWellAssignments", () => {
		const parsed = commandHelper.parseParams(spec, data, {
			properties: {
				list: {type: "array"},
				wells: {type: "Wells"}
			},
			required: ["list", "wells"]
		});
		return _.take(parsed.value.wells, parsed.value.list.length);
	});
}

function directive_data(spec, data) {
	// console.log("directive_data: "+JSON.stringify(spec, null, '\t'))
	// console.log("DATA: "+JSON.stringify(data.objects.DATA, null, '\t'))
	return expect.try("#data", () => {
		let result = Design.query(data.objects.DATA, spec);
		// console.log("result0: "+JSON.stringify(result))
		if (spec.templateGroup) {
			result = _.map(result, DATA => {
				const SCOPE = _.merge({}, data.objects.SCOPE, Design.getCommonValues(DATA));
				return commandHelper.substituteDeep(spec.templateGroup, data, SCOPE, DATA);
			});
			// console.log("result1: "+JSON.stringify(result))
		}
		else if (spec.template) {
			result = _.map(result, DATA => _.map(DATA, row => {
				const SCOPE = _.merge({}, data.objects.SCOPE, row);
				return commandHelper.substituteDeep(spec.template, data, SCOPE, DATA);
			}));
			// console.log("result1: "+JSON.stringify(result))
		}
		result = (spec.groupBy) ? result : _.flatten(result);
		// console.log("result2: "+JSON.stringify(result))

		if (spec.value) {
			result = _.map(result, spec.value);
			// console.log("result3: "+JSON.stringify(result))
		}

		if (spec.head) {
			result = _.head(result);
			// console.log("result4: "+JSON.stringify(result))
		}
		if (spec.join) {
			result = result.join(spec.join);
			// console.log("result5: "+JSON.stringify(result))
		}
		if (spec.reverse) {
			result = _.reverse(result);
		}
		return result;
	});
}

function directive_destinationWells(spec, data) {
	expect.truthy({}, _.isString(spec), "#destinationWells: expected string, received "+spec);
	return directive_wells(spec, data);
}

function directive_for(spec, data) {
	expect.paramsRequired(spec, ['factors', 'output']);
	var views = directive_factorialCols(spec.factors, data);
	//console.log("views:", views);
	return _.map(views, function(view) {
		var rendered = misc.renderTemplate(spec.output, view, data);
		//console.log("rendered:", rendered);
		var rendered2 = misc.handleDirectiveDeep(rendered, data);
		//console.log("rendered2:", rendered2);
		return rendered2;
	});
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
    assert(_.isPlainObject(spec) || _.isArray(spec));
	var variables = (_.isPlainObject(spec))
		? _.toPairs(spec)
		: _(spec).map(_.toPairs).flatten().value();

    var lists = _.map(variables, function(pair) {
		var key = pair[0];
		var values = pair[1];
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
    var result = directive_factorialMerge(lists, data);
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
            l.push((volume1 > 0) ? {source: data.source1, volume: math.unit(volume1, 'ul').format({precision: 14})} : null);
        	l.push((volume2 > 0) ? {source: data.source2, volume: math.unit(volume2, 'ul').format({precision: 14})} : null);
            //if (l.length > 0)
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
function directive_factorialMerge(spec, data) {
	//console.log("#merge", spec);
	if (_.isEmpty(spec)) return spec;

	//console.log("genMerge lists:", lists);
	var result = genMerge2(spec, data, {}, 0, []);
	// If all elements were objects rather than arrays, return an object:
	/*if (_.every(spec, _.isPlainObject)) {
		assert(result.length == 1);
		result = result[0];
	}*/
	//console.log("genMerge result:", result);
	return result;
}

/**
 * Helper function for factorial merging of arrays of objects.
 * For example, the first element of the first array is merged with the first element of the second array,
 * added to the `acc` list, then the first element of the first array is merged with the second element of the second array, and so on.
 * @param  {array} spec  array of objects, may be nested arbitrarily deep, i.e. array of arrays of objects
 * @param  {object} data  [description]
 * @param  {object} obj0  accumulated result of the current merge, will be added to `acc` once the end of the `spec` list is reached
 * @param  {number} index index of current item in `spec`
 * @param  {array} acc   accumulated list of merged objects
 * @return {array} Returns a factorial list of merged objects.
 */
function genMerge2(spec, data, obj0, index, acc) {
	//console.log("genMerge2", spec, obj0, index, acc);
	assert(_.isArray(spec));

	var list = _.map(spec, function(elem) {
		if (!_.isArray(elem))
			elem = [elem];
		return handleDirective(elem, data);
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
		//console.log("elem2:", elem2)
		if (_.isArray(elem2)) {
			genMerge2(elem2, data, obj1, 0, acc);
		}
		else if (elem2 !== null) {
			assert(_.isPlainObject(elem2));
			var obj1 = _.merge({}, obj0, elem[j]);
			genMerge2(list, data, obj1, index + 1, acc);
		}
	}

	return acc;
}

//function expandArrays

function directive_factorialMixtures(spec, data) {
	// Get mixutre items
	var items;
	if (_.isArray(spec))
		items = spec;
	else {
		expect.paramsRequired(spec, ['items']);
		items = misc.getVariableValue(spec.items, data.objects);
	}

	assert(_.isArray(items));
	var items2 = _.map(items, function(item) {
		return (_.isPlainObject(item))
			? directive_factorialCols(item, data)
			: item;
	});
	var combined = combineLists(items2);

	if (spec.replicates && spec.replicates > 1) {
		combined = directive_replicate({count: spec.replicates, value: combined}, data);
	}
	return combined;
}

function directive_length(spec, data) {
	if (_.isArray(spec))
		return spec.length;
	else if (_.isString(spec)) {
		var value = misc.getObjectsValue(spec, data.objects)
		 if (_.isPlainObject(value) && value.hasOwnProperty('value'))
			value = value.value;
		expect.truthy({}, _.isArray(value), '#length expected an array, received: '+spec);
		return value.length;
	}
	else {
		expect.truthy({}, false, '#length expected an array, received: '+spec);
	}
}

function directive_merge(spec, data) {
	assert(_.isArray(spec));
	var list = _.map(spec, function(x) { return handleDirective(x, data); });
	return _.merge.apply(null, [{}].concat(list));
}

function directive_pipetteMixtures(spec, data) {
	// Get mixutre items
	var items;
	if (_.isArray(spec))
		items = spec;
	else {
		expect.paramsRequired(spec, ['items']);
		items = misc.getVariableValue(spec.items, data.objects);
	}

	assert(_.isArray(items));
	var items2 = _.map(items, function(item) {
		return (_.isPlainObject(item))
			? directive_factorialCols(item, data)
			: item;
	});
	var combined = combineLists(items2);

	if (spec.replicates && spec.replicates > 1) {
		combined = directive_replicate({count: spec.replicates, value: combined}, data);
	}

	//console.log(1)
	//console.log(JSON.stringify(combined, null, '\t'))

	// Check and set volumes
	const volumePerMixture = (spec.volume)
		? math.eval(spec.volume) : undefined;
	//console.log({volumePerMixture})
	//console.log({combined})
	combined.forEach(l => {
		let volumeTotal = math.unit(0, 'l');
		let missingVolumeIndex = -1;
		//console.log({l})
		// Find total volume of components and whether any components are missing the volume parameter
		_.forEach(l, (x, i) => {
			//console.log({x, i, and: x.hasOwnProperty('volume')})
			if (x) {
				if (!x.hasOwnProperty('volume')) {
					assert(volumePerMixture, "missing volume parameter: "+JSON.stringify(x));
					assert(missingVolumeIndex < 0, "only one mixture element may omit the volume parameter: "+JSON.stringify(l));
					missingVolumeIndex = i;
					//console.log({missingVolumeIndex})
				}
				else {
					volumeTotal = math.add(volumeTotal, math.eval(x.volume));
				}
			}
		});
		// If one of the components needs to have its volume set:
		if (missingVolumeIndex >= 0) {
			assert(volumePerMixture);
			//console.log({volumePerMixture, volumeTotal, subtract: math.subtract(volumePerMixture, volumeTotal).format({precision: 10})})
			l[missingVolumeIndex] = _.merge({}, l[missingVolumeIndex], {
				volume: math.subtract(volumePerMixture, volumeTotal).format({precision: 13})
			});
		}
		else if (!_.isUndefined(volumePerMixture)) {
			const t1 = volumePerMixture.format({precision: 14});
			const t2 = volumeTotal.format({precision: 14});
			if (t1 !== t2) {
				console.log("WARNING: volume in mixture should sum to "+t1+" rather than "+t2+": "+JSON.stringify(l));
			}
		}
	});

	//console.log(2)
	//console.log(JSON.stringify(combined, null, '\t'))

	_.forEach(spec.transformations || [], t => {
		if (t.name === 'shuffle') {
			var mt = random.engines.mt19937();
			assert(_.isNumber(t.seed), "`shuffle` requires a numeric `seed` paramter");
			mt.seed(t.seed);
			random.shuffle(mt, combined);
		}
	});

	//console.log(3)
	//console.log(JSON.stringify(combined, null, '\t'))

	_.forEach(spec.transformationsPerWell || [], t => {
		if (t.name === 'sortByVolumeMax') {
			combined = _.map(combined, l => {
				const offset = 0;
				const count = t.count || l.length - offset;
				return _.take(l, offset)
					.concat(_.sortBy(_.take(l, count), x => -math.eval(x.volume).toNumber('l')))
					.concat(_.drop(l, offset + count));
			});
		}
	});

	//console.log(4)
	//console.log(JSON.stringify(combined, null, '\t'))

	return combined;
}

function directive_replaceLabware(spec, data) {
	expect.paramsRequired(spec, ['list', 'new']);
	var list = misc.getVariableValue(spec.list, data.objects);
	//if (!_.isArray(list)) console.log("list:", list)
	assert(_.isArray(list), "expected a list, received: "+JSON.stringify(list));
	assert(_.isString(spec.new));
	var l1 = _.flatten(_.map(list, function(s) {
		var l2 = wellsParser.parse(s);
		return _.map(l2, function(x) {
			//console.log("s:", s, "x:", x);
			//console.log(x.hasOwnProperty('labware'), !spec.old, x.labware === spec.old);
			if (x.hasOwnProperty('labware') && (!spec.old || x.labware === spec.old)) {
				x.labware = spec.new;
			}
			return x;
		});
	}));
	var l2 = wellsParser.processParserResult(l1, data.objects);
	return l2;
}

function directive_replicate(spec) {
    assert(_.isPlainObject(spec));
    assert(_.isNumber(spec.count));
	assert(spec.value);
	var depth = spec.depth || 0;
	assert(depth >= 0);

	var step = function(x, count, depth) {
		//console.log("step:", x, count, depth);
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
    assert(_.isPlainObject(table));
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
            assert(_.isPlainObject(row));
            defaults = _.merge(defaults, row);
            //console.log("defaults:", defaults)
        }
    });
    return list;
}

function directive_take(spec, data) {
	//console.log("#take:", spec);
	expect.paramsRequired(spec, ['list', 'count']);
	var list = misc.getVariableValue(spec.list, data.objects);
	var count = misc.getVariableValue(spec.count, data.objects);
	return _.take(list, count);
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
	"#createPipetteMixtureList": directive_pipetteMixtures,
	"#createWellAssignments": directive_createWellAssignments,
	"#data": directive_data,
	"#destinationWells": directive_destinationWells,
	"#factorialArrays": directive_factorialArrays,
	"#factorialCols": directive_factorialCols,
	"#factorialMerge": directive_factorialMerge,
	"#factorialMixtures": directive_factorialMixtures,
	"#for": directive_for,
	"#gradient": directive_gradient,
	"#length": directive_length,
	"#merge": directive_merge,
	"#replaceLabware": directive_replaceLabware,
	"#replicate": directive_replicate,
	"#tableCols": directive_tableCols,
	"#tableRows": directive_tableRows,
	"#take": directive_take,
	//"#wells": genWells,
	"#undefined": function() { return undefined; },
	"#zipMerge": directive_zipMerge
};
