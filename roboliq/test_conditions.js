import _ from 'lodash';
import assert from 'assert';
import Immutable, {Map, fromJS} from 'immutable';
import yaml from 'yamljs';

/*
 * What's the impact of:
 * - evaporation
 * - number of sealing layers
 * - sampling (removing content from wells)
 * - position in shaker
 *
 * The interactions among the factors are complex:
 * - sampling causes a seal puncture, which leads to accelerated evaporation
 * - but if we don't sample all wells at the same time, it's much more difficult to compare readouts at the end
 * - the experiments take a long time (48h) so if we're going to re-test conditions using swapped shaker positions, it going to take a long time
 * - we could possible minimize the effect of position by swapping positions frequently, but this seems complex
 * - each sealing layer reduces air exchange, so if we add another seal layer after puncturing in order to minimize evaporation, we've change the conditions
 * - could we inject more media to compensate for evaporation?  If so, which effects would this have?
 *
 * Additional confounding factors:
 * - position of well
 * - syringe used
 * - frequency and duration of stopping incubator to perform operations
 *
 * Design ideas for isolating evaporation:
 * - puncture wells at various time points; measure all wells at 48h; model could be $y \sim f(48 - t1)$; restricted to 8 wells.
 * - puncture wells at various time points and measure; measure all wells at 48h; model could be $y \sim f(t1, y1)$ or $y/y1 \sim f(t1)$; restricted to 8 wells.
 * - puncture wells at various time points; measures wells later; model: $y_t \sim dt$, where $y_t$ are all measurements at a given time, $dt$ is time since puncture.
 *
 * Design ideas for isolating site effects:
 * - use 2, 8, or 16 plates with the same conditions; just interleave their processing
 *
 * Design ideas for isolating well position effects:
 * - randomize wells and see whether col/row have an effect
 *
 * Design ideas for isolating sampling:
 * - pre-puncture all wells before incubation; assign wells to be sampled 1, 2, 3, or 4 times, where the last timepoint is the same for all
 * - dispense media, seal;
 * - puncture wells at various time points before 24h and measure; measure again 24h later; model is $y - y1 \sim f(evaporationTime)$
 * - puncture wells at various time points and measure or not; measures wells 12hr later;
 *     model 1) $y \sim x$, where $x$ is 1 if first puncture also involved measurement (0 otherwise)
 *
 * Design ideas for number of sealing layers:
 * -
 * Design ideas for general experiment:
 * - sample wells at various time points; measure again 12hr later (if we think 12hr is ok for evaporation); 4 wells have first measurement, 4 have second measurement for each time period;
 *     model 1) $y_i^{(t)} \sim k_i$, where $y_i^{(t)}$ are the measurements at a given time point, and $k$ is 1 for the first measurement and 2 for the second measurement.  This helps us assess the impact of evaporation+priorsampling; if we already quantified the impact of 12hr evaporation, then we may be able to extract the impact of prior sampling.
 *     model 2) ...
 * - puncture wells at various time points and measure or not; measures wells later;
 *     model 1) $y_t \sim x + dt$, where $y_t$ are all measurements at a given time, $x$ is 1 if first puncture also involved measurement (0 otherwise), $dt$ is time since puncture.
 *     model 2) $y \sim f(t, x, dt)$, full model
 *
 * More details for the first general design:
 * - measure every 15 minutes (=> 12hr = 48 sample times)
 * - for the first 48 sample times, sample 1 well
 * - for the next 48 sample times, resample each well from before and 1 new well
 * - for the next 48 sample times, same as previous cycle
 * - for the last 48 sample times, resample each well from before
 */

const design = {
	conditions: {
		"strainSource": "strain1",
		"mediaSource": "media1",
		"cultureWellIndex*": _.range(1, 4+1),
		//"sample1*": [false, true],
		//"sample1Cycle*": _.range(0, 4),
		//"dilution*": [1, 2]
	},
};

const design2 = {
	conditions: design.conditions,
	processing: [
		{}
	],
	replicates: {
		replicate: {
			count: 2
		}
	},
	assign: {
		cultureWell: {
			values: _.range(1, 96+1),
			random: true
		},
		sample1Cycle: {
			values: _.range(1, 4+1),
			random: true
		},
		sample2Cycle: {
			calculate: (row) => (row.sample1Cycle + 1)
		}
		/*order: {
			index: true,
			random: true
		}*/
	}
};

function printConditions(conditions) {
	console.log(yaml.stringify(conditions, 6, 2));
}

function printData(data) {
	// Get column names
	const columnMap = {};
	_.forEach(table, row => _.forEach(_.keys(row), key => { columnMap[key] = true; } ));
	const columns = _.keys(columnMap);
	// console.log({columns})

	// Convert data to array of lines (which are arrays of columns)
	const lines = [];
	_.forEach(data, group => {
		if (!_.isArray(group)) {
			group = [group];
		}
		else {
			lines.push(["---"]);
		}
		_.forEach(group, row => {
			const line = _.map(columns, key => _.get(row, key, ""));
			// TODO: have option to blank columns that are the same as the column of the previous row
			lines.push(line);
		});
	});

	// Calculate column widths
	const widths = _.map(columns, key => key.length);
	// console.log({widths})
	_.forEach(lines, line => {
		_.forEach(line, (s, i) => { if (!_.isEmpty(s)) widths[i] = Math.max(widths[i], s.length); });
	});
	// console.log({widths})

	console.log(columns.map((s, i) => _.padEnd(s, widths[i])).join("  "));
	console.log(columns.map((s, i) => _.repeat("=", widths[i])).join("  "));
	_.forEach(lines, (line, lineIndex) => {
		const s = line.map((s, i) => _.padEnd(s, widths[i])).join("  ");
		console.log(s);
	});
}
// Consider: select, groupBy, orderBy, unique

function flattenConditions(conditions, depth = -1) {
	assert(_.isPlainObject(conditions));
	let flatter = [conditions];
	let again = true;
	while (again && depth != 0) {
		//console.log({depth})
		again = false;
		flatter = _.flatMap(flatter, (row) => {
			//console.log({row})
			assert(_.isPlainObject(row));
			let rows = [{}];
			_.forEach(row, (value, key) => {
				//console.log({key, value})
				if (depth != 0 && _.endsWith(key, "*")) {
					again = true;
					const key2 = key.substring(0, key.length - 1);
					// For each entry in value, make a copy of every row in rows with the properties of the entry
					rows = _.flatMap(rows, x => {
						return _.map(value, (value3, key3) => {
							//console.log({key3, value3})
							if (_.isPlainObject(value3)) {
								const value2 = (_.isNumber(key3)) ? key3 + 1 : key3;
								return _.merge({}, x, _.fromPairs([[key2, value2]]), value3);
							}
							else {
								return _.merge({}, x, _.fromPairs([[key2, value3]]));
							}
						});
					});
				}
				else {
					_.forEach(rows, row => { row[key] = value; });
					/*if (key === "reseal") {
						console.log("reseal: "+value)
						console.log(rows)
					}*/
				}
			});
			//console.log({rows})
			return rows;
		});
		if (depth > 0)
			depth--;
	}

	return flatter;
}

function query(table, q) {
	let table2 = _.clone(table);
	if (q.select) {
		table2 = _.map(table2, x => _.pick(x, q.select));
	}

	if (q.where) {
		_.forEach(q.where, (value, key) => {
			table2 = _.filter(table, row => _.isEqual(row[key], value));
		});
	}

	if (q.uniqueBy) {
		const groupKeys = (_.isArray(q.uniqueBy)) ? q.uniqueBy : [q.uniqueBy];
		const groups = _.map(_.groupBy(table2, row => _.map(groupKeys, key => row[key])), _.identity);
		//console.log({groupsLength: groups.length})
		table2 = _.flatMap(groups, group => {
			const first = group[0];
			// Find the properties that are the same for all items in the group
			const uniqueKeys = [];
			_.forEach(first, (value, key) => {
				const isUnique = _.every(group, row => _.isEqual(row[key], value));
				if (isUnique) {
					uniqueKeys.push(key);
				}
			});
			return _.pick(first, uniqueKeys);
		});
	}
	else if (q.unique) {
		table2 = _.uniqWith(table2, _.isEqual);
	}

	if (q.groupBy) {
		const groupKeys = (_.isArray(q.groupBy)) ? q.groupBy : [q.groupBy];
		table2 = _.map(_.groupBy(table2, row => _.map(groupKeys, key => row[key])), _.identity);
	}
	else {
		table2 = [table2];
	}

	return table2;
}

function flattenDesign(design) {
	const table = flattenConditions(design.conditions);

	//console.log({assign: design.assign})
	_.forEach(design.assign, (x, name) => {
		//console.log({name, x})
		if (_.isArray(x.values)) {
			const values = _.shuffle(x.values);
			_.forEach(table, (row, i) => {
				row[name] = values[i];
			});
		}
		else if (_.isFunction(x.calculate)) {
			_.forEach(table, row => {
				row[name] = x.calculate(row);
			});
		}
	});

	return table;
}

const table = flattenDesign(design2);
printConditions(design.conditions);
printData(table);
//console.log(yaml.stringify(table, 4, 2))

let x;
//x = query(table, {select: "culturePlate"});
//x = query(table, {select: "culturePlate", groupBy: "culturePlate"});
//x = query(table, {select: "culturePlate", unique: true, groupBy: "culturePlate"});
//x = query(table, {select: ["culturePlate", "syringe", "cultureWell"], unique: true, groupBy: "culturePlate"});
//x = query(table, {select: ["culturePlate", "syringe", "cultureWell", "strain", "strainVolume", "media", "mediaVolume"], unique: true, groupBy: "culturePlate"});
//x = query(table, {uniqueBy: ["culturePlate", "cultureWell"]});
//x = query(table, {where: {dilutionFactor: 1}});
//console.log(yaml.stringify(x, 4, 2))

function appendStep(steps, step) {
	//console.log({steps, size: _.size(steps), keys: _.keys(steps)})
	const substeps = _.pickBy(steps, (x, key) => /^[0-9]+$/.test(key));
	steps[_.size(substeps)+1] = step;
	return step;
}

function narrow(scope, data, q, fn) {
	const groups = _.isPlainObject(q) ? query(data, q) : [data];
	_.forEach(groups, group => {
		if (group.length > 0) {
			const first = _.head(group);
			// Find the properties that are the same for all items in the group
			const uniqueKeys = [];
			_.forEach(first, (value, key) => {
				const isUnique = _.every(group, row => _.isEqual(row[key], value));
				if (isUnique) {
					uniqueKeys.push(key);
				}
				// FIXME: for debug only
				else {
					//if (key == "reseal") console.log(`not unique ${key}: ${value}, ${_.map(group, x => x[key]).join(",")}`)
				}
				// ENDFIX
			});
			//console.log({uniqueKeys: uniqueKeys.join(",")})
			let scope2 = scope;
			// Add those properties to the scope
			_.forEach(uniqueKeys, key => {
				scope2 = scope2.set(key, fromJS(first[key]));
			});
			fn(scope2, group);
		}
	});
}

function mapConditions(scope, data, q, flatten = 1, fn) {
	let result = [];
	narrow(scope, data, q, (scope2, data2) => {
		const l = _.map(data2, row => {
			const scope3 = scope2.merge(fromJS(row));
			return fn(scope3);
		});
		result.push(l);
	});
	for (let i = 0; i < flatten; i++) {
		result = _.flatten(result);
	}
	return result;
}

function mapConditionGroups(scope, data, q, flatten = 1, fn) {
	let result = [];
	narrow(scope, data, q, (scope2, data2) => {
		//console.log({scope2, data2})
		//process.exit(-1);
		result.push(fn(scope2, data2));
	});
	for (let i = 0; i < flatten; i++) {
		result = _.flatten(result);
	}
	return result;
}

function test() {
	const steps = {};

	let culturePlateIndex = 0;
	narrow(Map(), table, {groupBy: ["roma", "vector"]}, (scope, data) => {
		const data1 = _.shuffle(data);
		// console.log("A")
		// console.log({scope, data})

		const stepRomaVector = {comment: `Test plate movements for ${scope.get("roma")} with vector ${scope.get("vector")}`};

		narrow(scope, data1, {groupBy: "site"}, (scope, data) => {
			// console.log("C")
			// console.log({scope, data})
			appendStep(stepRomaVector, {
				command: "transporter.movePlate",
				equipment: scope.get("roma"),
				program: scope.get("vector"),
				object: scope.get("plate"),
				destination: scope.get("site")
			});
		});

		appendStep(stepRomaVector, {
			command: "transporter.movePlate",
			equipment: scope.get("roma"),
			program: scope.get("vector"),
			object: scope.get("plate"),
			destination: scope.get("storageSite")
		});

		appendStep(steps, stepRomaVector);
	});

	const protocol = {
		roboliq: "v1",
		objects: {
			plateDWP: {type: "Plate", model: "ourlab.model.plateModel_96_dwp", location: "ourlab.mario.site.REGRIP"},
			//plateNunc: {type: "Plate", model: "ourlab.model.plateModel_96_square_transparent_nunc", location:
		},
		steps
	};

	return protocol;
}

/*const protocol = test();

// If run from the command line:
if (require.main === module) {
	//console.log(yaml.stringify(protocol, 9, 2));
	console.log(JSON.stringify(protocol, null, '\t'))
}
else {
	module.exports = protocol;
}
*/
