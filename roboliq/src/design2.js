import _ from 'lodash';
import assert from 'assert';
// import Immutable, {Map, fromJS} from 'immutable';
// import math from 'mathjs';
import Random from 'random-js';
//import yaml from 'yamljs';

// import {locationRowColToText} from './parsers/wellsParser.js';


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
	return expandConditions(design.conditions, randomEngine);
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
			console.log({x})
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
	console.log("expandConditions: "+JSON.stringify(conditions))
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
	console.log(`expandRowsByNamedValue: ${name}, ${JSON.stringify(value)}`);
	console.log({rowIndexes})
	console.log(nestedRows)
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

	// TODO: turn the name/value into an action in order to allow for more sophisticated expansion
	if (_.endsWith(name, "*")) {
		branchRowsByNamedValue(nestedRows, rowIndexes, name.substr(0, name.length - 1), value, randomEngine);
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
	console.log(`assignRowsByNamedValue: ${name}, ${JSON.stringify(value)}`);
	printRows(nestedRows)
	console.log({rowIndexes})
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
	console.log(`assignRowByNamedValuesKey: ${name}, ${JSON.stringify(values)}, ${valueKeyIndex}`);
	const row = nestedRows[rowIndex];
	let n = 0;
	if (_.isArray(row)) {
		for (let i = 0; i < row.length; i++) {
			const n2 = assignRowByNamedValuesKey(row, i, name, values, valueKeyIndex, valueKeys, randomEngine);
			n += n2;
			valueKeyIndex += n2;
		}
	}
	else {
		let item, key;
		if (values instanceof Special) {
			[key, item] = values.next();
		}
		else {
			assert(valueKeyIndex < _.size(values), "fewer values than rows: "+JSON.stringify({name, values}));
			const valueKey = (valueKeys) ? valueKeys[valueKeyIndex] : valueKeyIndex;
			key = (valueKeys) ? valueKey : valueKey + 1;
			item = values[valueKey];
		}

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
	console.log(`branchRowsByNamedValue: ${name}, ${JSON.stringify(value)}`); console.log(JSON.stringify(nestedRows))
	const size
		= (_.isArray(value)) ? value.length
		: (_.isPlainObject(value)) ? _.size(value)
		: 1;
	for (let i = 0; i < rowIndexes.length; i++) {
		const rowIndex = rowIndexes[i];
		// Make replicates of row
		const row0 = nestedRows[rowIndex];
		if (_.isArray(row0)) {
			branchRowsByNamedValue(row0, _.range(row0.length), name, value, randomEngine);
		}
		else {
			const rows2 = Array(size);
			for (let rowIndex2 = 0; rowIndex2 < size; rowIndex2++) {
				rows2[rowIndex2] = _.cloneDeep(row0);
			}
			console.log(rows2)
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
	constructor({action, draw, reuse, randomEngine}, next) {
		this.action = action;
		this.draw = draw;
		this.reuse = reuse;
		this.randomEngine = randomEngine;
		this.nextIndex = 0;
		this.valueCount = _.size(this.action.values);
		this.next = (next || this.defaultNext);

		// Initialize this.indexes
		switch (this.draw) {
			case "direct":
				this.indexes = _.range(this.valueCount);
				break;
			case "shuffle":
				this.indexes = Random.sample(this.randomEngine, _.range(this.valueCount), this.valueCount);
				break;
		}
	}

	defaultNext() {
		console.log({this});
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
					console.log("shuffled indexes: "+this.indexes)
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

		const value = this.action.values[index]
		if (this.draw !== "sample") {
			this.nextIndex++;
		}

		return [key, value];
	}
}

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
}

const actionHandlers = {
	"assign":  (nestedRows, rowIndexes, name, action, randomEngine) => {
		assert(_.isArray(action.values), "should have 'values' array: "+JSON.stringify(action));
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
			}
		}
		const randomSeed = action.randomSeed;
		if (draw === "direct" && reuse === "none") {
			return action.values;
		}
		else {
			console.log("SPECIAL!")
			const randomEngine2 = (_.isNumber(randomSeed))
				? Random.engines.mt19937().seed(randomSeed)
				: randomEngine;
			return new Special({action, draw, reuse, randomEngine: randomEngine2});
		}
	},
	"range": (nestedRows, rowIndexes, name, action, randomEngine) => {
		let till = action.till;
		if (_.isUndefined(till)) {
			till = countRows(nestedRows, rowIndexes);
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
			return range;
		}
	},
	"sample": {

	}
}
