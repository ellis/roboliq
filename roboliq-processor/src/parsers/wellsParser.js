/**
 * Module for parsing strings that represent wells and labware.
 * @module
 */

const _ = require('lodash');
const assert = require('assert');
const random = require('random-js');
const expect = require('../expectCore.js');
const misc = require('../misc.js');
const wellsParser0 = require('./wellsParser0.js');

/**
 * Take a well identifier (e.g. A01) and returns an integer array
 * representing the row and column of that well.
 * @param  {string} location - a well identifier starting with a capital letter and followed by a number (e.g. A01)
 * @return {array} an integer array of `[row, col]`.  The values are 1-based (i.e. row 1 is the first row)
 * @static
 */
function locationTextToRowCol(location) {
	var row = location.charCodeAt(0) - "A".charCodeAt(0) + 1;
	var col = parseInt(location.substr(1));
	return [row, col];
}

/**
 * Converts a row and column index to a string.
 * For example, `[1, 1] => A01`, and `[8, 12] => H12`.
 * @param  {number} row - row of well
 * @param  {number} col - column of well
 * @return {string} string representation of location of well on labware
 * @static
 */
function locationRowColToText(row, col) {
	var colText = col.toString();
	if (colText.length == 1) colText = "0"+colText;
	return String.fromCharCode("A".charCodeAt(0) + row - 1) + colText;
}

/**
 * Parses a text which should represent one or more labwares and wells.
 * If the `objects` parameter is passed, this function will return an array of the individual wells;
 * otherwise it will return the raw parser results.
 *
 * @param {string} text - text to parse
 * @param {object} objects - map of protocol objects, in order to find labwares and number of rows and columns on labware.
 * @param {object} [config] - optional object that contains properties for 'rows' and 'columns', in case we want to expand something like 'A1 down C3' without having specified a plate
 * @return {array} If the `objects` parameter is passed, this function will return an array of the individual wells; otherwise it will return the raw parser results.
 * @static
 */
function parse(text, objects, config) {
	assert(_.isString(text), "wellsParser.parse() expected a string, received: "+text)
	var result;
	try {
		result = wellsParser0.parse(text);
	} catch (e) {
		expect.rethrow(e);
	}
	if (!objects)
		return result;

	return processParserResult(result, objects, text, config);
}

/**
 * Take the raw parser results and return an array of location names,
 * one entry for each well.
 * @param {array} result - raw parser results.
 * @param {object} objects - map of protocol objects, in order to find labwares and number of rows and columns on labware.
 * @param {string} text - the original text that was parsed; this is merely used for error output.
 * @param {object} [config] - optional object that contains properties for 'rows' and 'columns', in case we want to expand something like 'A1 down C3' without having specified a plate
 * @return {array} array of names for each plate + well (e.g. `plate1(C04)`)
 */
function processParserResult(result, objects, text, config = {}) {
	const commandHelper = require('../commandHelper.js');
	//console.log("text", text)
	//console.log("result", result)
	//console.log("result:\n"+JSON.stringify(result, null, '  '));
	var ll = _.map(result, function(clause) {
		const data = {objects};
		// Get source or labware objects
		//console.log({commandHelper})
		const parsed = commandHelper.parseParams(clause, data, {
			properties: {
				source: {},
				labware: {},
				//subject: 'Any?'
			}
		});
		// console.log({clause, parsed});
		if (parsed.value.source) {
			// If this was a source, return its wells
			if (parsed.value.source.type === 'Source' || parsed.value.source.type === 'Liquid')
				return [parsed.value.source.wells];
			// Handle the case of when a variable is used and here we can substitute in its value
			else if (_.isString(parsed.value.source) && parsed.value.source !== clause.source)
				return parse(parsed.value.source, objects);
			// Else
			else {
				// console.log({clause, parsed})
				expect.throw({}, "unrecognized source specifier: "+JSON.stringify(parsed.value.source));
			}
		}
		else if (parsed.value.labware || (config.rows && config.columns)) {
			// Get number of rows and columns
			let rows, columns;
			let labwareName;
			if (parsed.value.labware) {
				const labware = parsed.value.labware;
				labwareName = parsed.objectName.labware;
				var modelName = labware.model;
				assert(modelName, "`"+labwareName+".model` missing");
				var model = misc.getObjectsValue(modelName, objects);
				assert(model.rows, "`"+modelName+".rows` missing");
				assert(model.columns, "`"+modelName+".columns` missing");
				rows = model.rows;
				columns = model.columns;
			}
			else {
				rows = config.rows;
				columns = config.columns;
			}

			var l = [];
			if (clause.subject === 'all') {
				for (var col = 1; col <= columns; col++) {
					for (var row = 1; row <= rows; row++) {
						l.push([row, col]);
					}
				}
			}
			else {
				l.push(locationTextToRowCol(clause.subject));
			}
			if (clause.phrases) {
				_.forEach(clause.phrases, function(phrase) {
					switch (phrase[0]) {
						case "down":
							assert(l.length == 1, "`down` can only be used with a single well");
							var rc0 = l[0];
							var n = phrase[1];
							assert(n >= 1, "`down n` must be positive: "+text);
							var row = rc0[0];
							var col = rc0[1];
							for (var i = 1; i < n; i++) {
								row++;
								if (row > rows) {
									row = 1;
									col++;
									if (col > columns) {
										throw {name: "RangeError", message: "`"+text+"` extends beyond range of labware `"+labwareName+"`"};
									}
								}
								l.push([row, col])
							}
							break;
						case "down-to":
							assert(l.length == 1, "`down` can only be used with a single well");
							var rc0 = l[0];
							var rc1 = locationTextToRowCol(phrase[1]);
							assert(rc0[1] < rc1[1] || rc0[0] <= rc1[0], "invalid target for `down`: "+text)
							assert(rc0[1] <= rc1[1], "column of `"+phrase[1]+"` must be equal or greater than origin: "+text);
							var row = rc0[0];
							var col = rc0[1];
							while (row !== rc1[0] || col != rc1[1]) {
								row++;
								if (row > rows) {
									row = 1;
									col++;
									if (col > columns) {
										throw {name: "RangeError", message: "`"+text+"` extends beyond range of labware `"+labwareName+"`"};
									}
								}
								l.push([row, col])
							}
							break;
						case "down-block":
							assert(l.length == 1, "`block` can only be used with a single well");
							var rc0 = l[0];
							var rc1 = locationTextToRowCol(phrase[1]);
							assert(rc0[0] <= rc1[0], "row of `"+phrase[1]+"` must be equal or greater than origin: "+text);
							assert(rc0[1] <= rc1[1], "column of `"+phrase[1]+"` must be equal or greater than origin: "+text);
							var row = rc0[0];
							var col = rc0[1];
							while (row !== rc1[0] || col != rc1[1]) {
								row++;
								if (row > rc1[0]) {
									row = rc0[0];
									col++;
								}
								l.push([row, col])
							}
							break;
						case "right":
							assert(l.length == 1, "`right` can only be used with a single well");
							var rc0 = l[0];
							var n = phrase[1];
							assert(n >= 1, "`right n` must be positive: "+text);
							var row = rc0[0];
							var col = rc0[1];
							for (var i = 1; i < n; i++) {
								col++;
								if (col > columns) {
									row++;
									col = 1;
									if (row > rows) {
										throw {name: "RangeError", message: "`"+text+"` extends beyond range of labware `"+labwareName+"`"};
									}
								}
								l.push([row, col])
							}
							break;
						case "right-to":
							assert(l.length == 1, "`right` can only be used with a single well");
							var rc0 = l[0];
							var rc1 = locationTextToRowCol(phrase[1]);
							assert(rc0[0] < rc1[0] || rc0[1] <= rc1[1], "invalid target for `right`: "+text)
							assert(rc0[0] <= rc1[0], "row of `"+phrase[1]+"` must be equal or greater than origin: "+text);
							var row = rc0[0];
							var col = rc0[1];
							while (row !== rc1[0] || col != rc1[1]) {
								col++;
								if (col > columns) {
									col = 1;
									row++;
									if (row > rows) {
										throw {name: "RangeError", message: "`"+text+"` extends beyond range of labware `"+labwareName+"`"};
									}
								}
								l.push([row, col])
							}
							break;
						case "right-block":
							assert(l.length == 1, "`block` can only be used with a single well");
							var rc0 = l[0];
							var rc1 = locationTextToRowCol(phrase[1]);
							assert(rc0[0] <= rc1[0], "row of `"+phrase[1]+"` must be equal or greater than origin: "+text);
							assert(rc0[1] <= rc1[1], "column of `"+phrase[1]+"` must be equal or greater than origin: "+text);
							var row = rc0[0];
							var col = rc0[1];
							while (row !== rc1[0] || col != rc1[1]) {
								col++;
								if (col > rc1[1]) {
									col = rc0[1];
									row++;
								}
								l.push([row, col])
							}
							break;
						case "random":
							// Initialize randomizing engine
							var mt = random.engines.mt19937();
							if (phrase.length == 2) {
								mt.seed(phrase[1]);
							}
							else {
								mt.autoSeed();
							}
							// Randomize the list
							var rest = _.clone(l);
							random.shuffle(mt, rest);
							//console.log("rest:", rest);
							// Now try to not repeated pick the sames rows or columns
							var l2 = [];
							var choices = [];
							while (rest.length > 0) {
								if (choices.length == 0)
									choices = _.clone(rest);
								//console.log("choices:", JSON.stringify(choices));
								// Pick the first choice in list
								var rc = _.pullAt(choices, 0)[0];
								// Add it to our new list
								l2.push(rc);
								// Remove it from 'rest'
								rest = _.without(rest, rc);
								// Remove all items from choices with the same row or column
								_.remove(choices, function(rc2) {
									return (rc2[0] == rc[0] || rc2[1] == rc[1]);
								});
							}
							l = l2;
							break;
						case "take":
							var n = phrase[1];
							assert(n >= 0);
							l = _.take(l, n);
							break;
						case "row-jump":
							// Number of rows of space to leave between rows
							var n = phrase[1];
							expect.truthy(null, n >= 0, "row-jump value must be >= 0");
							var cycleLen = n + 1;

							var l2 = [];
							while (l.length > 0) {
								// Get consecutive rc's that are in the same col
								var col = l[0][1];
								var sameCol = _.takeWhile(l, function(rc) { return rc[1] == col; });
								l = _.drop(l, sameCol.length);

								//console.dir(sameCol);
								while (sameCol.length > 0) {
									var row = sameCol[0][0];
									var l3 = _.remove(sameCol, function(rc) {
										return (((rc[0] - row) % cycleLen) === 0);
									});
									//console.log(row, l3, sameCol);
									l2 = l2.concat(l3);
								}
							}
							l = l2;
							break;
						default:
							assert(false, "unhandled verb: "+phrase[0]);
					}
				});
			}
			// Convert the list of row/col back to text
			return _.map(l, function(rc) {
				var location = locationRowColToText(rc[0], rc[1]);
				return (labwareName) ? labwareName+'('+location+')' : location;
			});
		}
		else if (clause.subject) {
			assert(_.isEmpty(clause.phrases));
			return clause.subject;
		}
		else {
			assert(false);
		}
	});
	//console.log("ll:")
	//console.log(ll);
	return _.flatten(ll);
}

/**
 * Parses a string which should represent a single well, and return the raw
 * parser results.
 *
 * @param {string} text - text to parse
 * @return {object} an object representing the raw parser results.
 * @static
 */
function parseOne(text) {
	assert(_.isString(text), "wellsParser.parseOne() expected a string, received: "+JSON.stringify(text))
	try {
		return wellsParser0.parse(text, {startRule: 'startOne'});
	} catch (e) {
		throw e;
		//throw Error(e.toString());
	}

}

module.exports = {
	locationRowColToText,
	locationTextToRowCol,
	parse,
	parseOne,
	processParserResult,
};
