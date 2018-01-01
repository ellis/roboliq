const _ = require('lodash');
const assert = require('assert');
// import Immutable, {Map, fromJS} from 'immutable';
const math = require('mathjs');
const Random = require('random-js');
//const yaml = require('yamljs');

//const {locationRowColToText} = require('./parsers/wellsParser.js');
// FIXME: HACK: this function is included here temporarily, to make usage in react component easier for the moment
function locationRowColToText(row, col) {
	var colText = col.toString();
	if (colText.length == 1) colText = "0"+colText;
	return String.fromCharCode("A".charCodeAt(0) + row - 1) + colText;
}

export function printRows(rows, hideRedundancies = false) {
	const data = _.flattenDeep(rows);

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

export function flattenDesign(design) {
	const randomEngine = Random.engines.mt19937();
	if (_.isNumber(design.randomSeed)) {
		randomEngine.seed(design.randomSeed);
	}
	else {
		randomEngine.autoSeed();
	}

	let rows = expandConditions(design.conditions, randomEngine);
	if (design.orderBy) {
		rows = _.orderBy(rows, design.orderBy);
	}
	return rows;
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
		});
	}

	return common;
}

export function getCommonValuesNested(nestedRows, rowIndexes, common) {
	if (_.isEmpty(rowIndexes)) return {};

	for (let i = 0; i < rowIndexes.length; i++) {
		const rowIndex = rowIndexes[i];
		const row = nestedRows[rowIndex];
		if (_.isArray(row)) {
			getCommonValuesNested(row, _.range(row.length), common);
		}
		else if (_.isUndefined(common)) {
			common = _.clone(row);
		}
		else {
			// Remove any value from common which aren't shared with this row.
			_.forEach(row, (value, name) => {
				if (common.hasOwnProperty(name) && !_.isEqual(common[name], value)) {
					delete common[name];
				}
			});
		}
	}

	return common;
}

/**
 * Is like _.flattenDeep, but it mutates the array in-place.
 *
 * @param  {array} rows - array to flatten
 */
export function flattenArrayM(rows) {
	let i = rows.length;
	while (i > 0) {
		i--;
		const item = rows[i];
		if (_.isArray(item)) {
			// Flatten the sub-array
			flattenArrayM(item);
			// Splice the original sub-array back into the parent array
			rows.splice(i, 1, ...item);
		}
	}
	return rows;
}

/**
 * Is like _.flattenDeep, but only for the given rows, and it mutates both the rows array and rowIndexes array in-place.
 *
 * @param {array} rows - array to flatten
 * @param {array} rowIndexes - array of row indexes to flatten
 * @param {integer} rowIndexesOffset - index in rowIndexes to start at
 */
export function flattenArrayAndIndexes(rows, rowIndexes) {
	let i = 0;
	while (i < rowIndexes.length) {
		const rowIndex = rowIndexes[i];
		const item = rows[rowIndex];
		if (_.isArray(item)) {
			// Flatten the sub-array
			flattenArrayM(item);
			// Splice the original sub-array back into the parent array
			rows.splice(rowIndex, 1, ...item);

			// Update rowIndexes
			for (let j = i + 1; j < rowIndexes.length; j++) {
				rowIndexes[j] += item.length - 1;
			}
			const x = _.range(rowIndex, rowIndex + item.length);
			// console.log({x})
			rowIndexes.splice(i, 1, ...x);

			i += item.length;
		}
		else {
			i++;
		}
	}
}

/**
 */
export function expandConditions(conditions, randomEngine) {
	// console.log("expandConditions: "+JSON.stringify(conditions))
	const table = [{}];
	expandRowsByObject(table, [0], conditions, randomEngine);
	flattenArrayM(table);
	return table;
}

/**
 * expandRowsByObject:
 *   for each key/value pair, call expandRowsByNamedValue
 */
function expandRowsByObject(nestedRows, rowIndexes, conditions, randomEngine) {
	// console.log("expandRowsByObject: "+JSON.stringify(conditions));
	for (let name in conditions) {
		expandRowsByNamedValue(nestedRows, rowIndexes, name, conditions[name], randomEngine);
	}
}

/**
 * // REQUIRED by: expandRowsByObject
 * expandRowsByNamedValue:
 *   TODO: turn the name/value into an action in order to allow for more sophisticated expansion
 *   if has star-suffix, call branchRowsByNamedValue
 *   else call assignRowsByNamedValue
 */
function expandRowsByNamedValue(nestedRows, rowIndexes, name, value, randomEngine) {
	// console.log(`expandRowsByNamedValue: ${name}, ${JSON.stringify(value)}`); console.log({rowIndexes}); console.log(nestedRows)
	// If an action is specified using the "=" symbol:
	const iEquals = name.indexOf("=");
	if (iEquals >= 0) {
		// Need to flatten the rows in case the action uses groupBy or sameBy
		flattenArrayAndIndexes(nestedRows, rowIndexes);
		const actionType = name.substr(iEquals + 1) || "assign";
		const actionHandler = actionHandlers[actionType];
		assert(actionHandler, `unknown action type: ${actionType} in ${name}`)
		name = name.substr(0, iEquals);

		const result = actionHandler(nestedRows, rowIndexes, name, value, randomEngine);
		// If no result was returned, the action handled modified the rows directly:
		if (_.isUndefined(result)) {
			return;
		}
		// Otherwise, continue processing using the action's results
		else {
			value = result;
		}
	}

	const starIndex = name.indexOf("*");
	if (starIndex >= 0) {
		// Remove the branching suffix from the name
		name = name.substr(0, starIndex);
		// If the name is empty, automatically pick a dummy name that will be omitted
		if (_.isEmpty(name)) {
			name = ".HIDDEN";
		}
		// If the branching value is just a number, then assume it means the number of replicates
		if (_.isNumber(value)) {
			value = _.range(1, value + 1);
		}

		branchRowsByNamedValue(nestedRows, rowIndexes, name, value, randomEngine);
	}
	else {
		assignRowsByNamedValue(nestedRows, rowIndexes, name, value, randomEngine);
	}
}

/*
 * // REQUIRED by: expandRowsByNamedValue, branchRowsByNamedValue
 * assignRowsByNamedValue: (REQUIRED FOR ASSIGNING ARRAY TO ROWS)
 *   if value is array:
 *     for i in count:
 *       rowIndex = rowIndexes[i]
 *       assignRowByNamedKeyItem(nestedRows, rowIndex, name, i+1, value[i])
 *   else if value is object:
 *     keys = _.keys(value)
 *     for each i in keys.length:
 *       key = keys[i]
 *       item = value[key]
 *       assignRowByNamedKeyItem(nestedRows, rowIndex, name, key, item)
 *   else:
 *     for each row:
 *       setColumnValue(row, name, value)
 */
function assignRowsByNamedValue(nestedRows, rowIndexes, name, value, randomEngine) {
	// console.log(`assignRowsByNamedValue: ${name}, ${JSON.stringify(value)}`);
	// printRows(nestedRows)
	// console.log({rowIndexes})
	if (value instanceof Special || _.isArray(value)) {
		let valueIndex = 0;
		for (let i = 0; i < rowIndexes.length; i++) {
			const rowIndex = rowIndexes[i];
			valueIndex += assignRowByNamedValuesKey(nestedRows, rowIndex, name, value, valueIndex, undefined, randomEngine);
		}
	}
	else if (_.isPlainObject(value)) {
		let valueIndex = 0;
		const keys = _.keys(value);
		for (let i = 0; i < rowIndexes.length; i++) {
			const rowIndex = rowIndexes[i];
			valueIndex += assignRowByNamedValuesKey(nestedRows, rowIndex, name, value, valueIndex, keys, randomEngine);
		}
	}
	else {
		for (let i = 0; i < rowIndexes.length; i++) {
			const rowIndex = rowIndexes[i];
			setColumnValue(nestedRows[rowIndex], name, value);
			// console.log(JSON.stringify(nestedRows))
		}
	}
}

/*
 * // REQUIRED by: assignRowsByNamedValue
 * assignRowByNamedValuesKey:
 *   if item is array:
 *     setColumnValue(row, name, key)
 *     branchRowByArray(nestdRows, rowIndex, item)
 *   else if item is object:
 *     setColumnValue(row, name, key)
 *     expandRowsByObject(nestedRows, [rowIndex], item)
 *   else:
 *     setColumnValue(row, name, item)
 */
function assignRowByNamedValuesKey(nestedRows, rowIndex, name, values, valueKeyIndex, valueKeys, randomEngine) {
	// console.log(`assignRowByNamedValuesKey: ${name}, ${JSON.stringify(values)}, ${valueKeyIndex}`);
	// console.log(JSON.stringify({rowIndex, name, valueKeyIndex, valueKeys}))
	const row = nestedRows[rowIndex];
	let n = 0;
	if (_.isArray(row)) {
		// console.log("0")
		for (let i = 0; i < row.length; i++) {
			const n2 = assignRowByNamedValuesKey(row, i, name, values, valueKeyIndex, valueKeys, randomEngine);
			n += n2;
			valueKeyIndex += n2;
		}
	}
	else {
		// console.log("A")
		let item, key;
		if (values instanceof Special) {
			// console.log("B: "+rowIndex)
			[key, item] = values.next(nestedRows, rowIndex);
		}
		else {
			// console.log("C")
			assert(valueKeyIndex < _.size(values), "fewer values than rows: "+JSON.stringify({name, values}));
			const valueKey = (valueKeys) ? valueKeys[valueKeyIndex] : valueKeyIndex;
			key = (valueKeys) ? valueKey : valueKey + 1;
			item = values[valueKey];
		}

		// console.log("D")
		// console.log({item})
		if (_.isArray(item)) {
			setColumnValue(row, name, key);
			branchRowByArray(nestedRows, rowIndex, item, randomEngine);
		}
		else if (_.isPlainObject(item)) {
			setColumnValue(row, name, key);
			expandRowsByObject(nestedRows, [rowIndex], item, randomEngine);
		}
		else {
			setColumnValue(row, name, item);
		}
		n = 1;
	}
	return n;
}

/*
 * // REQUIRED by: expandRowsByNamedValue
 * branchRowsByNamedValue:
 *   size
 *     = (value is array) ? value.length
 *     : (value is object) ? _.size(value)
 *     : 1
 *   row0 = nestedRows[rowIndex];
 *   rows2 = Array(size)
 *   for each rowIndex2 in _.range(size):
 *     rows2[rowIndex] = _.cloneDeep(row0)
 *
 *   expandRowsByNamedValue(rows2, _.range(size), name, value);
 *   nestedRows[rowIndex] = _.flattenDeep(rows2);
 */
function branchRowsByNamedValue(nestedRows, rowIndexes, name, value, randomEngine) {
	// console.log(`branchRowsByNamedValue: ${name}, ${JSON.stringify(value)}, ${rowIndexes}`); console.log(JSON.stringify(nestedRows))
	const isSpecial = (value instanceof Special);
	const size
		= (_.isArray(value)) ? value.length
		: (_.isPlainObject(value)) ? _.size(value)
		: (isSpecial) ? value.valueCount
		: 1;
	for (let i = 0; i < rowIndexes.length; i++) {
		const rowIndex = rowIndexes[i];
		// Make replicates of row
		const row0 = nestedRows[rowIndex];

		if (isSpecial) {
			value.initGroup(nestedRows, [rowIndex]);
		}

		if (_.isArray(row0)) {
			branchRowsByNamedValue(row0, _.range(row0.length), name, value, randomEngine);
		}
		else {
			const rows2 = Array(size);
			for (let rowIndex2 = 0; rowIndex2 < size; rowIndex2++) {
				rows2[rowIndex2] = _.cloneDeep(row0);
			}
			// console.log(rows2)
			assignRowsByNamedValue(rows2, _.range(size), name, value, randomEngine);
			nestedRows[rowIndex] = _.flattenDeep(rows2);
		}
	}
}

function branchRowByArray(nestedRows, rowIndex, values, randomEngine) {
	// console.log(`branchRowByArray: ${JSON.stringify(values)}`);
	const size = values.length;
	// Make replicates of row
	const row0 = nestedRows[rowIndex];
	const rows2 = Array(size);
	for (let rowIndex2 = 0; rowIndex2 < size; rowIndex2++) {
		const value = values[rowIndex2];
		rows2[rowIndex2] = _.cloneDeep(row0);
		expandRowsByObject(rows2, [rowIndex2], values[rowIndex2], randomEngine);
	}

	nestedRows[rowIndex] = _.flattenDeep(rows2);
}

// Set the given value, but only if the name doesn't start with a period
function setColumnValue(row, name, value) {
	// console.log(`setColumnValue: ${name}, ${JSON.stringify(value)}`);
	// console.log("row: "+JSON.stringify(row))
	if (name.length >= 1 && name[0] != ".") {
		// Recurse into sub-rows
		if (_.isArray(row)) {
			// console.log("isArray")
			for (let i = 0; i < row.length; i++) {
				setColumnValue(row[i], name, value);
			}
		}
		// Set the value in the row
		else {
			row[name] = value;
			// console.log(`row[name] = ${row[name]}`)
		}
	}
}

class Special {
	constructor({action, draw, reuse, randomEngine}, next, initGroup) {
		this.action = action;
		this.draw = draw;
		this.reuse = reuse;
		this.randomEngine = randomEngine;
		this.next = (next || this.defaultNext);
		this.initGroup = (initGroup || this.defaultInitGroup);
	}

	defaultInitGroup(nestedRows, rowIndexes) {
		this.nextIndex = 0;
		this.valueCount = _.size(this.action.values);
		// Initialize this.indexes
		switch (this.draw) {
			case "direct":
				this.indexes = _.range(this.valueCount);
				break;
			case "shuffle":
				if (this.action.shuffleOnce !== true || !this.indexes) {
					this.indexes = Random.sample(this.randomEngine, _.range(this.valueCount), this.valueCount);
				} else {
					// FIXME: if this.valueCount is now larger than this.indexes.length, then generate more indexes
				}
				break;
		}
	}

	defaultNext() {
		// console.log({this});
		if (this.nextIndex >= this.valueCount) {
			switch (this.reuse) {
				case "restart":
					this.nextIndex = 0;
					break;
				case "reverse":
					this.indexes = _.reverse(this.indexes);
					this.nextIndex = 0;
					break;
				case "reshuffle":
					this.indexes = Random.sample(this.randomEngine, _.range(this.valueCount), this.valueCount);
					this.nextIndex = 0;
					// console.log("shuffled indexes: "+this.indexes)
					break;
				default:
					assert(false, "not enough values supplied to fill the rows: "+JSON.stringify(this.action));
			}
		}

		let index, key;
		switch (this.draw) {
			case "direct":
				index = this.indexes[this.nextIndex];
				key = index + 1;
				break;
			case "shuffle":
				index = this.indexes[this.nextIndex];
				key = index + 1;
				break;
			case "sample":
				index = Random.integer(0, this.valueCount - 1)(this.randomEngine);
				key = index + 1;
				break;
			default:
				assert(false, "unknown 'draw' value: "+JSON.stringify(this.draw)+" in "+JSON.stringify(this.action));
		}

		const value = this.action.values[index];
		if (this.draw !== "sample") {
			this.nextIndex++;
		}

		return [key, value];
	}
}

/*
function countRows(nestedRows, rowIndexes) {
	let sum = 0;
	for (let i = 0; i < rowIndexes.length; i++) {
		const rowIndex = rowIndexes[i];
		const row = nestedRows[rowIndex];
		if (_.isPlainObject(row)) {
			sum++;
		}
		else {
			sum += countRows(row, _.range(row.length));
		}
	}
	return sum;
}*/

const actionHandlers = {
	"allocatePlates": (rows, rowIndexes, name, action, randomEngine) => {
		const action2 = _.cloneDeep(action);
		return assign(rows, rowIndexes, name, action2, randomEngine, undefined, assign_allocatePlates_initGroup);
	},
	"allocateWells": (_rows, rowIndexes, name, action, randomEngine) => {
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
		const action2 = _.cloneDeep(action);
		action2.values = values;
		// console.log({values})
		return assign(_rows, rowIndexes, name, action2, randomEngine);
	},
	"assign": assign,
	"calculate": (rows, rowIndexes, name, action, randomEngine) => {
		return assign(rows, rowIndexes, name, {}, randomEngine, assign_calculate_next(action, {}));
	},
	"calculateWell": (rows, rowIndexes, name, action, randomEngine) => {
		return assign(rows, rowIndexes, name, {}, randomEngine, assign_calculateWell_next(action, {}));
	},
	"range": (rows, rowIndexes, name, action, randomEngine) => {
		const action2 = _.cloneDeep(action);
		_.defaults(action2, {from: 1, step: 1});
		return assign(rows, rowIndexes, name, action2, randomEngine, undefined, assign_range_initGroup);
	},
	"sample": {

	}
}

function assign(rows, rowIndexes, name, action, randomEngine, next, initGroup) {
	assert(_.isPlainObject(action), "expect an object for assignment")
	// Handle order in which to assign values
	let draw = "direct";
	let reuse = "none";
	if (!_.isEmpty(action.order)) {
		switch (action.order) {
			case "direct": case "direct/none": break;
			case "direct/restart": case "restart": draw = "direct"; reuse = "restart"; break;
			case "direct/reverse": case "reverse": draw = "direct"; reuse = "reverse"; break;
			case "shuffle": draw = "shuffle"; reuse = "none"; break;
			case "reshuffle": draw = "shuffle"; reuse = "reshuffle"; break;
			case "shuffle/reshuffle": draw = "shuffle"; reuse = "reshuffle"; break;
			case "shuffle/restart": draw = "shuffle"; reuse = "restart"; break;
			case "shuffle/reverse": draw = "shuffle"; reuse = "reverse"; break;
			case "sample": draw = "sample"; break;
			default: assert(false, "unrecognized 'order' value: "+action.order);
		}
	}

	if (_.isString(action.calculate)) {
		next = assign_calculate_next(action.calculate, action)
	}

	const randomEngine2 = (_.isNumber(action.randomSeed))
		? Random.engines.mt19937().seed(action.randomSeed)
		: randomEngine;
	const value2 = ((_.isArray(action.values)) && draw === "direct" && reuse === "none" && !initGroup)
		? action.values
		: new Special({action, draw, reuse, randomEngine: randomEngine2}, next, initGroup);

	return handleAssignmentWithQueries(rows, rowIndexes, name, action, randomEngine2, value2);
}

function handleAssignmentWithQueries(rows, rowIndexes, name, action, randomEngine, value) {
	const isSpecial = value instanceof Special;
	if (!action.groupBy && !action.sameBy) {
		if (isSpecial) {
			// console.log({name})
			value.initGroup(rows, rowIndexes);
		}
		return value;
	}
	else {
		// console.log("GROUP!")

		if (action.groupBy) {
			// printRows(rows);
			const rowIndexesGroups = query_groupBy(rows, rowIndexes, action.groupBy);
			// console.log({rowIndexesGroups})
			for (let i = 0; i < rowIndexesGroups.length; i++) {
				const rowIndexes2 = rowIndexesGroups[i];

				if (action.sameBy) {
					assignSameBy(rows, rowIndexes2, name, action, randomEngine, value);
				}
				else {
					if (isSpecial) {
						// console.log({rows, rowIndexes2})
						value.initGroup(rows, rowIndexes2);
					}
					expandRowsByNamedValue(rows, rowIndexes2, name, value, randomEngine);
				}
			}
			return undefined;
		}
		else if (action.sameBy) {
			assignSameBy(rows, rowIndexes, name, action, randomEngine, value);
		}
		else {
			if (isSpecial) {
				value.initGroup(rows, rowIndexes);
			}
			return value;
		}
	}
}

function assignSameBy(rows, rowIndexes, name, action, randomEngine, value) {
	// console.log(`assignSameBy: ${rowIndexes}, ${name}, ${action.sameBy}, ${JSON.stringify(value)}`)
	const isArray = _.isArray(value);
	const isObject = _.isPlainObject(value);
	const isSpecial = value instanceof Special;
	const rowIndexesSame = query_groupBy(rows, rowIndexes, action.sameBy);
	// console.log({rowIndexesSame})

	/*
	for (let i = 0; i < rowIndexesSame.length; i++) {
		const rowIndexes2 = rowIndexesSame[i];
		const rowIndex = rowIndexes2[0];
		const rows2 = rowIndexes2.map(i => rows[i]);
		rows.splice(rowIndex, 1, ..rows2);
		for (let j = 0; j < rowIndexesSame.length;)
	}
	*/
	if (isSpecial) {
		const rowIndexesFirst = rowIndexesSame.map(l => l[0]);
		value.initGroup(rows, rowIndexesFirst);
	}

	const keys = (isObject) ? _.keys(value) : 0;
	const table2 = _.zip.apply(_, table2);
	for (let i = 0; i < rowIndexesSame.length; i++) {
		const rowIndexes3 = rowIndexesSame[i];
		if (isSpecial) {
			value.nextIndex = i;
		}
		const value2
			= (isArray) ? value[i]
			: (isObject) ? value[keys[i]]
			: (isSpecial) ? value.next(rows, [rowIndexes3[0]])[1]
			: value;
		// console.log({i, rowIndexes3, value2})
		for (let i = 0; i < rowIndexes3.length; i++) {
			const rowIndex = rowIndexes3[i];
			expandRowsByNamedValue(rows, [rowIndex], name, value2, randomEngine);
		}
	}
}

function assign_allocatePlates_initGroup(rows, rowIndexes) {
	const action = this.action;
	if (_.isUndefined(this.plateIndex))
		this.plateIndex = 0;
	if (_.isUndefined(this.wellsUsed))
		this.wellsUsed = 0;

	assert(rowIndexes.length <= action.wellsPerPlate, "too many positions in group for plate to accomodate");

	if (this.wellsUsed + rowIndexes.length <= action.wellsPerPlate) {
		this.wellsUsed += rowIndexes.length;
	}
	else {
		this.plateIndex++;
		assert(this.plateIndex < action.plates.length, `require more plates than the ${action.plates.length} supplied: ${action.plates.join(", ")}`);
		this.wellsUsed = rowIndexes.length;
	}

	// TODO: allow for rotating plates for each group rather than assigning each plate until its full

	this.action.values = _.fill(Array(rowIndexes.length), action.plates[this.plateIndex]);
	// console.log({this_action_values: this.action.values});

	this.defaultInitGroup(rows, rowIndexes);
}

function assign_calculate_next(expr, action) {
	return function(nestedRows, rowIndex) {
		const row0 = nestedRows[rowIndex];
		const row = (_.isArray(row0)) ? _.head(_.flattenDeep(row0)) : row0;
		// Build the scope for evaluating the math expression from the current data row
		const scope = _.mapValues(row, x => {
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

		assert(!_.isUndefined(expr), "`expression` property must be specified");
		// console.log("scope:"+JSON.stringify(scope, null, '\t'))
		let value = math.eval(expr, scope);
		// console.log({type: value.type, value})
		if (_.isString(value) || _.isNumber(value)) {
			return [0, value];
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
			: unitless.toNumber();

		// Set units
		const valueText = (!_.isEmpty(units))
			? unitlessText + " " + units
			: unitlessText;

		this.nextIndex++;
		return [this.nextIndex, valueText];
	}
}

function assign_calculateWell_next(action) {
	return function(nestedRows, rowIndex) {
		const row0 = nestedRows[rowIndex];
		const row = (_.isArray(row0)) ? _.head(_.flattenDeep(row0)) : row0;
		// Build the scope for evaluating the math expression from the current data row
		const scope = _.mapValues(row, x => {
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

		// console.log("scope:"+JSON.stringify(scope, null, '\t'))
		let wellRow = math.eval(action.row, scope).toNumber();
		let wellCol = math.eval(action.column, scope).toNumber();
		const wellName = locationRowColToText(wellRow, wellCol);
		// console.log({wellRow, wellCol, wellName})

		this.nextIndex++;
		return [this.nextIndex, wellName];
	}
}

function assign_range_initGroup(rows, rowIndexes) {
	// console.log(`assign_range_initGroup:`, {rows, rowIndexes})
	const commonHolder = []; // cache for common values, if needed
	const from = getOrCalculateNumber(this.action, "from", rowIndexes.length, rows, rowIndexes, commonHolder);
	const till = getOrCalculateNumber(this.action, "till", rowIndexes.length, rows, rowIndexes, commonHolder);
	const end = till + 1;

	let values;
	if (_.isNumber(this.action.count)) {
		const diff = till - from;
		values = _.range(this.action.count).map(i => {
			const d = diff * i / (this.action.count - 1);
			return from + d;
		});
	}
	else {
		values = _.range(from, end, this.action.step);
	}
	if (values) {
		if (_.isNumber(this.action.decimals)) {
			values = values.map(n => Number(n.toFixed(this.action.decimals)));
		}
		if (_.isString(this.action.units)) {
			values = values.map(n => `${n} ${this.action.units}`);
		}
	}
	this.action.values = values;
	// console.log({this_action_values: this.action.values});

	this.defaultInitGroup(rows, rowIndexes);
}
/*
function assign_range_next(nestedRows, rowIndex) {
	const commonHolder = []; // cache for common values, if needed
	const from = getOrCalculateNumber(this.action, "from", rowIndexes.length, nestedRows, [rowIndex], commonHolder);
	const till = getOrCalculateNumber(this.action, "till", rowIndexes.length, nestedRows, [rowIndex], commonHolder);
	const end = till + 1;

	let values;
	if (_.isNumber(this.action.count)) {
		const diff = till - from;
		values = _.range(this.action.count).map(i => {
			const d = diff * i / (this.action.count - 1);
			return from + d;
		});
	}
	else {
		values = _.range(from, end, this.action.step);
	}
	if (values) {
		if (_.isNumber(this.action.decimals)) {
			values = values.map(n => Number(n.toFixed(this.action.decimals)));
		}
		if (_.isString(this.action.units)) {
			values = values.map(n => `${n} ${this.action.units}`);
		}
	}
	console.log({values});

	this.nextIndex++;
	return [this.nextIndex, values];
}
*/
function getOrCalculateNumber(action, propertyName, dflt, nestedRows, rowIndexes, commonHolder) {
	const value = _.get(action, propertyName);
	if (_.isUndefined(value)) {
		return dflt;
	}
	else if (_.isNumber(value)) {
		return value;
	}
	else if (_.isString(value)) {
		const options = {};
		const next = assign_calculate_next(value, options);
		const fakethis = {nextIndex: 0};
		if (commonHolder.length === 0) {
			// console.log({nestedRows, rowIndexes})
			commonHolder.push(commonHolder.push(getCommonValuesNested(nestedRows, rowIndexes)));
		}
		const common = commonHolder[0];
		// console.log({common})
		const [dummyIndex, result] = next.bind(fakethis)([common], [0]);
		return result;
	}
}

/*
const assign_range_next = (expr, action) => function(nestedRows, rowIndex) {
	console.log("assign_range_next: "+JSON.stringify(this));
	const n = this.action.from + this.nextIndex * this.action.step;
	assert(!_.isNumber(this.action.till) || n < this.action.till, "range could not fill rows");
	this.nextIndex++;
	return [this.nextIndex, n];
}*/

export function query_groupBy(rows, rowIndexes, groupBy) {
	const groupKeys = (_.isArray(groupBy)) ? groupBy : [groupBy];
	// console.log({groupBy, groupKeys, rowIndexes, rows});
	return _.values(_.groupBy(rowIndexes, rowIndex => _.map(groupKeys, key => rows[rowIndex][key])));
}

export function query(table, q) {
	let table2 = _.clone(table);
	if (q.select) {
		table2 = _.map(table2, x => _.pick(x, q.select));
	}

	if (q.where) {
		_.forEach(q.where, (value, key) => {
			if (_.isPlainObject(value)) {
				const op = _.keys(value)[0];
				const x = value[op];
				switch (op) {
					case "eq":
						table2 = _.filter(table, row => _.isEqual(row[key], x));
						break;
					case "gt":
						// console.log("before:"); printRows(table2);
						table2 = _.filter(table, row => _.gt(row[key], x));
						// console.log("after:"); printRows(table2);
						break;
					case "gte":
						table2 = _.filter(table, row => _.gte(row[key], x));
						break;
					default:
						assert(false, `unrecognized operator: ${op} in ${JSON.stringify(x)}`);
				}
			}
			else {
				table2 = _.filter(table, row => _.isEqual(row[key], value));
			}
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
