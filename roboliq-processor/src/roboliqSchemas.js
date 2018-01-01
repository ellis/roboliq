/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017, ETH Zurich, Ellis Whitehead
 * @license GPL-3.0
 */

// REFACTOR: it might make sense to remove this file, but it's currently used in commandHelper.js

const roboliqSchemas = {
	Number: {type: 'number'},
	Object: {type: 'object'},
	//String: {type: 'string'},
};

module.exports = roboliqSchemas;
