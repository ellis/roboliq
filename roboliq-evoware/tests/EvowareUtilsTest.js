/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017, ETH Zurich, Ellis Whitehead
 * @license GPL-3.0
 */

const _ = require('lodash');
const should = require('should');
const EvowareUtils = require('../src/EvowareUtils.js');

describe("EvowareUtils", function() {
	describe("parseEncodedIndexes", function() {
		it("should work", function() {
			should.deepEqual(
				EvowareUtils.parseEncodedIndexes("01001000000000000000000000000000000000000"),
				[1, 0, [0]]
			);
			should.deepEqual(
				EvowareUtils.parseEncodedIndexes("0100¯000000000000000000000000000000000000"),
				[1, 0, [0, 1, 2, 3, 4, 5, 6]]
			);
		});
		it.skip("should figure out this code that I don't understand yet", function() {
			should.deepEqual(
				EvowareUtils.parseEncodedIndexes("0100ï¿½ï¿½30000000000000000000000000000000000"),
				[1, 0, [0]]
			);
		});
	});
});
