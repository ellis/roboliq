import _ from 'lodash';
import assert from 'assert';
import Immutable, {Map, fromJS} from 'immutable';
import math from 'mathjs';
import Random from 'random-js';
//import yaml from 'yamljs';

import {locationRowColToText} from './parsers/wellsParser.js';


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
 * - measure every 20 minutes (=> 16hr = 48 sample times)
 * - for the first 48 sample times, sample 1 well
 * - for the next 48 sample times, resample each well from before and 1 new well
 * - for the last 48 sample times, resample each well that's only been sampled once
 */

// TODO: add cultureOrder so that random groups of items with syringes 1-8 are assigned increasing order numbers
export const design2 = {
	conditions: {
		"strainSource": "strain1",
		"mediaSource": "media1",
		//"cultureNum*": _.range(1, 4+1),
		"cultureNum*=range": {
			till: 96
		},
		"cultureWell=range": {
			till: 96,
			shuffle: true
		},
		//"cultureCol=math": "floor((cultureWell - 1) / 8) + 1",
		//"cultureRow=math": "((cultureWell - 1) % 8) + 1",
		// TODO: syringe should be unique for each cultureWell, and ideally different for every sampleCycle
		"syringe=range": {
			till: 8,
			shuffle: true,
			rotateValues: true
		},
		/*"sampleNum": 1,
		"sampleCycle=range": {
			shuffle: true
		},
		"*": {
			conditions: {
				sampleNum: 2,
				"sampleCycle=math": "sampleCycle + 48"
			}
		},
		"dNum*=range": {till: 5},
		"dPlate=allocatePlates": {
			plates: ["dPlate1", "dPlate2", "dPlate3", "dPlate4", "dPlate5", "dPlate6", "dPlate7", "dPlate8", "dPlate9", "dPlate10", "dPlate11"],
			wellsPerPlate: 96,
			groupBy: "sampleCycle",
			orderBy: "sampleCycle",
			alternatePlatesByGroup: true
		},
		"dWell=range": {
			till: 96,
			groupBy: "dPlate",
			shuffle: true
		}*/
	},
	// TODO: add randomSeed
	actions: [
	]
};

// function printConditions(conditions) {
// 	console.log(yaml.stringify(conditions, 6, 2));
// }

export function printData(data, hideRedundancies = false) {
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

	// console.log(columns.map((s, i) => _.padEnd(s, widths[i])).join("  "));
	// console.log(columns.map((s, i) => _.repeat("=", widths[i])).join("  "));
	let linePrev;
	_.forEach(lines, line => {
		const s = line.map((s, i) => {
			const s2 = (s === "") ? "-"
				: (hideRedundancies && linePrev && s === linePrev[i]) ? ""
				: s;
			return _.padEnd(s2, widths[i]);
		}).join("  ");
		// console.log(s);
		linePrev = line;
	});
	// console.log(columns.map((s, i) => _.repeat("=", widths[i])).join("  "));
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

export function query(table, q) {
	let table2 = _.clone(table);
	if (q.select) {
		table2 = _.map(table2, x => _.pick(x, q.select));
	}

	if (q.where) {
		_.forEach(q.where, (value, key) => {
			table2 = _.filter(table, row => _.isEqual(row[key], value));
		});
	}

	if (q.shuffle) {
		table2 = _.shuffle(table);
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

	if (q.transpose) {
		table2 = _.zip.apply(_, table2);
	}

	return table2;
}

class ActionResult {
	constructor(functions) {
		//console.log({functions})
		this._functions = functions;
	}

	getGroupValues(groupIndex) {
		//console.log("this: "+JSON.stringify(this))
		if (this._functions.getGroupValues)
			return this._functions.getGroupValues(groupIndex);
	}

	getRowValues(rowIndex) {

	}
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
		// console.log("assign: "+JSON.stringify(action));
		if (_.isArray(action.values)) {
			return {[action.name]: action.values};
		}
		else if (_.isFunction(action.calculate)) {
			return {[action.name]: action.calculate(data)};
		}
		else {
			return {[action.name]: action.values};
		}
	},
	/*"assignRandomly": function(action, data) {
		const group = data.group;
		if (!group) return;
		const output = Array(group.length);
		let values = [];
		let j = 0;
		for (let i = 0; i < output.length; i++) {
			if (j >= values.length) {
				CONTINUE with shuffling values
				values = _.shuffle(action.values);
				j = 0;
			}
			output[i] = values[j++];
		}
	},*/
	"allocatePlates": function(action, data) {
		const groups = data.groupsOfSames;
		if (!groups) return;

		//printData(groups);

		// Find number of plates required
		let plateCount = -1;
		let n = 0;
		const sequence = [];
		for (let i = 0; i < groups.length; i++) {
			const group = groups[i];
			const n2 = n + group.length;
			if (plateCount < 0 && n2 > 0) {
				plateCount = 1;
			}

			if (n2 <= action.wellsPerPlate) {
				n = n2;
			}
			else {
				assert(n > 0, "too many positions in group for plate to accomodate");
				plateCount++;
				n = groups[i].length;
			}
			sequence.push(plateCount - 1);
			// console.log({i, group, n2, n, plateCount})
		}

		assert(plateCount <= action.plates.length, `required ${plateCount} plates, but only ${action.plates.length} supplied: ${action.plates.join(",")}`);

		const groupValues = (action.alternatePlatesByGroup)
			? _.map(groups, (group, i) => action.plates[i % plateCount])
			: _.map(groups, (group, i) => action.plates[sequence[i]]);
		//console.log({plateCount, groupLength: groups.length, sequence, plateValues: values})

		return new ActionResult({
			getGroupValues: (groupIndex) => _.fromPairs([[action.name, groupValues[groupIndex]]])
		});
	},
	"allocateWells": function(action, data) {
		const rows = action.rows;
		const cols = action.columns;
		assert(_.isNumber(rows) && rows > 0, "missing required positive number `rows`");
		assert(_.isNumber(cols) && cols > 0, "missing required positive number `columns`");
		const byColumns = _.get(action, "byColumns", true);
		const values = _.range(rows * cols).map(i => {
			const [row, col] = (byColumns) ? [i % rows, Math.floor(i / rows)] : [Math.floor(i / cols), i % cols];
			const s = locationRowColToText(row + 1, col + 1);
			// console.log({row, col, s});
			return s;
		});
		// console.log({values})
		return { [action.name]: values };
	},
	"math": function(action, data) {
		// TODO: adapt so that it can work on groups?
		if (data.row) {
			// Build the scope for evaluating the math expression from the current data row
			const scope = _.mapValues(data.row, x => {
				// console.log({x})
				try {
					const result = math.eval(x);
					// If evaluation succeeds, but it was just a unit name, then set value as string instead
 					if (result.type === "Unit" && result.value === null)
						return x;
					else {
						return result;
					}
				}
				catch (e) {}
				return x;
			});
			const expr = _.get(action, "expression", action.values);
			assert(!_.isUndefined(expr), "`expression` property must be specified");
			// console.log("scope:"+JSON.stringify(scope, null, '\t'))
			let value = math.eval(expr, scope);
			// console.log({type: value.type, value})
			if (_.isString(value)) {
				return { [action.name]: value };
			}

			// Get units to use in the end, and the unitless value
			const {units0, units, unitless} = (() => {
				const result = {
					units0: undefined,
					units: action.units,
					unitless: value
				};
				// If the result has units:
				if (value.type === "Unit") {
					result.units0 = value.formatUnits();
					if (_.isUndefined(result.units))
						result.units = result.units0;
					const conversionUnits = (_.isEmpty(result.units)) ? result.units0 : result.units;
					// If the units dissappeared, e.g. when dividing 30ul/1ul = 30:
					if (_.isEmpty(conversionUnits)) {
						// TODO: find a better way to get the unit-less quantity from `value`
						// console.log({action})
						// console.log({result, conversionUnits});
						result.unitless = math.eval(value.format());
					}
					else {
						result.unitless = value.toNumeric(conversionUnits);
					}
				}
				return result;
			})();
			// console.log({unitless})

			// Restrict decimal places
			const unitlessText = (_.isNumber(action.decimals))
				? unitless.toFixed(action.decimals)
				: unitless.toNumber().toString();

			// Set units
			const valueText = (!_.isEmpty(units))
				? unitlessText + " " + units
				: unitlessText;

			return { [action.name]: valueText };
		}
	},
	"range": function(action, data) {
		let till = action.till;
		if (_.isUndefined(till)) {
			const rows = data.group || data.table;
			if (rows)
				till = rows.length;
		}
		let range;
		if (!_.isUndefined(till)) {
			const from = _.get(action, "from", 1);
			assert(_.isNumber(from), "`from` must be a number");
			if (_.isNumber(action.count)) {
				const diff = till - from;
				range = _.range(action.count).map(i => {
					const d = diff * i / (action.count - 1);
					return from + d;
				});
			}
			else {
				const step = _.get(action, "step", 1);
				range = _.range(from, till+1, step);
			}
		}
		if (range) {
			if (_.isNumber(action.decimals)) {
				range = range.map(n => Number(n.toFixed(action.decimals)));
			}
			if (_.isString(action.units)) {
				range = range.map(n => `${n} ${action.units}`);
			}
			return { [action.name]: range };
		}
	},
	"sample": function(action, data) {
		action.sample = true;
		console.log('sample')
		console.log({action})
		return actionHandlers.assign(_.merge({}, action, {sample: true}), data);
	}
	/*case "assignPlates":
		table = assignPlates(table, action);
		break;
	case "replicate":
		table = replicate(table, action);
		break;
	*/
};

export function flattenDesign(design) {
	//let table = flattenConditions(design.conditions);

	//console.log({assign: design.assign})

	const conditionActions = convertConditionsToActions(design.conditions);
	const actions = _.compact(conditionActions.concat(design.actions));
	// console.log({actions})

	var randomEngine = Random.engines.mt19937();
	if (_.isNumber(design.randomSeed)) {
		randomEngine.seed(design.randomSeed);
	}
	else {
		randomEngine.autoSeed();
	}
	//random.shuffle(randomEngine, combined);

	let table = [{}];
	_.forEach(actions, action => {
		applyActionToTable(table, action, randomEngine);
	});

	return table;
}

function convertConditionsToActions(conditions) {
	return _.map(conditions, (value, key) => {
		// Create replicate rows
		if (_.startsWith(key, "*")) {
			return _.merge({}, {action: "replicate"}, value);
		}
		else if (_.includes(key, "=")) {
			const [name, action] = key.split("=");
			if (!_.isEmpty(action)) {
				const handler2 = actionHandlers[action];
				assert(handler2, "unknown action: "+key+": "+JSON.stringify(value));
				const value2 = _.isString(value) || _.isArray(value) ? {values: value} : value;
				return _.merge({}, {action, name}, value2);
			}
			else {
				return (_.isPlainObject(value))
					? _.merge({action: "assign", name}, value)
					: {action: "assign", name, values: value};
			}
		}
		else {
			return {action: "assign", name: key, values: value};
		}
	});
}

function applyActionToTable(table, action, randomEngine) {
	console.log("action: "+JSON.stringify(action))
	const groupsOfSames = groupSameIndexes(table, action);
	// console.log("groupsOfSames: "+JSON.stringify(groupsOfSames))

	// Action properties:
	// - groupBy -- creates groupings
	// - sameBy -- groups rows together that will get the same value
	// - applyPerGroup: true -- call function to get values once per group, rather than applying same result to all groups

	const replacements = [];
	if (action.action === "replicate") {
		replicate(action, table, _.flattenDeep(groupsOfSames), replacements);
	}
	else {
		const handler = actionHandlers[action.action];
		if (!handler) return;

		let valueOffset = 0;
		function getValues(action, data) {
			// console.log(`getValues, dataKeys=${_.keys(data).join(",")}`)
			// console.log(data)
			valueOffset = 0;
			let values = handler(action, data);
			// console.log({values})
			if (!_.isUndefined(values)) {
				//console.log("sample? "+(_.isNumber(action.sample) || action.sample === true))
				// TEMPORARY: For legacy reasons, allow for use of deprecated 'random' alias
				const shuffle = _.get(action, "shuffle", action.random);
				if (_.isNumber(shuffle) || shuffle === true) {
					const randomEngine2 = (_.isNumber(shuffle))
						? Random.engines.mt19937().seed(shuffle)
						: randomEngine;
					if (_.isArray(values)) {
						values = Random.sample(randomEngine2, values, values.length);
					}
					else if (_.isPlainObject(values)) {
						values = _.mapValues(values, x => {
							return (_.isArray(x)) ? Random.sample(randomEngine2, x, x.length) : x;
						});
					}
				}
				/*else if (_.isNumber(action.sample) || action.sample === true) {
					const randomEngine2 = (_.isNumber(action.sample))
						? Random.engines.mt19937().seed(action.sample)
						: randomEngine;
					const count
						= (data.row) ? 1
						: (data.groups) ? data.groups.length
						: (data.groupsOfSames) ? _.sum(data.groupsOfSames.map(groupOfSames => groupOfSames.length))
						: values.length;
					console.log({action, count, dataKeys: _.keys(data), values})
					if (_.isArray(values)) {
						values = sample(values, count, randomEngine2);
					}
					else if (_.isPlainObject(values)) {
						values = _.mapValues(values, x => {
							return (_.isArray(x)) ? sample(x, count, randomEngine2) : x;
						});
					}
				}*/
			}
			return values;
		}

		// Call getValues for the entire table if:
		const doGetValuesTable = (
			// Assume we can,
			true &&
			// But don't if 'applyPerGroup' is set
			!action.applyPerGroup &&
			// Nor if the value is randomized (and 'shuffleOnce' isn't set)
			!(action.shuffle && !action.shuffleOnce)
		)
		const valuesTable = (doGetValuesTable) ? getValues(action, {table, groupsOfSames}) : undefined;
		// console.log({valuesTable})

		let shuffledIndexes = [];
		_.forEach(groupsOfSames, (groupOfSames, groupIndex) => {
			// Create group using the first row in each set of "same" rows (ones which will be assigned the same value)
			const group = _.map(groupOfSames, sames => table[sames[0]]);
			const valuesGroup
				= (valuesTable instanceof ActionResult) ? valuesTable.getGroupValues(groupIndex)
				: (_.isUndefined(valuesTable)) ? getValues(action, {table, group, groupIndex})
				: valuesTable;
			// console.log({valuesGroup})
			_.forEach(groupOfSames, (sames, samesIndex) => {
				// console.log({sames, samesIndex})
				let values;
				if (_.isUndefined(valuesGroup)) {
					const row = table[sames[0]]; // Arbitrarily pick first row of sames
					values = (_.isUndefined(valuesGroup)) ? getValues(action, {table, group, groupIndex, row, rowIndex: samesIndex}) : valuesGroup;
					//console.log("row: "+JSON.stringify(row));
					//console.log("value: "+JSON.stringify(value));
				}
				else {
					if (_.isArray(valuesGroup)) {
						const j = (action.rotateValues || action.shuffle) ? (samesIndex + valueOffset) % valuesGroup.length : samesIndex;
						if (j === 0 && action.shuffle) {
							shuffledIndexes = Random.shuffle(randomEngine, _.range(valuesGroup.length));
						}
						const k = (action.shuffle) ? shuffledIndexes[j] : j;
						values = valuesGroup[j];
					}
					else if (_.isPlainObject(valuesGroup)) {
						//assert(_.size(valuesGroup) === 1, "can only handle a single assignment: "+JSON.stringify(valuesGroup));
						const values2 = _(valuesGroup).toPairs().flatMap(([key, value]) => {
							if (!_.isArray(value) || _.endsWith(key, "*")) {
								return [[key, value]];
							}
							else {
								const j = (action.rotateValues) ? valueOffset % value.length : samesIndex;
								return (
									(_.isPlainObject(value[j])) ? [[key, j + 1]].concat(_.toPairs(value[j])) :
									(_.isArray(value[j])) ? [[key, j + 1], [".IGNORE*", value[j]]] :
									[[key, value[j]]]
								);
							}
						}).value();
						// console.log("values2: "+JSON.stringify(values2));
						values = _.fromPairs(values2);
					}
					else {
						assert(false, "expected an array or object: "+JSON.stringify(valuesGroup))
					}
				}
				// console.log({values, valueOffset})
				mergeValues(table, sames, values, valueOffset, action, replacements);
				valueOffset++;
			});
		});
	}

	// Apply row replacements to table
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
	// printData(table);
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
 * @param  {array} table - source table
 * @param  {array} sames - indexes of rows to be assigned the same value
 * @param  {object} values - map from column keys to new values
 * @param  {integer} valueOffset CONTINUE
 * @param  {[type]} changeMap [description]
 * @return {[type]}           [description]
 */
function mergeValues(table, sames, values, valueOffset, action, replacements) {
	// console.log("mergeValues:")
	// console.log({valueOffset, values, sames})
	if (_.isArray(values)) {
		_.forEach(sames, (rowIndex, i) => {
			const j = (action.rotateValues) ? (valueOffset + i) % values.length : i;
			const values1 = values[j];
			// console.log({i, j, value1})
			expandRowByValues(table, rowIndex, values1, replacements)
		});
	}
	else if (_.isPlainObject(values)) {
		_.forEach(sames, rowIndex => {
			expandRowByValues(table, rowIndex, values, replacements)
		});
	}
	else {
		assert(false, "expected and array or object: "+JSON.stringify(values));
	}
}

/**
 * Add the properties in 'values' to the given row in the table,
 * possibly expanding the number of rows in the table.
 * 'replacements' will be updated with the new array of rows to replace the
 * original row.
 * @param  {array} table - original table of rows
 * @param  {integer} rowIndex - index of row to modify
 * @param  {object} values - map from either colum name to column value, or starred column name to a map of more column names.
 * @param  {object} replacements - map from rowIndex to array of replacement rows
 */
function expandRowByValues(table, rowIndex, values, replacements) {
	// console.log("expandRowByValues:")
	// console.log({table, rowIndex, values, replacements})

	function expandRowByObject(row, starName, starKey, starValue) {
		const conditionActions = convertConditionsToActions(starValue);
		// Add starName/Key to row (but ignore names that start with a '.')
		const row1 = (_.startsWith(starName, ".")) ? row : _.merge({}, row, {[starName]: starKey});
		// Create a table from the row
		const table2 = [_.cloneDeep(row1)];
		// Expand the table
		_.forEach(conditionActions, action => {
			applyActionToTable(table2, action);
		});
		// Return the expanded table
		return table2;
	}

	let rows = [table[rowIndex]];
	_.forEach(values, (value, key) => {
		// console.log({key, value})
		if (_.endsWith(key, "*")) {
			const starName = key.substring(0, key.length - 1);
			const starValues = value;
			// console.log({starName, starValues})
			if (_.isPlainObject(starValues)) {
				// For each entry in value, make a copy of every row in rows with the properties of the entry
				rows = _.flatMap(rows, row => {
					return _.flatMap(starValues, (starValue, starKey) => {
						assert(_.isPlainObject(starValue));
						return expandRowByObject(row, starName, starKey, starValue);
					});
				});
			}
			else if (_.isArray(starValues)) {
				// For each entry in value, make a copy of every row in rows with the properties of the entry
				rows = _.flatMap(rows, row => {
					return _.flatMap(starValues, (starValue, starValueIndex) => {
						// console.log({starName, starValueIndex, starValue})
						if (_.isPlainObject(starValue)) {
							const starKey = starValueIndex + 1;
							return expandRowByObject(row, starName, starKey, starValue);
						}
						else {
							const starKey = starValue;
							return _.merge({}, row, {[starName]: starKey});
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

/**
 * - extendRowsByValue:
 *     - for each selected row, call extendRowByValue
 * - extendRowsByObject:
 *     - for each key/value pair, call extendRowsByNamedValue
 * - extendRowsByNamedValue:
 *     - TODO: turn the name/value into an action in order to allow for more sophisticated expansion
 *     - if the value is an array or object, for each row and subvalue, call extendRowByNamedValue with subvalue
 *     - otherwise, for each row, call extendRowByNamedValue
 * - extendRowByNamedValue:
 *     - if the value is an array, replicate the row N times, set `$name=i+1`, and then call extendRowByValue for each
 *     - if the value is an object, replicate the row N times, set `$name=key`, then call extendRowByValue for each
 *     - otherwise, call setColumnValue
 * - extendRowByValue:
 *     - if the value is an array, replicate the row N times, and call extendRowsByValue for each
 *     - if the value is an object, for each key/value pair, call extendRowsByValue
 *     - otherwise error
 * - setColumnValue: sets a basic named value on a row
 *
 * PROBLEM: when should an array be assigned to rows, and when should it replicate the rows?
 */
/*
a*: [a, b] # branching
b: [a, b] # assignment
c: hello # assignment
d: [[{e: A, f: L}, {e: B, f: R}], [{e: A, f: R}, {e: B, f: L}]] # Assignment then branching
e: [[1, 2], [3, 4]] # ERROR
 */

// TODO: rename extendRow* to expandRow*

export function extendRowsByValue(nestedRows, rowIndexes, value) {
	assert(_.isArray(nestedRows));
	assert(_.isArray(rowIndexes));

	for (let i = 0; i < rowIndexes.length; i++) {
		const rowIndex = rowIndexes[i];
		extendRowByValue(nestedRows, rowIndex, value);
	}
}

export function extendRowsByObject(nestedRows, rowIndexes, o) {
	assert(_.isArray(nestedRows));
	assert(_.isArray(rowIndexes));

	_.forEach(o, (name, value) => {
		extendRowsByNamedValue(nestedRows, rowIndexes, name, value);
	}
}

export function extendRowsByNamedValue(nestedRows, rowIndexes, name, value) {
	assert(_.isArray(nestedRows));
	assert(_.isArray(rowIndexes));
	assert(!_.endsWith(name, "*"), `extendRowsByNamedValue() cannot handle branch factor names: ${name}`);

	if (_.isArray(value) || _.isPlainObject(value)) {
		const valueKeys = (_.isArray(value)) ? _.range(value.length) : _.keys(value);
		assert(rowIndexes.length <= valueKeys.length, "fewer values than rows: "+JSON.stringify({name, values, rowIndexes}));
		for (let i = 0; i < rowIndexes.length; i++) {
			const valueKey = valueKeys[i];
			const value = values[valueKey];
			const rowIndex = rowIndexes[i];
			const row = nestedRows[rowIndex];
			// If this "row" is has nested rows:
			if (_.isArray(row)) {
				extendRowsByNamedValue(row, _.range(row.length), name, value);
			}
			// If this is an actual row:
			else if (_.isPlainObject(row)) {
				extendRowByNamedValue(nestedRows, rowIndex, name, value);
				if (_.isArray(value)) {
					setColumnValue(row, name, valueKey);
					multiplyRowByArray(nestedRows, rowIndex, value);
				}
				else if (_.isPlainObject(value)) {
					setColumnValue(row, name, valueKey);
					extendRowByNamedObject(nestedRows, rowIndex, name, value);
				}
				else {
					setColumnValue(row, name, value)
				}
			}
			else {
				assert(false, "row must be a plain object or an array: "+JSON.stringify(row));
			}
		}
	}
	else {
		for (let i = 0; i < rowIndexes.length; i++) {
			const rowIndex = rowIndexes[i];
			const row = nestedRows[rowIndex];
			if (_.isArray(row)) {
				extendRowsByNamedValue(row, _.range(row.length), name, value);
			}
			else {
				assert(_.isPlainObject(row), "row must be a plain object or an array: "+JSON.stringify(row));
				setColumnValue(row, name, value);
			}
		}
	}
}

CONTINUE
export function extendRowByNamedValue(nestedRows, rowIndex, name, value) {
	assert(_.isArray(nestedRows));
	assert(!_.endsWith(name, "*"), `extendRowsByNamedValue() cannot handle branch factor names: ${name}`);

	const row = nestedRows[rowIndex];

	if (_.isArray(value) || _.isPlainObject(value)) {
		const valueKeys = (_.isArray(value)) ? _.range(value.length) : _.keys(value);
		assert(rowIndexes.length <= valueKeys.length, "fewer values than rows: "+JSON.stringify({name, values, rowIndexes}));
			const valueKey = valueKeys[i];
			const value = values[valueKey];
			// If this "row" is has nested rows:
			if (_.isArray(row)) {
				extendRowsByNamedValue(row, _.range(row.length), name, value);
			}
			// If this is an actual row:
			else if (_.isPlainObject(row)) {
				if (_.isArray(value)) {
					setColumnValue(row, name, valueKey);
					multiplyRowByArray(nestedRows, rowIndex, value);
				}
				else if (_.isPlainObject(value)) {
					setColumnValue(row, name, valueKey);
					extendRowByNamedObject(nestedRows, rowIndex, name, value);
				}
				else {
					setColumnValue(row, name, value)
				}
			}
			else {
				assert(false, "row must be a plain object or an array: "+JSON.stringify(row));
			}
		}
	}
	else {
		if (_.isArray(row)) {
			extendRowsByNamedValue(row, _.range(row.length), name, value);
		}
		else {
			assert(_.isPlainObject(row), "row must be a plain object or an array: "+JSON.stringify(row));
			setColumnValue(row, name, value);
		}
	}
}

// Set the given value, but only if the name doesn't start with a period
function setColumnValue(row, name, value) {
	if (name.length > 1 && name[0] != ".") {
		row[name] = value;
	}
}

function multiplyRowByArray(nestedRows, rowIndex, values) {
	assert(_.isArray(nestedRows));
	assert(_.isArray(values));
	const row0 = nestedRows[rowIndex];
	assert(_.isPlainObject(row));
	const rows = Array(values.length);
	nestedRows[rowIndex] = rows;

	for (let i = 0; i < rows.length; i++) {
		const row = _.clone(row0);
		rows[i] = row;
		const value = values[i];
		// If the value is an array, we set name=index, create another nesting
		// of rows, and multiply them.
		if (_.isArray(value)) {
			rows[i] = [rows[i]];
			multiplyRowByArray(rows, i, value);
		}
		// If the value is an object, we set name=index, create another nesting
		// of rows, and extend them.
		else if (_.isPlainObject(value)) {
			extendRowByObject(rows, i, value);
		}
		else {
			assert(false, "all array items must be objects or arrays: "+JSON.stringify(values[i]));
		}
	}
}

function extendRowByObject(nestedRows, rowIndex, o) {
	_.forEach(o, (value, name) => {
		extendRowByNamedValue(nestedRows, rowIndex, name, value);
	});
}

function expandRowByNamedValue(nestedRows, rowIndex, name, value) {
	CONTINUE
	const conditionActions = convertConditionsToActions(starValue);
	// Add starName/Key to row (but ignore names that start with a '.')
	const row1 = (_.startsWith(starName, ".")) ? row : _.merge({}, row, {[starName]: starKey});
	// Create a table from the row
	const table2 = [_.cloneDeep(row1)];
	// Expand the table
	_.forEach(conditionActions, action => {
		applyActionToTable(table2, action);
	});
	// Return the expanded table
	return table2;
}

function replicate(action, table, rowIndexes, replacements) {
	// console.log("replicate:")
	// console.log({action, table, rowIndexes, replacements})
	_.forEach(rowIndexes, rowIndex => {
		const row0 = table[rowIndex];
		const count = _.get(action, "count", 1);
		let rows1 = (count === 1) ? [row0] : _.map(_.range(count), i => _.clone(row0));

		if (action.map) {
			const rows2 = _.flatMap(rows1, row1 => {
				const row2 = action.map(row1);
				return (_.isArray(row2)) ? [row1].concat(row2)
					: (_.isPlainObject(row2)) ? [row1, row2]
					: [row1];
			});
			replacements[rowIndex] = rows2;
		}
		else if (action.conditions) {
			const rows2 = _.flatMap(rows1, row1 => {
				const conditionActions = convertConditionsToActions(action.conditions);
				const table2 = [_.cloneDeep(row1)];
				_.forEach(conditionActions, action => {
					applyActionToTable(table2, action);
				});
				return [row1].concat(table2);
			});
			replacements[rowIndex] = rows2;
		}
		else {
			replacements[rowIndex] = rows1;
		}
	});
	// console.log({replacements})
}

export function getCommonValues(table) {
	if (_.isEmpty(table)) return {};

	let common = _.clone(table[0]);
	for (let i = 1; i < table.length; i++) {
		// Remove any value from common which aren't shared with this row.
		_.forEach(table[i], (value, name) => {
			if (common.hasOwnProperty(name) && !_.isEqual(common[name], value)) {
				delete common[name];
			}
		})
	}

	return common;
}

/*
function sample(source, count, randomEngine) {
	// console.log("sample", source, count)
	const output = Array(count);
	let values = _.clone(source);
	let j = values.length;
	for (let i = 0; i < output.length; i++) {
		if (j >= values.length) {
			Random.shuffle(randomEngine, values);
			j = 0;
		}
		output[i] = values[j++];
	}
	return output;
}*/

// const table = flattenDesign(design2);
// printConditions(design2.conditions);
// printData(table);
//console.log(yaml.stringify(table, 4, 2))
