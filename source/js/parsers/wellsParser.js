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
			var l = [];
			if (clause.subject === 'all') {
				assert(labware.model, "in order to evaluate `all` in `"+text+"`, please set `"+clause.labware+".model`");
				var model = misc.getObjectsValue(objects, labware.model);
				assert(model, "in order to evaluate `all` in `"+text+"`, please set `"+labware.model+"`");
				assert(model.rows, "`"+labware.model+".rows` missing");
				assert(model.columns, "`"+labware.model+".columns` missing");
				for (row = 1; row <= models.rows; row++) {
					for (col = 1; col <= models.columns; col++) {
						l.push([row, col]);
					}
				}
				return l;
			}
			else {
				l.push(locationTextToRowCol(clause.subject));
			}
			if (clause.phrases) {
				_.forEach(clause.phrases, function(phrase) {
					switch (phrase[0]) {
						case "down":
							var l2 = [];
							_.forEach(l, function(rc) {
								for (var d = 0; d < phrase[1]; d++) {
									l2.push([rc[0] + d, rc[1]]);
								}
							});
							l = _.uniq(l2);
							break;
						// TODO: case "down-to":
						case "right":
							var l2 = [];
							_.forEach(l, function(rc) {
								for (var d = 0; d < phrase[1]; d++) {
									l2.push([rc[0], rc[1] + d]);
								}
							});
							l = _.uniq(l2);
							break;
						// TODO: case "right-to":
						// TODO: case "block-to":
						case "random":
							// Initialize randomizing engine
							var mt = random.engines.mt19937();
							if (phrase.length == 2) {
								mt.seed(phrase[1]);
							}
							else {
								mt.autoseed();
							}
							// Randomize the list
							var rest = _.clone(l);
							random.shufle(mt, rest);
							// Now try to not pick rows and columns too close to each other
							var l2 = [];
							var choices = [];
							while (rest.length > 0) {
								if (choices.length == 0)
									choices = _.clone(rest);
								// Pick the first choice in list
								var rc = _.pullAt(choices, 0)[0];
								// Add it to our new list
								l2.push(rc);
								// Remove it from 'rest'
								rest = _.without(rest, rc);
								// Remove all items from choices with the same row or column
								_.remove(choices, function(rc2) {
									rc2[0] == rc[0] || rc2[1] == rc[1];
								});
							}
							l = l2;
							break;
						case "take":
							l = _.take(l, phrase[1]);
							break;
						default:
							assert(false);
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
			return [source.wells, "poop"];
		}
		else {
			assert(false);
		}
	});
	console.log("ll:")
	console.log(ll);
	return _.flatten(ll);
}

module.exports = {
	parse: parse
}
