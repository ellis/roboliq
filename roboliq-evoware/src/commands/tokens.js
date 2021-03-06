/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017, ETH Zurich, Ellis Whitehead
 * @license GPL-3.0
 */

/**
 * Create a Mix token.
 * @param {object} params - {syringeMask, program, volumes
 * @param {string} params.syringeMask
 * @param {string} params.program
 * @param {array} params.volumes - 12 numbers representing mix volumes in ul for each tip
 * @param {integer} params.evowareGrid
 * @param {integer} params.evowareSite - (site index starts at 1 rather than 0)
 * @param {integer} [params.syringeSpacing] - optional spacing between syringes (starts at 1)
 * @param {string} params.plateMask
 * @param {integer} params.count - number of times to mix
 * @class
 */

const evowareHelper = require('./evowareHelper.js')

class Mix {
	constructor(params) {
		// console.log(params)
		this.params = params;
	}

	toLine() {
		const params = this.params;
		const l = [
			params.syringeMask,
			`"${evowareHelper.stripQuotes(params.program)}"`,
			params.volumes.join(","),
			params.evowareGrid, params.evowareSite - 1,
			params.syringeSpacing || 1,
			`"${params.plateMask}"`,
			params.count,
			0,
			0
		];
		return `Mix(${l.join(",")});`;
	}
}

module.exports = {
  Mix,
};
