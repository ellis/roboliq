/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017, ETH Zurich, Ellis Whitehead
 * @license GPL-3.0
 */

/**
 * Roboliq's default configuration.
 * @module config/roboliq
 */

const _ = require('lodash');
const yaml = require('yamljs');

var predicates = [
	//
	// Rules
	//

	// same: Two things are the same if they unify.
	{
		"<--": {
			"same": {
				"thing1": "?thing",
				"thing2": "?thing"
			}
		}
	},

	// labwareHasNoLid: a labware has no lid if no labware is on top of it
	{
		"<--": {
			"labwareHasNoLid": {
				"labware": "?labware"
			},
			"and": [{
				"not": {
					"location": {
						"labware": "?lid",
						"site": "?labware"
					}
				}
			}]
		}
	},

	// siteIsClear: a site is clear if no labware is on it
	{
		"<--": {
			"siteIsClear": {
				"site": "?site"
			},
			"and": [{
				"not": {
					"location": {
						"labware": "?labware",
						"site": "?site"
					}
				}
			}]
		}
	},

	// siteIsOpen: a site is open if it's not closed (FUTURE: and if it's not locked)
	{
		"<--": {
			"siteIsOpen": {
				"site": "?site"
			},
			"and": [{
				"not": {
					"siteIsClosed": {
						"site": "?site"
					}
				}
			}]
		}
	},

	// member of list (see HTN-orig/lists.js)
	{"<--": {"member": {"target": "?target", "list": {"cons": {"first": "?target",
								   "rest": "?restOfList"}}}}},
	{"<--": {"member": {"target": "?target", "list": {"cons": {"first": "?firstOfList",
								   "rest": "?restOfList"}}},
		 "and": [{"member": {"target": "?target", "list": "?restOfList"}}]}},

	{"method": {"description": "generic.openSite-null: null-operation for opening an already open site",
		"task": {"generic.openSite": {"site": "?site"}},
		"preconditions": [
			{"siteIsOpen": {"site": "?site"}}
		],
		"subtasks": {"ordered": [
			//{"print": {"text": "generic.openSite-null"}}
		]}
	}},
];

var objectToPredicateConverters = {
	Lid: function(name, object) {
		return [
			{"isLabware": {"labware": name}},
			{"isLid": {"labware": name}},
			{"model": {"labware": name, "model": object.model}},
			{"location": {"labware": name, "site": object.location}}
		];
	},
	LidModel: function(name, object) {
		return [{ "isModel": { "model": name } }];
	},
	"Plate": function(name, object) {
		var value = [
			{ "isLabware": { "labware": name } },
			{ "isPlate": { "labware": name } },
			{ "model": { "labware": name, "model": object.model } },
			{ "location": { "labware": name, "site": object.location } }
		];
		if (object.sealed) {
			value.push({ "plateIsSealed": { "labware": name } });
		}
		return value;
	},
	"PlateModel": function(name, object) {
		return [{ "isModel": { "model": name } }];
	},
	"Site": function(name, object) {
		return _.compact([
			{"isSite": {"model": name}},
			(object.closed) ? {"siteIsClosed": {"site": name}} : null,
		]);
	},
};

var planHandlers = {
	"trace": function() {
		return [];
	}
};

module.exports = {
	roboliq: "v1",
	imports: [
		'../commands/data.js',
		'../commands/system.js',
		'../commands/abstract.js',
		// Equipment
		'../commands/equipment.js',
		'../commands/absorbanceReader.js',
		'../commands/centrifuge.js',
		'../commands/fluorescenceReader.js',
		'../commands/incubator.js',
		'../commands/pipetter.js',
		'../commands/scale.js',
		'../commands/sealer.js',
		'../commands/shaker.js',
		'../commands/timer.js',
		'../commands/transporter.js',
	],
	predicates: predicates,
	objectToPredicateConverters: objectToPredicateConverters,
	schemas: yaml.load(__dirname+'/../schemas/roboliq.yaml'),
	planHandlers: planHandlers,
	directiveHandlers: require('./roboliqDirectiveHandlers.js'),
};
