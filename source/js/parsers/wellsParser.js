var _ = require('lodash');
var assert = require('assert');
var random = require('random-js');
var misc = require('../misc.js');
var wellsParser0 = require('./wellsParser0.js');

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
	var result = wellsParser0.parse(text);
	if (!objects)
		return result;

	//console.log("result:\n"+JSON.stringify(result, null, '  '));
	var ll = _.map(result, function(clause) {
		//console.log("clause:\n"+JSON.stringify(clause, null, '  '));
		if (clause.labware) {
			var labware = misc.getObjectsValue(objects, clause.labware);
			assert(labware);
			assert(labware.model, "`"+clause.labware+".model` missing");
			var model = misc.getObjectsValue(objects, labware.model);
			assert(model, "`"+labware.model+"` missing");
			assert(model.rows, "`"+labware.model+".rows` missing");
			assert(model.columns, "`"+labware.model+".columns` missing");
			var l = [];
			if (clause.subject === 'all') {
				for (row = 1; row <= model.rows; row++) {
					for (col = 1; col <= model.columns; col++) {
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
										throw {name: "RangeError", message: "`"+text+"` extends beyond range of labware `"+clause.labware+"`"};
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
										throw {name: "RangeError", message: "`"+text+"` extends beyond range of labware `"+clause.labware+"`"};
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
										throw {name: "RangeError", message: "`"+text+"` extends beyond range of labware `"+clause.labware+"`"};
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
										throw {name: "RangeError", message: "`"+text+"` extends beyond range of labware `"+clause.labware+"`"};
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
						default:
							assert(false, "unhandled verb: "+phrase[0]);
					}
				});
			}
			// Convert the list of row/col back to text
			return _.map(l, function(rc) {
				var location = locationRowColToText(rc[0], rc[1]);
				return clause.labware+'('+location+')';
			});
		}
		else if (clause.source) {
			var source = misc.getObjectsValue(objects, clause.source);
			assert(!_.isEmpty(source.wells), "`"+clause.source+".wells` missing");
			return [source.wells];
		}
		else {
			assert(false);
		}
	});
	//console.log("ll:")
	//console.log(ll);
	return _.flatten(ll);
}

module.exports = {
	parse: parse
}
