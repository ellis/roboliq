import _ from 'lodash';
import assert from 'assert';
import Immutable, {Map, fromJS} from 'immutable';
import math from 'mathjs';
import Random from 'random-js';
//import yaml from 'yamljs';

import {locationRowColToText} from './parsers/wellsParser.js';


/**
 * expandRowsByConditions:
 *   for each key/value pair, call expandRowsByNamedValue
 *
 * // REQUIRED by: branchRowsByNamedValue
 * expandRowsByNamedValue:
 *   TODO: turn the name/value into an action in order to allow for more sophisticated expansion
 *   if has star-suffix, call branchRowsByNamedValue
 *   else call assignRowsByNamedValue
 *
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
 *
 * // REQUIRED by: assignRowsByNamedValue
 * assignRowByNamedKeyItem:
 *   setColumnValue(row, name, key)
 *   if item is array:
 *     branchRowByArray(nestdRows, rowIndex, item)
 *   else if item is object:
 *     setColumnValue(row, name, key)
 *     expandRowsByValue(nestedRows, [rowIndex], value)
 *   else:
 *     setColumnValue(row, name, item)
 *
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


/**
 * expandRowsByConditions:
 *   for each key/value pair, call expandRowsByNamedValue
 */
function expandRowsByConditions(rows, rowIndexes, conditions) {
	for (let name in conditions) {
		expandRowsByNamedValue(rows, rowIndexes, name, conditions[name]);
	}
}

/**
 * // REQUIRED by: branchRowsByNamedValue
 * expandRowsByNamedValue:
 *   TODO: turn the name/value into an action in order to allow for more sophisticated expansion
 *   if has star-suffix, call branchRowsByNamedValue
 *   else call assignRowsByNamedValue
 */
function expandRowsByNamedValue(rows, rowIndexes, name, value) {
	// TODO: turn the name/value into an action in order to allow for more sophisticated expansion
	if (_.endsWith(name, "*")) {

	}
	else {
		CONTINUE
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
 *
 * // REQUIRED by: assignRowsByNamedValue
 * assignRowByNamedKeyItem:
 *   setColumnValue(row, name, key)
 *   if item is array:
 *     branchRowByArray(nestdRows, rowIndex, item)
 *   else if item is object:
 *     setColumnValue(row, name, key)
 *     expandRowsByValue(nestedRows, [rowIndex], value)
 *   else:
 *     setColumnValue(row, name, item)
 *
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


/*
a*: [a, b] # branching
b: [a, b] # assignment
c: hello # assignment
d: [[{e: A, f: L}, {e: B, f: R}], [{e: A, f: R}, {e: B, f: L}]] # Assignment then branching
e: [[1, 2], [3, 4]] # ERROR

then figure out actions for sampling and whatnot
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
