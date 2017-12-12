/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017 Ellis Whitehead
 * @license GPL-3.0
 */

/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017 Ellis Whitehead
 * @license GPL-3.0
 */

import isArray from 'lodash/isArray';
import isPlainObject from 'lodash/isPlainObject';

/**
 * Recursively remove any properties that are undefined.
 * @param  {object} obj - objects to remove undefined properties from
 */
export default function stripUndefined(obj) {
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
