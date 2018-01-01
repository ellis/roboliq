/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017, ETH Zurich, Ellis Whitehead
 * @license GPL-3.0
 */

const isArray = require('lodash/isArray');
const isPlainObject = require('lodash/isPlainObject');

/**
 * Recursively remove any properties that are undefined.
 * @param  {object} obj - objects to remove undefined properties from
 */
function stripUndefined(obj) {
	for (let key in obj) {
		const x = obj[key]
		if (x === undefined) {
			delete obj[key];
		}
		else if (isArray(x) || isPlainObject(x)) {
			stripUndefined(x);
		}
	}
	return obj;
}

module.exports = stripUndefined;
