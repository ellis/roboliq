'use strict';

const _clone = require('lodash/clone');
const _forEach = require('lodash/forEach');
const _isEmpty = require('lodash/isEmpty');
const _isEqual = require('lodash/isEqual');

// HACK: copied from design2.js
function getCommonValues(table) {
	if (_isEmpty(table)) return {};

	let common = _clone(table[0]);
	for (let i = 1; i < table.length; i++) {
		// Remove any value from common which aren't shared with this row.
		_forEach(table[i], (value, name) => {
			if (common.hasOwnProperty(name) && !_isEqual(common[name], value)) {
				delete common[name];
			}
		});
	}

	return common;
}

module.exports = {
	getCommonValues
};
