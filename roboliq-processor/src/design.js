'use babel';

/**
 * Functions for processing design specifications.
 * In particular, it can take concise design specifications and expand them
 * into a long table of factor values.
 * @module
 */
import _ from 'lodash';
import assert from 'assert';
// import Immutable, {Map, fromJS} from 'immutable';
import math from 'mathjs';
import naturalSort from 'javascript-natural-sort';
import Random from 'random-js';
import stableSort from 'stable';
//import yaml from 'yamljs';

import wellsParser from './parsers/wellsParser.js';


const DEBUG = false;

//import {locationRowColToText} from './parsers/wellsParser.js';
// FIXME: HACK: this function is included here temporarily, to make usage in react component easier for the moment
function locationRowColToText(row, col) {
	var colText = col.toString();
	if (colText.length == 1) colText = "0"+colText;
	return String.fromCharCode("A".charCodeAt(0) + row - 1) + colText;
}

/**
 * Print a text representation of the table
 * @param  {array}  rows - array of rows
 * @param  {Boolean} [hideRedundancies] - suppress printing of values that haven't changed from the previous row
 */
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
			// console.log(JSON.stringify(row))
			const line = _.map(columns, key => {
				const x1 = _.get(row, key, "");
				const x2 = (_.isNull(x1)) ? "" : x1;
				return x2.toString();
			});
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

/**
 * Print a TAB-formatted representation of the table
 * @param  {array}  rows - array of rows
 */
export function printTAB(rows) {
	const hideRedundancies = false;
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
			// console.log(JSON.stringify(row))
			const line = _.map(columns, key => {
				const x1 = _.get(row, key, "");
				const x2 = (_.isNull(x1)) ? "" : x1;
				return x2.toString();
			});
			lines.push(line);
		});
	});

	console.log(columns.join("\t"));
	_.forEach(lines, line => {
		const s = line.join("\t");
		console.log(s);
	});
}

/**
 * Print a markdown pipe table
 * @param  {array}  rows - array of rows
 */
export function printMarkdown(rows) {
	const hideRedundancies = false;
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
			// console.log(JSON.stringify(row))
			const line = _.map(columns, key => {
				const x1 = _.get(row, key, "");
				const x2 = (_.isNull(x1)) ? "" : x1;
				return x2.toString();
			});
			lines.push(line);
		});
	});

	console.log(columns.join(" | "));
	console.log(columns.map(s => ":-----:").join(" | "));
	_.forEach(lines, line => {
		const s = line.join(" | ");
		console.log(s);
	});
	console.log();
}

/**
 * Turn a design specification into a design table.
 * @param {object} design - the design specification.
 */
export function flattenDesign(design, randomEngine) {
	if (_.isEmpty(design)) {
		return [];
	}

	randomEngine = randomEngine || Random.engines.mt19937();
	const randomSeed = _.isNumber(design.randomSeed) ? design.randomSeed : 0;
	randomEngine.seed(randomSeed);

	let children;
	if (_.isArray(design.children)) {
		children = design.children.map(child => flattenDesign(child, randomEngine));
	}
	else {
		const conditionsList = _.isArray(design.design) ? design.design : [design.design];
		children = conditionsList.map(conditions => expandConditions(conditions, randomEngine, design.initialRows));
	}

	let rows;
	if (children.length == 1) {
		rows = children[0];
	}
	// If there were multiple children, we'll need to join them: either merge columns or concat rows
	else {
		// console.log(JSON.stringify(children, null, '\t'))
		const joinMethod = design.join || "concat";
		if (joinMethod === "merge") {
			rows = _.merge.apply(_, [[]].concat(children));
		}
		else {
			rows = [].concat(...children);
		}
	}

	if (design.where) {
		rows = filterOnWhere(rows, design.where);
	}
	if (design.orderBy) {
		rows = _.orderBy(rows, design.orderBy);
	}
	if (design.select) {
		rows = rows.map(row => _.pick(row, design.select));
	}
	return rows;
}

export function getCommonValues(table) {
	if (_.isEmpty(table)) return {};
	assert(_.isArray(table), `required an array: ${JSON.stringify(table)}`);

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
 * @param {array} [otherRowIndexes] - a second, optional array of row indexes that should have the same modifications made to it as rowIndexes
 * @param {integer} rowIndexesOffset - index in rowIndexes to start at
 */
export function flattenArrayAndIndexes(rows, rowIndexes, otherRowIndexes = []) {
	if (DEBUG) {
		console.log(`flattenArrayAndIndexes:`);
		console.log(` otherRowIndexes: ${JSON.stringify(otherRowIndexes)}`);
		console.log(` rowIndexes: ${JSON.stringify(rowIndexes)}\n ${JSON.stringify(rows)}`)
	}
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
			// console.log(` 1: ${rowIndexes.join(",")}`)
			const x = _.range(rowIndex, rowIndex + item.length);
			// console.log({x})
			rowIndexes.splice(i, 1, ...x);
			// console.log(` 2: ${rowIndexes.join(",")}`)

			for (let m = 0; m < otherRowIndexes.length; m++) {
				const rowIndexes2 = otherRowIndexes[m];
				let k = -1;
				for (let j = 0; j < rowIndexes2.length; j++) {
					if (rowIndexes2[j] === rowIndex) {
						k = j;
					}
					else if (rowIndexes2[j] > rowIndex) {
						rowIndexes2[j] += item.length - 1;
					}
				}
				if (k >= 0) {
					const x = _.range(rowIndex, rowIndex + item.length);
					// console.log({x})
					rowIndexes2.splice(k, 1, ...x);
				}
				// console.log({m, len: otherRowIndexes.length, k})
				// console.log(` 3 otherRowIndexes: ${JSON.stringify(otherRowIndexes)}`);
			}

			i += item.length;
		}
		else {
			i++;
		}
		// console.log(` 4 otherRowIndexes: ${JSON.stringify(otherRowIndexes)}`);
	}
	if (DEBUG) { console.log(" (flattenArrayAndIndexes) otherRowIndexes: "+JSON.stringify(otherRowIndexes)); }
}

/**
 * If conditions is an array, then each element will be processed individually and then the results will be merged together.
 * @param {object|array} conditions - an object of conditions or an array of such objects.
 * @param {array} table0 - the initial rows to start expanding conditions on (default `[{}]`)
 */
export function expandConditions(conditions, randomEngine, table0 = [{}]) {
	// console.log("expandConditions: "+JSON.stringify(conditions))

	const conditionsList = _.isArray(conditions) ? conditions : [conditions];
	const conditionsRows = conditionsList.map(conditions => {
		const table = _.cloneDeep(table0);
		expandRowsByObject(table, _.range(0, table.length), [], conditions, randomEngine);
		flattenArrayM(table);
		return table;
	});

	let table = (conditionsRows.length == 1)
		? conditionsRows[0]
		: _.merge.apply(_, [[]].concat(conditionsRows));  // should probably be `_.merge([], ...conditionsRows)`
	return table;
}

/**
 * expandRowsByObject:
 *   for each key/value pair, call expandRowsByNamedValue
 */
function expandRowsByObject(nestedRows, rowIndexes, otherRowIndexes, conditions, randomEngine) {
	if (DEBUG) {
		console.log("expandRowsByObject: "+JSON.stringify(conditions));
		console.log(` otherRowIndexes: ${JSON.stringify(otherRowIndexes)}`);
		console.log(` rowIndexes: ${rowIndexes}\n ${JSON.stringify(nestedRows)}`)
		assertNoDuplicates(otherRowIndexes);
	}
	for (let name in conditions) {
		expandRowsByNamedValue(nestedRows, rowIndexes, otherRowIndexes, name, conditions[name], randomEngine);
	}
}

/**
 * // REQUIRED by: expandRowsByObject
 * expandRowsByNamedValue:
 *   TODO: turn the name/value into an action in order to allow for more sophisticated expansion
 *   if has star-suffix, call branchRowsByNamedValue
 *   else call assignRowsByNamedValue
 */
export function expandRowsByNamedValue(nestedRows, rowIndexes, otherRowIndexes, name, value, randomEngine) {
	if (DEBUG) {
		console.log(`expandRowsByNamedValue: ${name}, ${JSON.stringify(value)}`);
		console.log(` otherRowIndexes: ${JSON.stringify(otherRowIndexes)}`);
		console.log(` rowIndexes: ${JSON.stringify(rowIndexes)}\n ${JSON.stringify(nestedRows)}`)
		assertNoDuplicates(otherRowIndexes);
		assertNoDuplicates(otherRowIndexes.concat([rowIndexes]));
	}
	// If an action is specified using the "=" symbol:
	const iEquals = name.indexOf("=");
	if (iEquals >= 0) {
		// Need to flatten the rows in case the action uses groupBy or sameBy
		flattenArrayAndIndexes(nestedRows, rowIndexes, otherRowIndexes);
		// console.log(` 'otherRowIndexes: ${JSON.stringify(otherRowIndexes)}`);
		// console.log(` 'rowIndexes: ${JSON.stringify(rowIndexes)}\n ${JSON.stringify(nestedRows)}`)
		const actionType = name.substr(iEquals + 1) || "assign";
		const actionHandler = actionHandlers[actionType];
		assert(actionHandler, `unknown action type: ${actionType} in ${name}`)
		name = name.substr(0, iEquals);

		const result = actionHandler(nestedRows, rowIndexes, otherRowIndexes, name, value, randomEngine);
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

		// console.log({loc: "A", rowIndexes, nestedRows})
		branchRowsByNamedValue(nestedRows, rowIndexes, otherRowIndexes, name, value, randomEngine);
		// console.log("B")
	}
	else {
		assignRowsByNamedValue(nestedRows, rowIndexes, otherRowIndexes, name, value, randomEngine, true);
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
function assignRowsByNamedValue(nestedRows, rowIndexesGroups, otherRowIndexes, name, value, randomEngine, doUnnest = true) {
	const l = (_.every(rowIndexesGroups, l => _.isArray(l))) ? rowIndexesGroups : [rowIndexesGroups];
	if (DEBUG) {
		console.log(`assignRowsByNamedValue: ${name}, ${JSON.stringify(value)}`);
		console.log(` otherRowIndexes: ${JSON.stringify(otherRowIndexes)}`);
		console.log(` rowIndexesGroups: ${JSON.stringify(rowIndexesGroups)}\n ${JSON.stringify(nestedRows)}`)
		assertNoDuplicates(otherRowIndexes);
		assertNoDuplicates(otherRowIndexes.concat(l));
		//printRows(nestedRows)
	}
	const otherRowIndexes2 = otherRowIndexes.concat(l);
	for (let il = 0; il < l.length; il++) {
		const rowIndexes = _.clone(l[il]);
		// console.log({il, rowIndexes, l})
		let valueIndex = 0;
		const isSpecial = value instanceof Special;
		if (isSpecial) {
			value.reset();
		}
		/*// If value is an array of objects
		if (_.isArray(value) && _.every(value, x => _.isObject(x))) {
			// Assign indexes
			const valueIndexes = _.range(1, value.length + 1);
			assignRowsByNamedValue(nestedRows, rowIndexes, otherRowIndexes, name, valueIndexes, randomEngine, doUnnest);
			// Assign objects
			expandRowsByObject(nestedRows, rowIndexes, otherRowIndexes, item, randomEngine);
		}
		else*/ if (isSpecial || _.isArray(value)) {
			for (let i = 0; i < rowIndexes.length; i++) {
				const rowIndex = rowIndexes[i];
				const rowIndexes2 = [rowIndex];
				const n = assignRowByNamedValuesKey(nestedRows, rowIndex, otherRowIndexes.concat([rowIndexes, rowIndexes2]), name, value, valueIndex, undefined, randomEngine, doUnnest);
				valueIndex += n;
				i += rowIndexes2.length - 1;
				if (DEBUG) {
					console.log({rowIndex, dRowIndex: rowIndexes2.length, dValueIndex: n, valueIndex, i, rowIndexes})
					console.log(` (assignRowsByNamedValue:) rowIndexes: ${rowIndexes.join(", ")}`)
				}
			}
		}
		else if (_.isPlainObject(value)) {
			let valueIndex = 0;
			const keys = _.keys(value);
			for (let i = 0; i < rowIndexes.length; i++) {
				const rowIndex = rowIndexes[i];
				const rowIndexes2 = [rowIndex];
				const n = assignRowByNamedValuesKey(nestedRows, rowIndex, otherRowIndexes.concat([rowIndexes, rowIndexes2]), name, value, valueIndex, keys, randomEngine, doUnnest);
				valueIndex += n;
				i += rowIndexes2.length - 1;
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
 * Returns number of values actually assigned (may be more than one for nested rows)
 */
function assignRowByNamedValuesKey(nestedRows, rowIndex, otherRowIndexes, name, values, valueKeyIndex, valueKeys, randomEngine, doUnnest) {
	assert(!_.isUndefined(doUnnest));
	if (DEBUG) {
		console.log(`assignRowByNamedValuesKey: ${name}, ${JSON.stringify(values)}, ${valueKeyIndex}, ${valueKeys}`);
		console.log(` otherRowIndexes: ${JSON.stringify(otherRowIndexes)}`);
		console.log(` rowIndex: ${rowIndex}\n ${JSON.stringify(nestedRows)}`)
		assertNoDuplicates(otherRowIndexes);
	}
	const row = nestedRows[rowIndex];
	let n = (doUnnest) ? 0 : 1;
	if (_.isArray(row)) {
		// console.log("0")
		const otherRowIndexes2 = otherRowIndexes.concat([_.range(row.length)]);
		for (let i = 0; i < row.length; i++) {
			// console.log(` (assignRowByNamedValuesKey) #${i} of ${row.length}`)
			const n2 = assignRowByNamedValuesKey(row, i, otherRowIndexes2, name, values, valueKeyIndex, valueKeys, randomEngine, doUnnest);
			if (doUnnest) {
				n += n2;
				valueKeyIndex += n2;
			}
			// console.log({i, n2, n, valueKeyIndex, row})
		}
		flattenArrayAndIndexes(nestedRows, [rowIndex], otherRowIndexes);
	}
	else {
		// Error.stackTraceLimit = Infinity;

		// console.log("A")
		let item, key;
		if (values instanceof Special) {
			// console.log("B: "+rowIndex)
			// console.log(nestedRows[rowIndex])
			const result = values.next(nestedRows, rowIndex);
			// console.log({result})
			key = result[0];
			item = result[1];
			// [key, item] = result;
		}
		else {
			// console.log("C")
			assert(valueKeyIndex < _.size(values), `fewer values (${_.size(values)}) than rows: `+JSON.stringify({name, values}));
			const valueKey = (valueKeys) ? valueKeys[valueKeyIndex] : valueKeyIndex;
			key = (valueKeys) ? valueKey : valueKey + 1;
			item = values[valueKey];
		}

		// console.log("D")
		// console.log({item})
		const rowIndexes2 = [rowIndex];
		if (_.isArray(item)) {
			setColumnValue(row, name, key);
			branchRowByArray(nestedRows, rowIndex, otherRowIndexes, item, randomEngine);
		}
		else if (_.isPlainObject(item)) {
			setColumnValue(row, name, key);
			expandRowsByObject(nestedRows, rowIndexes2, otherRowIndexes, item, randomEngine);
		}
		else {
			setColumnValue(row, name, item);
		}
		// n = rowIndexes2.length
		n = 1;
	}
	if (DEBUG) {
		console.log(` (assignRowByNamedValuesKey): ${JSON.stringify(nestedRows)}`)
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
function branchRowsByNamedValue(nestedRows, rowIndexes, otherRowIndexes, name, value, randomEngine) {
	if (DEBUG) {
		console.log(`branchRowsByNamedValue: ${name}, ${JSON.stringify(value)}`);
		console.log(` otherRowIndexes: ${JSON.stringify(otherRowIndexes)}`);
		console.log(` rowIndexes: ${JSON.stringify(rowIndexes)}\n ${JSON.stringify(nestedRows)}`)
	}
	const isSpecial = (value instanceof Special);
	const size
		= (_.isArray(value)) ? value.length
		: (_.isPlainObject(value)) ? _.size(value)
		: (isSpecial) ? value.valueCount
		: 1;

	//flattenArrayAndIndexes(nestedRows, rowIndexes, otherRowIndexes);

	// Create 'size' copies of each row in rowIndexes.
	const rows2 = Array(size * rowIndexes.length);
	const rowIndexesGroups2 = Array(rowIndexes.length);
	const rowIndexesGroups2Transposed = _.range(size).map(i => Array(rowIndexes.length));
	for (let j = 0; j < rowIndexes.length; j++) {
		const rowIndex = rowIndexes[j];
		rowIndexesGroups2[j] = Array(size);
		for (let i = 0; i < size; i++) {
			const k = j * size + i;
			rowIndexesGroups2[j][i] = k;
			rows2[k] = _.cloneDeep(nestedRows[rowIndex]);
			rowIndexesGroups2Transposed[i][j] = k;
		}
	}
	// console.log({rows2, rowIndexesGroups2, rowIndexesGroups2Transposed});

	if (_.isArray(value) && _.every(value, x => _.isPlainObject(x))) {
		// Assign indexes
		const valueIndexes = _.range(1, value.length + 1);
		assignRowsByNamedValue(rows2, rowIndexesGroups2, rowIndexesGroups2Transposed, name, valueIndexes, randomEngine, false);
		// Assign objects
		const otherRowIndexes3 = rowIndexesGroups2Transposed.concat(rowIndexesGroups2);
		for (let i = 0; i < size; i++) {
			const rowIndexes3 = _.clone(rowIndexesGroups2Transposed[i]);
			expandRowsByObject(rows2, rowIndexes3, otherRowIndexes3, value[i], randomEngine);
		}
	}
	else if (_.isPlainObject(value)) {
		// Assign indexes
		const valueIndexes = _.keys(value);
		assignRowsByNamedValue(rows2, rowIndexesGroups2, rowIndexesGroups2Transposed, name, valueIndexes, randomEngine, false);
		// Assign objects
		const otherRowIndexes3 = rowIndexesGroups2Transposed.concat(rowIndexesGroups2);
		for (let i = 0; i < size; i++) {
			const key = valueIndexes[i];
			const rowIndexes3 = _.clone(rowIndexesGroups2Transposed[i]);
			expandRowsByObject(rows2, rowIndexes3, otherRowIndexes3, value[key], randomEngine);
		}
	}
	else {
		// Assign to those copies
		assignRowsByNamedValue(rows2, rowIndexesGroups2, [], name, value, randomEngine, false);
	}
	// console.log({rows2, rowIndexesGroups2, rowIndexesGroups2Transposed});
	flattenArrayAndIndexes(rows2, [], rowIndexesGroups2);
	// console.log({rows2, rowIndexesGroups2});

	// console.log({nestedRows})

	// Transpose back into nestedRows
	for (let j = 0; j < rowIndexes.length; j++) {
		const rowIndexes3 = rowIndexesGroups2[j];
		const rows3 = rowIndexes3.map(i => rows2[i]);
		const rowIndex = rowIndexes[j];
		// console.log({rows3, rowIndex})
		nestedRows[rowIndex] = rows3;
	}
	// console.log({nestedRows})

	// console.log({loc: "B", otherRowIndexes})
	// console.log(JSON.stringify(nestedRows))
	flattenArrayAndIndexes(nestedRows, rowIndexes, otherRowIndexes);
	// console.log({loc: "C", otherRowIndexes})
}

function branchRowByArray(nestedRows, rowIndex, otherRowIndexes, values, randomEngine) {
	if (DEBUG) {
		console.log(`branchRowByArray: ${JSON.stringify(values)}`);
		console.log(` otherRowIndexes: ${JSON.stringify(otherRowIndexes)}`);
		console.log(` rowIndex: ${rowIndex}\n ${JSON.stringify(nestedRows)}`)
		assertNoDuplicates(otherRowIndexes);
	}
	const size = values.length;
	// Make replicates of row
	const row0 = nestedRows[rowIndex];
	const rows2 = Array(size);
	for (let rowIndex2 = 0; rowIndex2 < size; rowIndex2++) {
		const value = values[rowIndex2];
		rows2[rowIndex2] = _.cloneDeep(row0);
		expandRowsByObject(rows2, [rowIndex2], [], values[rowIndex2], randomEngine);
	}

	nestedRows[rowIndex] = _.flattenDeep(rows2);
	flattenArrayAndIndexes(nestedRows, [rowIndex], otherRowIndexes);
	if (DEBUG) {
		console.log(` (branchRowByArray): ${JSON.stringify(nestedRows)}`)
	}
}

// Set the given value, but only if the name doesn't start with a period
function setColumnValue(row, name, value) {
	if (DEBUG) { console.log(`setColumnValue: ${name}, ${JSON.stringify(value)}`); console.log("row: "+JSON.stringify(row)); }
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
		this.reset = this.defaultReset;
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
		// if (DEBUG) { console.log("defaultNext: "+)}
		// console.log({this});
		if (this.nextIndex >= this.valueCount) {
			// console.log(`next: this.nextIndex >= this.valueCount, ${this.nextIndex} >= ${this.valueCount}`)
			switch (this.reuse) {
				case "repeat":
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
			// console.log("this.nextIndex = "+this.nextIndex)
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
		// console.log({index, key})

		const value = this.action.values[index];
		if (this.draw !== "sample") {
			this.nextIndex++;
		}

		return [key, value];
	}

	defaultReset() {
		if (this.nextIndex) {
			this.nextIndex = 0;
		}
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

/**
 * If an action handler return 'undefined', it means that the handler took care of the action already.
 * @type {Object}
 */
const actionHandlers = {
	"allocatePlates": (rows, rowIndexes, otherRowIndexes, name, action, randomEngine) => {
		const action2 = _.cloneDeep(action);
		return assign(rows, rowIndexes, otherRowIndexes, name, action2, randomEngine, undefined, assign_allocatePlates_initGroup);
	},
	"allocateWells": (_rows, rowIndexes, otherRowIndexes, name, action, randomEngine) => {
		const rows = action.rows;
		const cols = action.columns;
		const rowJump = action.rowJump || 0; // whether to jump over rows (and then return to the skipped ones), and by how much
		assert(_.isNumber(rows) && rows > 0, "missing required positive number `rows`");
		assert(_.isNumber(cols) && cols > 0, "missing required positive number `columns`");
		assert(_.isNumber(rowJump) && rowJump >= 0, "`rowJump`, if specified, must be a number >= 0");
		const from0 = action.from || 1;
		let iFrom;
		if (_.isInteger(from0)) {
			iFrom = from0 - 1;
		}
		else {
			const [rowFrom, colFrom] = wellsParser.locationTextToRowCol(from0);
			iFrom = (colFrom - 1) * rows + (rowFrom - 1);
			// console.log({from0, rowFrom, colFrom, rows, iFrom})
		}

		let values;
		if (action.wells) {
			values = wellsParser.parse(action.wells, {}, {rows, columns: cols});
			// TODO: handle `from` for both cases of well name or for integer
		}
		else {
			const byColumns = _.get(action, "byColumns", true);
			values = _.range(iFrom, rows * cols).map(i => {
				const [row0, col] = (byColumns) ? [i % rows, Math.floor(i / rows)] : [Math.floor(i / cols), i % cols];
				// Calculate row when jumping
				const row1 = row0 * (rowJump + 1);
				const rowLayer = Math.floor(row1 / rows);
				const row = (row1 + rowLayer) % rows;
				const s = locationRowColToText(row + 1, col + 1);
				// console.log({row, col, s});
				return s;
			});
		}

		// console.log({values})
		const action2 = _.cloneDeep(action);
		action2.values = values;
		// console.log({values})
		return assign(_rows, rowIndexes, otherRowIndexes, name, action2, randomEngine);
	},
	"assign": assign,
	"case": (rows, rowIndexes, otherRowIndexes, name, action, randomEngine) => {
		if (DEBUG) {
			console.log(`=case: ${name}=${JSON.stringify(action)}`);
			console.log(` otherRowIndexes: ${JSON.stringify(otherRowIndexes)}`);
			console.log(` rowIndexes: ${JSON.stringify(rowIndexes)}\n ${JSON.stringify(rows)}`)
			assertNoDuplicates(otherRowIndexes);
			assertNoDuplicates(otherRowIndexes.concat([rowIndexes]));
		}
		const cases = _.isArray(action) ? action : action.cases;
		const caseMap = _.isArray(cases)
			? cases.map((v, i) => [i + 1, v])
			: _.toPairs(cases);
		// Group the rows according to the first case they satisfy
		const rowIndexesGroups = caseMap.map(x => []);
		for (let j = 0; j < rowIndexes.length; j++) {
			const rowIndex = rowIndexes[j];
			const row = rows[rowIndex];
			const table1 = [row];
			for (let i = 0; i < caseMap.length; i++) {
				const [caseName, caseSpec] = caseMap[i];
				if (!caseSpec.where || filterOnWhere(table1, caseSpec.where).length === 1) {
					rowIndexesGroups[i].push(rowIndex);
					break;
				}
			}
		}
		if (DEBUG) { console.log({caseMap, rowIndexesGroups}); }

		const otherRowIndexes2 = otherRowIndexes.concat([rowIndexes]).concat(rowIndexesGroups);
		for (let i = 0; i < caseMap.length; i++) {
			const [caseName, caseSpec] = caseMap[i];
			const rowIndexes2 = _.clone(rowIndexesGroups[i]);
			expandRowsByNamedValue(rows, rowIndexes2, otherRowIndexes2, name, caseName, randomEngine)
			expandRowsByObject(rows, rowIndexes2, otherRowIndexes2, caseSpec.design, randomEngine);
		}
	},
	"calculate": (rows, rowIndexes, otherRowIndexes, name, action, randomEngine) => {
		const expr = _.isString(action) ? action : action.expression;
		const action2 = _.isString(action) ? {} : action;
		return assign(rows, rowIndexes, otherRowIndexes, name, {}, randomEngine, assign_calculate_next(expr, action2));
	},
	// TODO: consider renaming to `calculateWellColumn`
	"calculateColumn": (rows, rowIndexes, otherRowIndexes, name, action, randomEngine) => {
		return assign(rows, rowIndexes, otherRowIndexes, name, {}, randomEngine, assign_calculateColumn_next(action, {}));
	},
	"calculateRow": (rows, rowIndexes, otherRowIndexes, name, action, randomEngine) => {
		return assign(rows, rowIndexes, otherRowIndexes, name, {}, randomEngine, assign_calculateRow_next(action, {}));
	},
	"calculateWell": (rows, rowIndexes, otherRowIndexes, name, action, randomEngine) => {
		return assign(rows, rowIndexes, otherRowIndexes, name, {}, randomEngine, assign_calculateWell_next(action, {}));
	},
	"concat": (rows, rowIndexes, otherRowIndexes, name, action, randomEngine) => {
		if (DEBUG) {
			console.log(`=concat: ${name}=${JSON.stringify(action)}`);
			console.log(` otherRowIndexes: ${JSON.stringify(otherRowIndexes)}`);
			console.log(` rowIndexes: ${JSON.stringify(rowIndexes)}\n ${JSON.stringify(rows)}`)
			assertNoDuplicates(otherRowIndexes);
			assertNoDuplicates(otherRowIndexes.concat([rowIndexes]));
		}

		// find groups (either from `groupBy` or just use everything in rowIndexes)
		const groupBy = _.isArray(action.groupBy)
			? action.groupBy
			: _.isEmpty(action.groupBy)
				? [] : [action.groupBy];
		const rowIndexesGroups = (!_.isEmpty(groupBy))
			? query_groupBy(rows, rowIndexes, groupBy)
			: [_.clone(rowIndexes)];

		// for each group, create a temporary row with the group variables and then expand the conditions on that row
		for (let i = 0; i < rowIndexesGroups.length; i++) {
			// create a temporary row with the group variables
			const rowIndexesGroup = rowIndexesGroups[i];
			const row0 = (_.isEmpty(groupBy)) ? {} : _.pick(rows[rowIndexesGroup[0]], groupBy);
			const rows2 = [row0];
			const rowIndexes2 = [0];
			// console.log({groupBy, rowIndexesGroup, row0})
			// expand the conditions on that row
			expandRowsByObject(rows2, rowIndexes2, [], action.design, randomEngine);
			// Add the rows back to the original table
			rows.push(...rows2);
			rowIndexes.push(..._.range(rowIndexes.length, rowIndexes.length + rows2.length));
		}
		return undefined;
	},
	"range": (rows, rowIndexes, otherRowIndexes, name, action, randomEngine) => {
		const action2 = _.cloneDeep(_.isNull(action) ? {} : action);
		_.defaults(action2, {from: 1, step: 1});
		return assign(rows, rowIndexes, otherRowIndexes, name, action2, randomEngine, undefined, assign_range_initGroup);
	},
	"rotateColumn": (rows, rowIndexes, otherRowIndexes, name, action, randomEngine) => {
		const action2 = _.isString(action) ? ({column: action, n: 1}) : _.cloneDeep(action);
		return assign(rows, rowIndexes, otherRowIndexes, name, action2, randomEngine, undefined, assign_rotateColumn_initGroup);
	},
	// "sample": {
	//
	// }
}

function assign(rows, rowIndexes, otherRowIndexes, name, action, randomEngine, next, initGroup) {
	assert(_.isPlainObject(action), "expect an object for assignment")
	// Handle order in which to assign values
	let draw = "direct";
	let reuse = "none";
	if (!_.isEmpty(action.order)) {
		switch (action.order) {
			case "direct": case "direct/none": break;
			case "direct/repeat": case "repeat": draw = "direct"; reuse = "repeat"; break;
			case "direct/reverse": case "reverse": draw = "direct"; reuse = "reverse"; break;
			case "shuffle": draw = "shuffle"; reuse = "none"; break;
			case "reshuffle": draw = "shuffle"; reuse = "reshuffle"; break;
			case "shuffle/reshuffle": draw = "shuffle"; reuse = "reshuffle"; break;
			case "shuffle/repeat": draw = "shuffle"; reuse = "repeat"; break;
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

	return handleAssignmentWithQueries(rows, rowIndexes, otherRowIndexes, name, action, randomEngine2, value2);
}

function handleAssignmentWithQueries(rows, rowIndexes0, otherRowIndexes, name, action, randomEngine, value0) {
	if (DEBUG) {
		console.log(`handleAssignmentWithQueries: ${JSON.stringify({name, action, value0})}`);
		console.log(` otherRowIndexes: ${JSON.stringify(otherRowIndexes)}`)
		console.log(` rowIndexes: ${JSON.stringify(rowIndexes0)}\n ${JSON.stringify(rows)}`)
		assertNoDuplicates(otherRowIndexes);
		assertNoDuplicates(otherRowIndexes.concat([rowIndexes0]));
	}
	const isSpecial = value0 instanceof Special;
	const hasGroupOrSame = action.groupBy || action.sameBy;

	if (!action.groupBy && !action.sameBy && !action.orderBy) {
		if (isSpecial) {
			// console.log({name})
			value0.initGroup(rows, rowIndexes0);
		}
		return value0;
	}

	const rowIndexesGroups = (action.groupBy)
		? query_groupBy(rows, rowIndexes0, action.groupBy)
		: [_.clone(rowIndexes0)];

	// console.log({rowIndexesGroups})
	for (let i = 0; i < rowIndexesGroups.length; i++) {
		let rowIndexes = _.clone(rowIndexesGroups[i]);

		let value = value0;
		// If 'orderBy' is set, we should re-order the values (if just returning 'value') or rowIndexes (otherwise)
		if (action.orderBy) {
			// console.log("A")
			if (_.isArray(value)) {
				// This is a copy of 'makeComparer', but with more indirection in assignment of row1 and row2
				const propertyNames = action.orderBy;
				function comparer(i1, i2) {
					const row1 = rows[rowIndexes[i1]];
					const row2 = rows[rowIndexes[i2]];
					const l = (_.isArray(propertyNames)) ? propertyNames : [propertyNames];
					for (let j = 0; j < l.length; j++) {
						const propertyName = l[j];
						const value1 = row1[propertyName];
						const value2 = row2[propertyName];
						const cmp = naturalSort(value1, value2);
						if (cmp !== 0)
							return cmp;
					}
					return 0;
				};
				const is1 = stableSort(_.range(rowIndexes.length), comparer);
				// console.log({is1});

				// Allocate a new value array
				const value1 = new Array(rowIndexes.length);
				// Insert values into the new array according to the new desired order
				for (let i = 0; i < rowIndexes.length; i++) {
					const j = is1[i];
					value1[j] = value[i];
				}
				// console.log({is1, rowIndexes, rows, value, value1});
				value = value1;
			}
			//if (hasGroupOrSame)
			else {
				const rowIndexes2 = query_orderBy(rows, rowIndexes, action.orderBy);
				// console.log({orderBy: action.orderBy, rowIndexes, rowIndexes2})
				// console.log({rowIndexes})
				rowIndexes = rowIndexes2;
			}
		}

		// const rowIndexes2 = _.clone(rowIndexes);

		// console.log({rowIndexes, rowIndexesGroups})
		const otherRowIndexes2 = otherRowIndexes.concat([rowIndexes0]).concat(rowIndexesGroups);
		if (DEBUG) {
			assertNoDuplicates(otherRowIndexes2);
		}
		// console.log({otherRowIndexes, rowIndexes, rowIndexesGroups, otherRowIndexes2})
		if (action.sameBy) {
			assignSameBy(rows, rowIndexes, otherRowIndexes2, name, action, randomEngine, value);
		}
		else {
			if (isSpecial) {
				// console.log({rows, rowIndexes2})
				value.initGroup(rows, rowIndexes);
			}
			// console.log({rows, rowIndexes2, otherRowIndexes2, name, value})
			expandRowsByNamedValue(rows, rowIndexes, otherRowIndexes2, name, value, randomEngine);
		}
	}

	return undefined;
}

function assignSameBy(rows, rowIndexes, otherRowIndexes, name, action, randomEngine, value) {
	if (DEBUG) {
		console.log(`assignSameBy: ${JSON.stringify({name, action, value})}`);
		console.log(` rowIndexes: ${JSON.stringify(rowIndexes)}\n ${JSON.stringify(rows)}`)
		// assertNoDuplicates(otherRowIndexes);
		assertNoDuplicates(otherRowIndexes.concat([rowIndexes]));
	}
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
		// if (isSpecial) {
		// 	value.nextIndex = i;
		// }
		const value2
			= (isArray) ? value[i]
			: (isObject) ? value[keys[i]]
			: (isSpecial) ? value.next(rows, [rowIndexes3[0]])[1]
			: value;
		// console.log({i, rowIndexes3, value2, value})
		for (let i = 0; i < rowIndexes3.length; i++) {
			const rowIndex = rowIndexes3[i];
			expandRowsByNamedValue(rows, [rowIndex], otherRowIndexes, name, value2, randomEngine);
		}
	}
}

function assign_allocatePlates_initGroup(rows, rowIndexes) {
	if (DEBUG) {
		console.log(`assign_allocatePlates_initGroup: ${rows}, ${rowIndexes}`)
	}
	const action = this.action;
	if (_.isUndefined(this.plateIndex))
		this.plateIndex = 0;
	if (_.isUndefined(this.wellsUsed))
		this.wellsUsed = 0;

	// If the wells on the plate should be segmented by some grouping
	if (action.groupBy) {
		assert(rowIndexes.length <= action.wellsPerPlate, `too many positions in group for plate to accomodate: ${rowIndexes.length} rows, ${action.wellsPerPlate} wells per plate`);

		// If they fit on the current plate:
		if (this.wellsUsed + rowIndexes.length <= action.wellsPerPlate) {
			this.wellsUsed += rowIndexes.length;
		}
		// Otherwise, skip to next plate
		else {
			this.plateIndex++;
			assert(this.plateIndex < action.plates.length, `require more plates than the ${action.plates.length} supplied: ${action.plates.join(", ")}`);
			this.wellsUsed = rowIndexes.length;
		}

		// TODO: allow for rotating plates for each group rather than assigning each plate until its full
		// console.log()
		// console.log({this})

		this.action.values = _.fill(Array(rowIndexes.length), action.plates[this.plateIndex]);
		// console.log({this_action_values: this.action.values});
	}
	else {
		// assert(rowIndexes.length <= action.plates.length * action.wellsPerPlate, `too many row for plates to accomodate: ${rowIndexes.length} rows, ${action.wellsPerPlate} wells per plate, ${action.plates.length} plates`);

		// If all the rows can be assigned to the current plate:
		if (this.wellsUsed + rowIndexes.length <= action.wellsPerPlate) {
			this.wellsUsed += rowIndexes.length;
			this.action.values = _.fill(Array(rowIndexes.length), action.plates[this.plateIndex]);
		}
		// Otherwise, just allocate each plate until its full
		else {
			this.action.values = Array(rowIndexes.length);

			let i = 0;
			while (i < rowIndexes.length) {
				const n = Math.min(rowIndexes.length - i, action.wellsPerPlate - this.wellsUsed);
				if (n == 0) {
					this.plateIndex++;
					assert(this.plateIndex < action.plates.length, `require more plates than the ${action.plates.length} supplied: ${action.plates.join(", ")}`);
					this.wellsUsed = 0;
				}
				else {
					for (let j = 0; j < n; j++) {
						this.action.values[i + j] = action.plates[this.plateIndex];
					}
					this.wellsUsed += n;
				}
				i += n;
			}
		}

		// TODO: allow for rotating plates for each group rather than assigning each plate until its full
		// console.log()
		// console.log({this})

		// console.log({this_action_values: this.action.values});
	}

	this.defaultInitGroup(rows, rowIndexes);
}

/**
 * Calculate `expr` using variables in `row`, with optional `action` object specifying `units` and/or `decimals`
 */
export function calculate(expr, row, action = {}) {
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
	// console.log({expr, scope})
	// console.log("scope:"+JSON.stringify(scope, null, '\t'))
	let value = math.eval(expr, scope);
	// console.log({type: value.type, value})
	if (_.isString(value) || _.isNumber(value) || _.isBoolean(value)) {
		return value;
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
	// console.log(`unitless: ${JSON.stringify(unitless)}`)

	// Restrict decimal places
	// console.log({unitless})
	const unitlessText = (_.isNumber(action.decimals))
		? unitless.toFixed(action.decimals)
		: _.isNumber(unitless) ? unitless : unitless.toNumber();

	// Set units
	const valueText = (!_.isEmpty(units))
		? unitlessText + " " + units
		: unitlessText;

	return valueText;
}

function assign_calculate_next(expr, action) {
	return function(nestedRows, rowIndex) {
		const row0 = nestedRows[rowIndex];
		const row = (_.isArray(row0)) ? _.head(_.flattenDeep(row0)) : row0;
		// Build the scope for evaluating the math expression from the current data row

		const valueText = calculate(expr, row, action);

		this.nextIndex++;
		return [this.nextIndex, valueText];
	}
}

function assign_calculateColumn_next(expr, action) {
	return function(nestedRows, rowIndex) {
		const row0 = nestedRows[rowIndex];
		const row = (_.isArray(row0)) ? _.head(_.flattenDeep(row0)) : row0;
		// Build the scope for evaluating the math expression from the current data row

		const valueText = _.get(row, expr, expr);
		const [r, c] = wellsParser.locationTextToRowCol(valueText);

		this.nextIndex++;
		return [this.nextIndex, c];
	}
}

function assign_calculateRow_next(expr, action) {
	return function(nestedRows, rowIndex) {
		const row0 = nestedRows[rowIndex];
		const row = (_.isArray(row0)) ? _.head(_.flattenDeep(row0)) : row0;
		// Build the scope for evaluating the math expression from the current data row

		const valueText = _.get(row, expr, expr);
		const [r, c] = wellsParser.locationTextToRowCol(valueText);

		this.nextIndex++;
		return [this.nextIndex, r];
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

function assign_rotateColumn_initGroup(rows, rowIndexes) {
	const l = rowIndexes.map(i => rows[i][this.action.column]);
	if (this.action.n > 0) {
		for (let i = 0; i < this.action.n; i++) {
			const x = l.pop();
			l.unshift(x);
		}
	}
	else {
		for (let i = 0; i < -this.action.n; i++) {
			const x = l.shift();
			l.push(x);
		}
	}
	this.action.values = l;

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

/**
 * Return an array of rowIndexes which are ordered by the `orderBy` criteria.
 * @param  {array} rows - a flat array of row objects
 * @param  {array} rowIndexes - array of row indexes to consider
 * @param  {string|array} orderBy - the column(s) to order by
 * @return {array} a sorted ordering of rowIndexes
 */
export function query_orderBy(rows, rowIndexes, orderBy) {
	// console.log({rows, rowLen: rows.length, rowIndexes, orderBy})
	// console.log(rowIndexes.map(i => _.values(_.pick(rows[i], orderBy))))
	return stableSort(rowIndexes, makeComparer(rows, orderBy));
}

function makeComparer(rows, propertyNames) {
	return function(i1, i2) {
		const row1 = rows[i1];
		const row2 = rows[i2];
		if (!row1 || !row2) {
			console.log({i1, i2, row1, row2})
		}
		const l = (_.isArray(propertyNames)) ? propertyNames : [propertyNames];
		for (let j = 0; j < l.length; j++) {
			const propertyName = l[j];
			const value1 = row1[propertyName];
			const value2 = row2[propertyName];
			const cmp = naturalSort(value1, value2);
			if (cmp !== 0)
				return cmp;
		}
		return 0;
	};
}

export function query(table, q, SCOPE = undefined) {
	let table2 = _.clone(table);

	if (q.where) {
		// console.log({where: q.where})
		table2 = filterOnWhere(table2, q.where, SCOPE);
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

	if (q.select) {
		table2 = table2.map(rows => rows.map(row => _.pick(row, q.select)));
	}

	if (q.transpose) {
		table2 = _.zip.apply(_, table2);
	}

	return table2;
}

const compareFunctions = {
	"eq": _.isEqual,
	"gt": _.gt,
	"gte": _.gte,
	"lt": _.lt,
	"lte": _.lte,
	"ne": (a, b) => !_.isEqual(a, b),
	"in": _.includes
};
function filterOnWhere(table, where, SCOPE = undefined) {
	let table2 = table;
	if (_.isPlainObject(where)) {
		_.forEach(where, (value, key) => {
			if (_.isPlainObject(value)) {
				_.forEach(value, (value2, op) => {
					// Get compare function
					assert(compareFunctions.hasOwnProperty(op), `unrecognized operator: ${op} in ${JSON.stringify(value)}`);
					const fn = compareFunctions[op];
					// console.log({op, fn})
					table2 = filterOnWhereOnce(table2, key, value2, fn);
				});
			}
			else {
				table2 = filterOnWhereOnce(table2, key, value, _.isEqual);
			}
		});
	}
	else if (_.isString(where)) {
		// console.log({where})
		table2 = _.filter(table, row => {
			const scope1 = _.mapValues(row, x => {
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
			const scope = _.defaults({}, scope1, SCOPE);
			// console.log({where, row, scope})
			try {
				const result = math.eval(where, scope);
				// console.log({result});
				return result;
			} catch (e) {
				console.log("WARNING: "+e);
				console.log("where: "+JSON.stringify(where));
				// console.log({scope})
				process.exit()
			}
			return false;
		});
	}
	return table2;
}

/**
 * Sub-function that filters table on a single criterion.
 * @param  {array}   table - table to filter
 * @param  {string}   key - key of column in table
 * @param  {any}   x - value to compare to
 * @param  {Function} fn - comparison function
 * @return {array} filtered table
 */
function filterOnWhereOnce(table, key, x, fn) {
	// console.log("filterOnWhereOnce: "); console.log({key, x, fn})
	// If x is an array, do an array comparison
	if (_.isArray(x)) {
		return _.filter(table, (row, i) => fn(row[key], x[i]));
	}
	// If we need to
	else if (_.isString(x)) {
		if (_.startsWith(x, "\"")) {
			const text = x.substr(1, x.length - 2);
			return table.filter(row => fn(row[key], text));
		}
		else {
			const key2 = x.substr(1);
			return table.filter(row => fn(row[key], row[key2]));
		}
	}
	else {
		return table.filter(row => fn(row[key], x));
	}
}

/**
 * Check whether the same underlying array shows up more than once in otherRowIndexes.
 * This should never be the case, because if we modify one, the "other" will also be modified.
 */
function assertNoDuplicates(otherRowIndexes) {
	for (let i = 0; i < otherRowIndexes.length - 1; i++) {
		for (let j = i + 1; j < otherRowIndexes.length; j++) {
			assert(otherRowIndexes[i] != otherRowIndexes[j], `same underlying array appears in 'otherRowIndexs' at both ${i} and ${j}: ${JSON.stringify(otherRowIndexes)}`)
		}
	}
}
