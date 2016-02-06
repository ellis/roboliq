/**
 * Roboliq's default configuration.
 * @module config/roboliq
 */

var _ = require('lodash');
import yaml from 'yamljs';

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

	// clear: a site is clear if no labware is on it
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

	// open: a site is open if it's not closed (FUTURE: and if it's not locked)
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
	"Plate": function(name, object) {
		var value = [{
			"isLabware": {
				"labware": name
			}
		}, {
			"isPlate": {
				"labware": name
			}
		}, {
			"model": {
				"labware": name,
				"model": object.model
			}
		}, {
			"location": {
				"labware": name,
				"site": object.location
			}
		}];
		if (object.sealed)
			value.push({
				"plateIsSealed": {
					"labware": name
				}
			});
		return {
			value: value
		};
	},
	"PlateModel": function(name, object) {
		return {
			value: [{
				"isModel": {
					"model": name
				}
			}]
		};
	},
	"Site": function(name, object) {
		return {
			value: _.compact([
				{"isSite": {"model": name}},
				(object.closed) ? {"siteIsClosed": {"site": name}} : null,
			])
		};
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
		'../commands/experiment.js',
		'../commands/system.js',
		// Equipment
		'../commands/equipment.js',
		'../commands/centrifuge.js',
		'../commands/fluorescenceReader.js',
		'../commands/pipetter.js',
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
