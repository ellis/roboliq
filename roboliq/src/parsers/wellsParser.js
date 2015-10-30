var _ = require('lodash');
var assert = require('assert');
var random = require('random-js');
var expect = require('../expectCore.js');
var misc = require('../misc.js');
var wellsParser0 = require('./wellsParser0.js');
//import commandHelper from '../commandHelper.js';

function locationTextToRowCol(location) {
	var row = location.charCodeAt(0) - "A".charCodeAt(0) + 1;
	var col = parseInt(location.substr(1));
	return [row, col];
}

function locationRowColToText(row, col) {
	var colText = col.toString();
	if (colText.length == 1) colText = "0"+colText;
	return String.fromCharCode("A".charCodeAt(0) + row - 1) + colText;
}

function parse(text, objects) {
	assert(_.isString(text), "wellsParser.parse() expected a string, received: "+text)
	var result;
	try {
		result = wellsParser0.parse(text);
	} catch (e) {
		throw Error(e.toString());
	}
	if (!objects)
		return result;

	return processParserResult(result, objects, text);
}

function processParserResult(result, objects, text) {
	var commandHelper = require('../commandHelper.js');
	//console.log("text", text)
	//console.log("result", result)
	//console.log("result:\n"+JSON.stringify(result, null, '  '));
	var ll = _.map(result, function(clause) {
		const data = {objects};
		// Get source or labware objects
		//console.log({commandHelper})
		const parsed = commandHelper.parseParams(clause, data, {
			source: 'Any?',
			labware: 'Any?',
			//subject: 'Any?'
		});
		//console.log(parsed);
		if (parsed.source.value) {
			if (parsed.source.value.type === 'Source')
				return [parsed.source.value.wells];
			else if (_.isString(parsed.source.value) && parsed.source.value !== clause.source)
				return parse(parsed.source.value, objects);
		}
		else if (parsed.labware.value) {
			const labware = parsed.labware.value;
			const labwareName = parsed.labware.objectName;
			var modelName = labware.model;
			assert(modelName, "`"+labwareName+".model` missing");
			var model = misc.getObjectsValue(modelName, objects);
			assert(model.rows, "`"+modelName+".rows` missing");
			assert(model.columns, "`"+modelName+".columns` missing");
			var l = [];
			if (clause.subject === 'all') {
				for (var col = 1; col <= model.columns; col++) {
					for (var row = 1; row <= model.rows; row++) {
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
								if (row > model.rows) {
									row = 1;
									col++;
									if (col > model.columns) {
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
								if (row > model.rows) {
									row = 1;
									col++;
									if (col > model.columns) {
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
								if (col > model.columns) {
									row++;
									col = 1;
									if (row > model.rows) {
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
								if (col > model.columns) {
									col = 1;
									row++;
									if (row > model.rows) {
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
							expect.truthy(null, n >= 0, "row-spacing value must be >= 0");
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
				return labwareName+'('+location+')';
			});
		}
		else {
			assert(false);
		}
	});
	//console.log("ll:")
	//console.log(ll);
	return _.flatten(ll);
}

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
	parse: parse,
	parseOne: parseOne,
	processParserResult: processParserResult
}
