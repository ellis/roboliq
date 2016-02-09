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

const design2 = {
	conditions: {
		"strainSource": "strain1",
		"mediaSource": "media1",
		//"cultureNum*": _.range(1, 4+1),
		"cultureNum*=": {
			action: "range",
			till: 4
		},
		"cultureWell=": {
			action: "range",
			till: 96,
			random: true
		},
		"sampleNum": 1,
		"sampleCycle=": {
			action: "range",
			random: true
		},/*
		"sampleNum**": {
			template: {
				"sampleNum": 2,
				"sampleCycle$": "sampleCycle+1"
			}
		}
		*/
	},
	// TODO: add cultureRow, cultureCol, syringe, randomSeed, dilution plates
	// TODO: in assigning dilution plates, it would be better to alternate each cycle, but select minimum number of plates
	actions: [
		/*{
			action: "replicate",
			map: (row) => (_.merge({}, row, {sampleNum: 2, sampleCycle: row.sampleCycle + 1}))
		},
		{
			action: "add",
			object: {"dilution*": [1, 2]}
		},
		// TODO: it would be better to alternate, so 1) pick number of plates required, 2) shuffle plates, 3) orderBy sampleCycle, 4) groupBy sampleCycle, 5) assign to groups with recycling
		{
			action: "assignPlates",
			name: "dilutionPlate",
			plates: ["dilutionPlate1", "dilutionPlate2"],
			wellsPerPlate: 8,
			orderBy: "sampleCycle",
			groupBy: "sampleCycle",
		},
		{
			action: "assign",
			name: "dilutionWell",
			values: _.range(1, 96+1),
			groupBy: "dilutionPlate",
			random: true
		}*/
	]
};

function printConditions(conditions) {
	console.log(yaml.stringify(conditions, 6, 2));
}

function printData(data, hideRedundancies = false) {
	// Get column names
	const columnMap = {};
	_.forEach(data, row => _.forEach(_.keys(row), key => { columnMap[key] = true; } ));
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
	let linePrev;
	_.forEach(lines, line => {
		const s = line.map((s, i) => {
			const s2 = (s === "") ? "-"
				: (hideRedundancies && linePrev && s === linePrev[i]) ? ""
				: s;
			return _.padEnd(s2, widths[i]);
		}).join("  ");
		console.log(s);
		linePrev = line;
	});
	console.log(columns.map((s, i) => _.repeat("=", widths[i])).join("  "));
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
				if (depth != 0 && _.endsWith(key, "*")) {
					again = true;
				}
				rows = expandRows(rows, key, value, depth);
			});
			//console.log({rows})
			return rows;
		});
		if (depth > 0)
			depth--;
	}

	return flatter;
}

function expandRows(rows, key, value, depth = -1) {
	//console.log({key, value})
	if (depth != 0 && _.endsWith(key, "*")) {
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
	return rows;
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

	if (q.orderBy) {
		table2 = _.orderBy(table2, q.orderBy);
	}

	if (q.distinctBy) {
		const groupKeys = (_.isArray(q.distinctBy)) ? q.distinctBy : [q.distinctBy];
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

// TODO: wells on dilution plates need to be unique
// figure out better way to chain operations.
// In this case, it might be best to 1) group, 2) shuffle, 3) assign to rows
// In other cases, we'll want to 1) shuffle, 2) group, 3) assign one value per each group as a whole
// An in yet other cases, 1) shuffle, 2) group, 3) assign to rows
//
// Every action handler is passed: row, rowIndex, group, groupIndex.
// The action handler returns an array of values to be expanded back into the row.
// The "row" may, in fact, be the first row of a group, if 'distinctBy' was used.
//
// So the options are:
// 1) group+shuffle+rows (re-shuffled for each group)
// 2) shuffle+group+rows (same shuffling is applied to each group)
// 3) shuffle+group+group (one value per group)
//
// Action properties:
// - groupBy -- creates groupings
// - distinctBy -- groups rows together that will get the same value
// - applyPerGroup: true -- call function to get values once per group, rather than applying same result to all groups
const actionHandlers = {
	"add": function(action, data) {
		return action.object;
	},
	"assign": function(action, data) {
		//console.log({name, x})
		if (_.isArray(action.values)) {
			return _.fromPairs([[action.name, action.values]]);
		}
		else if (_.isFunction(action.calculate)) {
			return _.fromPairs([[action.name, action.calculate(data)]]);
		}
		else {
			return _.fromPairs([[action.name, action.values]]);
		}
	},
	"range": function(action, data) {
		let from = _.get(action, "from", 1);
		let till = action.till;
		if (_.isUndefined(till)) {
			const rows = data.group || data.table;
			if (rows)
				till = rows.length;
		}
		if (!_.isUndefined(till)) {
			return _.fromPairs([[action.name, _.range(from, till+1)]]);
		}
	},
	/*case "assignPlates":
		table = assignPlates(table, action);
		break;
	case "replicate":
		table = replicate(table, action);
		break;
	*/
};

function flattenDesign(design) {
	//let table = flattenConditions(design.conditions);

	//console.log({assign: design.assign})

	function replicate(table, action) {
		table = _.flatMap(table, row => {
			const row2 = action.map(row);
			const rows
			  = (_.isArray(row2)) ? [row].concat(row2)
			  : (_.isPlainObject(row2)) ? [row, row2]
			  : [row];
			return rows;
		});
		return table;
	}

	function assignPlates(table, action) {
		const groups = query(table, {orderBy: action.orderBy, groupBy: action.groupBy});
		let plateIndex = 0;
		let i = 0;
		let n = 0;
		while (i < groups.length) {
			if (n == 0) {
				; // do nothing
			}
			else if (n + groups[i].length <= action.wellsPerPlate) {
				; // do nothing
			}
			else {
				plateIndex++;
				n = 0;
			}
			_.forEach(groups[i], row => { row[action.name] = action.plates[plateIndex]; });
			n += groups[i].length;
			assert(n <= action.wellsPerPlate);
			i++;
		}
		return table;
	}

	const conditionActions = _.map(design.conditions, (value, key) => {
		if (_.endsWith(key, "=")) {
			const handler2 = actionHandlers[value.action];
			assert(handler2, "unknown action: "+key+": "+JSON.stringify(value));
			if (handler2) {
				return _.merge({}, value, {name: key.substring(0, key.length - 1)});
			}
		}
		else {
			return {action: "assign", name: key, values: value};
		}
	});
	const actions = _.compact(conditionActions.concat(design.actions));
	console.log({actions})

	let table = [{}];
	_.forEach(actions, action => {
		const handler = actionHandlers[action.action];
		if (!handler) return;

		console.log("action: "+JSON.stringify(action))
		const groupsOfSames = groupSameIndexes(table, action);
		console.log("groupsOfSames: "+JSON.stringify(groupsOfSames))

		// Action properties:
		// - groupBy -- creates groupings
		// - sameBy -- groups rows together that will get the same value
		// - applyPerGroup: true -- call function to get values once per group, rather than applying same result to all groups

		function getValues(action, data) {
			let values = handler(action, data);
			if (!_.isUndefined(values)) {
				if (action.random) {
					if (_.isArray(values)) {
						values = _.shuffle(values);
					}
					else if (_.isPlainObject(values)) {
						values = _.mapValues(values, x => {
							return (_.isArray(x)) ? _.shuffle(x) : x;
						});
					}
				}
			}
			return values;
		}

		const valuesTable = (!action.applyPerGroup) ? getValues(action, {table}) : undefined;

		const replacements = [];

		_.forEach(groupsOfSames, (groupOfSames, groupIndex) => {
			// Create group using the first row in each set of "same" rows (ones which will be assigned the same value)
			const group = _.map(groupOfSames, sames => table[sames[0]]);
			const valuesGroup = (_.isUndefined(valuesTable)) ? getValues(action, {table, group, groupIndex}) : valuesTable;
			console.log({valuesGroup})
			_.forEach(groupOfSames, (sames, samesIndex) => {
				console.log({sames, samesIndex})
				let values;
				if (_.isUndefined(valuesGroup)) {
					const row = table[sames[0]]; // Arbitrarily pick first row of sames
					values = (_.isUndefined(valuesGroup)) ? getValues(action, {table, group, groupIndex, row, rowIndex: samesIndex}) : valuesGroup;
					//console.log("row: "+JSON.stringify(row));
					//console.log("value: "+JSON.stringify(value));
				}
				else {
					if (_.isArray(valuesGroup)) {
						values = valuesGroup[samesIndex];
					}
					else if (_.isPlainObject(valuesGroup)) {
						//assert(_.size(valuesGroup) === 1, "can only handle a single assignment: "+JSON.stringify(valuesGroup));
						values = _.mapValues(valuesGroup, (value, key) => {
							return (_.isArray(value) && !_.endsWith(key, "*")) ? value[samesIndex] : value
						});
					}
					else {
						assert(false, "expected an array or object: "+JSON.stringify(valuesGroup))
					}
				}
				mergeValues(table, sames, values, replacements);
			});
		});

		//console.log("replacements:\n"+replacements.map(JSON.stringify).join("\n"));
		//console.log({table});
		for (let i = replacements.length - 1; i >= 0; i--) {
			const rows = replacements[i];
			//console.log({i, rows})
			if (_.isArray(rows)) {
				table.splice(i, 1, ...rows);
				//console.log("table: "+JSON.stringify(table));
			}
		}
		printData(table);
		/*
		switch (action.action) {
			case "add":
				_.forEach(action.object, (value, name) => {
					table = expandRows(table, name, value);
				});
				break;
			case "assign":
				table = assign(table, action.name, action);
				break;
			case "assignPlates":
				table = assignPlates(table, action);
				break;
			case "replicate":
				table = replicate(table, action);
				break;
		}*/
	});

	return table;
}

function groupSameIndexes(table, action) {
	const indexes = _.range(table.length);

	// Group the row indexes
	let groups = [];
	if (action.groupBy) {
		const keys = (_.isArray(action.groupBy)) ? action.groupBy : [action.groupBy];
		groups = _(indexes).groupBy(rowIndex => _.map(keys, key => table[rowIndex][key])).values().value();
	}
	else {
		groups = [indexes];
	}

	// For each group, combine the row indexes that should be treated as being the same
	let groupsOfSames = [];
	if (action.sameBy) {
		const keys = (_.isArray(action.sameBy)) ? action.sameBy : [action.sameBy];
		groupsOfSames = _.map(groups, indexes => {
			return _(indexes).groupBy(rowIndex => _.map(keys, key => table[rowIndex][key])).values().value();
		});
	}
	else {
		// Every row should be treated as unique
		groupsOfSames = _.map(groups, indexes => _.map(indexes, i => [i]));
	}

	return groupsOfSames;
}

/**
 * [mergeValues description]
 * @param  {[type]} table     [description]
 * @param  {[type]} sames - indexes of rows to be assigned the same value
 * @param  {[type]} values    [description]
 * @param  {[type]} changeMap [description]
 * @return {[type]}           [description]
 */
function mergeValues(table, sames, values, replacements) {
	if (_.isArray(values)) {
		_.forEach(sames, (rowIndex, i) => {
			const values1 = values[i];
			expandRowByValues(table, rowIndex, values1, replacements)
		});
	}
	else if (_.isPlainObject(values)) {
		_.forEach(sames, (rowIndex, i) => {
			expandRowByValues(table, rowIndex, values, replacements)
		});
	}
	else {
		assert(false, "expected and array or object: "+JSON.stringify(values));
	}
}

/**
 * Add properties in 'values' to the given row in the table,
 * possibly expanding the number of rows in the table.
 * 'replacements' will be updated with the new array of rows.
 * @param  {[type]} table     [description]
 * @param  {[type]} rowIndex  [description]
 * @param  {[type]} values    [description]
 * @param  {[type]} replacements [description]
 * @return {[type]}           [description]
 */
function expandRowByValues(table, rowIndex, values, replacements) {
	let rows = [table[rowIndex]];
	_.forEach(values, (value, key) => {
		console.log({key, value})
		if (_.endsWith(key, "*")) {
			const starName = key.substring(0, key.length - 1);
			const starValues = value;
			if (_.isPlainObject(starValues)) {
				// For each entry in value, make a copy of every row in rows with the properties of the entry
				rows = _.flatMap(rows, row => {
					return _.map(starValues, (starValue, starKey) => {
						assert(_.isPlainObject(starValue));
						console.log({starName, starKey, starValue});
						return _.merge({}, row, _.fromPairs([[starName, starKey]]), starValue);
					});
				});
			}
			else if (_.isArray(starValues)) {
				// For each entry in value, make a copy of every row in rows with the properties of the entry
				rows = _.flatMap(rows, row => {
					return _.map(starValues, (starValue, starValueIndex) => {
						console.log({starName, starValueIndex, starValue})
						if (_.isPlainObject(starValue)) {
							const starKey = starValueIndex + 1;
							return _.merge({}, row, _.fromPairs([[starName, starKey]]), starValue);
						}
						else {
							const starKey = starValue;
							return _.merge({}, row, _.fromPairs([[starName, starKey]]));
						}
					});
				});
			}
			else {
				assert(false, "expected and array or object: "+JSON.stringify(starValues));
			}
		}
		else {
			_.forEach(rows, row => { row[key] = value; });
			/*if (key === "reseal") {
				console.log("reseal: "+value)
				console.log(rows)
			}*/
		}
	});
	replacements[rowIndex] = rows;
}

const table = flattenDesign(design2);
printConditions(design2.conditions);
printData(table);
//console.log(yaml.stringify(table, 4, 2))
