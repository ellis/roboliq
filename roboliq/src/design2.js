import _ from 'lodash';
import assert from 'assert';
// import Immutable, {Map, fromJS} from 'immutable';
// import math from 'mathjs';
// import Random from 'random-js';
//import yaml from 'yamljs';

// import {locationRowColToText} from './parsers/wellsParser.js';


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
 */
export function expandConditions(conditions) {
	const table = [{}];
	expandRowsByObject(table, [0], conditions);
	flattenArrayM(table);
	return table;
}

/**
 * expandRowsByObject:
 *   for each key/value pair, call expandRowsByNamedValue
 */
function expandRowsByObject(nestedRows, rowIndexes, conditions) {
	// console.log("expandRowsByObject: "+JSON.stringify(conditions));
	for (let name in conditions) {
		expandRowsByNamedValue(nestedRows, rowIndexes, name, conditions[name]);
	}
}

/**
 * // REQUIRED by: branchRowsByNamedValue
 * expandRowsByNamedValue:
 *   TODO: turn the name/value into an action in order to allow for more sophisticated expansion
 *   if has star-suffix, call branchRowsByNamedValue
 *   else call assignRowsByNamedValue
 */
function expandRowsByNamedValue(nestedRows, rowIndexes, name, value) {
	// console.log(`expandRowsByNamedValue: ${name}, ${JSON.stringify(value)}`);
	// TODO: turn the name/value into an action in order to allow for more sophisticated expansion
	if (_.endsWith(name, "*")) {
		branchRowsByNamedValue(nestedRows, rowIndexes, name, value);
	}
	else {
		assignRowsByNamedValue(nestedRows, rowIndexes, name, value);
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
function assignRowsByNamedValue(nestedRows, rowIndexes, name, value) {
	// console.log(`assignRowsByNamedValue: ${name}, ${JSON.stringify(value)}`);
	if (_.isArray(value)) {
		assert(rowIndexes.length <= value.length, "fewer values than rows: "+JSON.stringify({name, value, rowIndexes}));
		for (let i = 0; i < rowIndexes.length; i++) {
			const rowIndex = rowIndexes[i];
			assignRowByNamedKeyItem(nestedRows, rowIndex, name, i + 1, value[i]);
		}
	}
	else if (_.isObject(value)) {
		const keys = _.keys(value);
		assert(rowIndexes.length <= keys.length, "fewer values than rows: "+JSON.stringify({name, keys, rowIndexes}));
		for (let i = 0; i < rowIndexes.length; i++) {
			const key = keys[i];
			const rowIndex = rowIndexes[i];
			assignRowByNamedKeyItem(nestedRows, rowIndex, name, key, value[key]);
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
 * assignRowByNamedKeyItem:
 *   if item is array:
 *     setColumnValue(row, name, key)
 *     branchRowByArray(nestdRows, rowIndex, item)
 *   else if item is object:
 *     setColumnValue(row, name, key)
 *     expandRowsByConditions(nestedRows, [rowIndex], item)
 *   else:
 *     setColumnValue(row, name, item)
 */
function assignRowByNamedKeyItem(nestedRows, rowIndex, name, key, item) {
	if (_.isArray(item)) {
		setColumnValue(nestedRows[rowIndex], name, key);
		branchRowByArray(nestedRows, rowIndex, item);
	}
	else if (_.isObject(item)) {
		setColumnValue(nestedRows[rowIndex], name, key);
		expandRowsByConditions(nestedRows, [rowIndex], item);
	}
	else {
		setColumnValue(nestedRows[rowIndex], name, item);
	}
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
function branchRowsByNamedValue(nestedRows, rowIndexes, name, value) {
	const size
		= (_.isArray(value)) ? value.length
		: (_.isPlainObject(value)) ? _.size(value)
		: 1;
	for (let i = 0; i < rowIndexes.length; i++) {
		const rowIndex = rowIndexes[i];
		// Make replicates of row
		const row0 = nestedRows[rowIndex];
		const rows2 = Array(size);
		for (let rowIndex2 = 0; rowIndex2 < size; rowIndex2++) {
			rows2[rowIndex2] = _.cloneDeep(row0);
		}

		expandRowsByNamedValue(rows2, _.range(size), name, value);
		nestedRows[rowIndex] = _.flattenDeep(rows2);
	}
}

function branchRowByArray(nestedRows, rowIndex, array) {
	const size = array.length;
	// Make replicates of row
	const row0 = nestedRows[rowIndex];
	const rows2 = Array(size);
	for (let rowIndex2 = 0; rowIndex2 < size; rowIndex2++) {
		rows2[rowIndex2] = _.cloneDeep(row0);
	}

	expandRowsByConditions(rows2, _.range(size), name, value);
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
