var _ = require('lodash');
var math = require('mathjs');
var commandHelper = require('../commandHelper.js');
var expect = require('../expect.js');

function makeEvowareFacts(params, data, variable, value) {
	var parsed = commandHelper.parseParams(params, data, {
		agent: "name",
		equipment: "name"
	});
	var carrier = commandHelper.getParsedValue(parsed, data, "equipment", "evowareId");
	var result2 = {
		command: "evoware._facts",
		agent: parsed.agent.objectName,
		factsEquipment: carrier,
		factsVariable: carrier+"_"+variable
	};
	var value2 = value;
	if (_.isFunction(value))
		value2 = value(parsed, data);
	return _.merge(result2, {factsValue: value2});
}

module.exports = {
	roboliq: "v1",
	imports: ["roboliq.js"],
	objects: {
		"ourlab": {
			"type": "Namespace",
			"mario": {
				"type": "Namespace",
				"centrifuge": {
					"type": "Centrifuge",
					"sitesInternal": ["ourlab.mario.site.CENTRIFUGE_1", "ourlab.mario.site.CENTRIFUGE_2", "ourlab.mario.site.CENTRIFUGE_3", "ourlab.mario.site.CENTRIFUGE_4"],
					"evowareId": "Centrifuge"
				},
				"evoware": {
					"type": "EvowareRobot"
				},
				"reader": {
					"type": "Reader",
					"sitesInternal": ["ourlab.mario.site.READER"],
					"evowareId": "ReaderNETwork"
				},
				"roma1": {
					"type": "Transporter",
					"evowareRoma": 0
				},
				"roma2": {
					"type": "Transporter",
					"evowareRoma": 1
				},
				"liha": {
					"type": "Pipetter",
					"syringes": {
						"1": {
							"type": "Syringe",
							"tipModel": "tipModel1000",
							"tipModelPermanent": "tipModel1000"
						},
						"2": {
							"type": "Syringe",
							"tipModel": "tipModel1000",
							"tipModelPermanent": "tipModel1000"
						},
						"3": {
							"type": "Syringe",
							"tipModel": "tipModel1000",
							"tipModelPermanent": "tipModel1000"
						},
						"4": {
							"type": "Syringe",
							"tipModel": "tipModel1000",
							"tipModelPermanent": "tipModel1000"
						}
					}
				},
				"sealer": {
					"type": "Sealer",
					"evowareId": "RoboSeal"
				},
				"site": {
					"type": "Namespace",
					"CENTRIFUGE_1": { "type": "Site", "evowareCarrier": "Centrifuge", "evowareGrid": 54, "evowareSite": 1, "closed": true },
					"CENTRIFUGE_2": { "type": "Site", "evowareCarrier": "Centrifuge", "evowareGrid": 54, "evowareSite": 2, "closed": true },
					"CENTRIFUGE_3": { "type": "Site", "evowareCarrier": "Centrifuge", "evowareGrid": 54, "evowareSite": 1, "closed": true },
					"CENTRIFUGE_4": { "type": "Site", "evowareCarrier": "Centrifuge", "evowareGrid": 54, "evowareSite": 2, "closed": true },
					"P2": {
						"type": "Site",
						"evowareCarrier": "MP 2Pos H+P Shake",
						"evowareGrid": 10,
						"evowareSite": 2
					},
					"P3": {
						"type": "Site",
						"evowareCarrier": "MP 2Pos H+P Shake",
						"evowareGrid": 10,
						"evowareSite": 4
					},
					P4: { type: "Site", evowareCarrier: "MP 3Pos Cooled 1 PCR", evowareGrid: 17, evowareSite: 2 },
					P5: { type: "Site", evowareCarrier: "MP 3Pos Cooled 1 PCR", evowareGrid: 17, evowareSite: 4 },
					P6: { type: "Site", evowareCarrier: "MP 3Pos Cooled 2 PCR", evowareGrid: 17, evowareSite: 2 },
					P8: { type: "Site", evowareCarrier: "MP 3Pos Cooled 2 PCR", evowareGrid: 17, evowareSite: 4 },
					P7: { type: "Site", evowareCarrier: "MP 3Pos Cooled 2 PCR", evowareGrid: 17, evowareSite: 6 },
					R1: { type: "Site", evowareCarrier: "LI - Trough 3Pos 100ml", evowareGrid: 3, evowareSite: 1 },
					R2: { type: "Site", evowareCarrier: "LI - Trough 3Pos 100ml", evowareGrid: 3, evowareSite: 2 },
					R3: { type: "Site", evowareCarrier: "LI - Trough 3Pos 100ml", evowareGrid: 3, evowareSite: 3 },
					R4: { type: "Site", evowareCarrier: "LI - Trough 3Pos 100mlOffset", evowareGrid: 8, evowareSite: 1 },
					R5: { type: "Site", evowareCarrier: "LI - Trough 3Pos 100mlOffset", evowareGrid: 8, evowareSite: 2 },
					R6: { type: "Site", evowareCarrier: "LI - Trough 3Pos 100mlOffset", evowareGrid: 8, evowareSite: 3 },
					READER: { type: "Site", evowareCarrier: "Infinite M200", evowareGrid: 61, evowareSite: 1, closed: true },
					"REGRIP": {
						"type": "Site",
						"evowareCarrier": "ReGrip Station",
						"evowareGrid": 62,
						"evowareSite": 1
					},
					"ROBOSEAL": {
						"type": "Site",
						"evowareCarrier": "RoboSeal",
						"evowareGrid": 35,
						"evowareSite": 1
					},
					"SYSTEM": {
						"type": "Site",
						"evowareCarrier": "System",
						"evowareGrid": -1,
						"evowareSite": 0
					},
					T3: { type: "Site", evowareCarrier: "Block 20Pos", evowareGrid: 16, evowareSite: 1 },
				},
				"systemLiquidLabwareModel": {
					"description": "dummy labware model representing the system liquid source",
					"rows": 8,
					"columns": 1,
					"evowareName": "SystemLiquid"
				},
				"systemLiquid": {
					"type": "Liquid",
					"wells": [
						"ourlab.mario.systemLiquidLabware(A01)",
						"ourlab.mario.systemLiquidLabware(B01)",
						"ourlab.mario.systemLiquidLabware(C01)",
						"ourlab.mario.systemLiquidLabware(D01)"
					]
				},
				"systemLiquidLabware": {
					"description": "dummy labware representing the system liquid source",
					"model": "ourlab.mario.systemLiquidLabwareModel",
					"location": "ourlab.mario.site.SYSTEM",
					"contents": ["Infinity l", "systemLiquid"]
				},
				"timer1": {
					"type": "Timer",
					"evowareId": 1
				},
				"washProgram": {
					"type": "Namespace",
					"flush_1000": {
						"type": "EvowareWashProgram",
						"wasteGrid": 1,
						"wasteSite": 2,
						"cleanerGrid": 1,
						"cleanerSite": 1,
						"wasteVolume": 1,
						"wasteDelay": 500,
						"cleanerVolume": 1,
						"cleanerDelay": 500,
						"airgapVolume": 10,
						"airgapSpeed": 70,
						"retractSpeed": 30,
						"fastWash": false
					},
					"flush_0050": {
						"type": "EvowareWashProgram",
						"wasteGrid": 1,
						"wasteSite": 2,
						"cleanerGrid": 1,
						"cleanerSite": 1,
						"wasteVolume": 0.05,
						"wasteDelay": 500,
						"cleanerVolume": 0.05,
						"cleanerDelay": 500,
						"airgapVolume": 10,
						"airgapSpeed": 70,
						"retractSpeed": 30,
						"fastWash": false
					},
					"light_1000": {
						"type": "EvowareWashProgram",
						"wasteGrid": 1,
						"wasteSite": 2,
						"cleanerGrid": 1,
						"cleanerSite": 1,
						"wasteVolume": 4,
						"wasteDelay": 500,
						"cleanerVolume": 2,
						"cleanerDelay": 500,
						"airgapVolume": 10,
						"airgapSpeed": 70,
						"retractSpeed": 30,
						"fastWash": false
					},
					"thorough_1000": {
						"type": "EvowareWashProgram",
						"script": "C:\\ProgramData\\TECAN\\EVOware\\database\\scripts\\Roboliq\\Roboliq_Clean_Thorough_1000.esc"
					},
					"thorough_0050": {
						"type": "EvowareWashProgram",
						"script": "C:\\ProgramData\\TECAN\\EVOware\\database\\scripts\\Roboliq\\Roboliq_Clean_Thorough_0050.esc"
					}
				}
			},
			"model": {
				"type": "Namespace",
				"plateModel_96_pcr": {
					"label": "96 well PCR plate",
					"rows": 8,
					"columns": 12,
					"evowareName": "D-BSSE 96 Well PCR Plate"
				},
				"plateModel_96_dwp": {
					"label": "96 well deep-well plate",
					"rows": 8,
					"columns": 12,
					"evowareName": "D-BSSE 96 Well DWP"
				},
				"plateModel_384_round": {
					"label": "384 round-well plate",
					"rows": 16,
					"columns": 24,
					"evowareName": "D-BSSE 384 Well Plate White"
				},
				"plateModel_384_square": {
					"label": "384 square-flat-well white plate",
					"rows": 16,
					"columns": 24,
					"evowareName": "D-BSSE 384 Well Plate White"
				},
				"plateModel_384_square_transparent_greiner": {
					"label": "384 square-flat-well transparent Greiner",
					"rows": 16,
					"columns": 24,
					"evowareName": "384 Sqr Flat Trans Greiner"
				},
				"plateModel_96_square_transparent_nunc": {
					"label": "96 square-well transparent Nunc plate",
					"rows": 8,
					"columns": 12,
					"evowareName": "Ellis Nunc F96 MicroWell"
				},
				"troughModel_100ml": {
					"label": "Trough 100ml",
					"rows": 8,
					"columns": 1,
					"evowareName": "Trough 100ml"
				},
				"troughModel_100ml_lowvol_tips": {
					"label": "Trough 100ml LowVol Tips",
					"rows": 8,
					"columns": 1,
					"evowareName": "Trough 100ml LowVol Tips"
				},
				"tubeHolderModel_1500ul": {
					"label": "20 tube block 1.5ml",
					"rows": 4,
					"columns": 5,
					"evowareName": "Block 20Pos 1.5 ml Eppendorf"
				}
			}
		}
	},

	objectToPredicateConverters: {
		"EvowareRobot": function(name) {
			return {
				value: [{
					"isAgent": {
						"agent": name
					}
				}]
			};
		}
	},

	predicates: _.flatten([{
		"isSiteModel": {
			"model": "ourlab.siteModel1"
		}
	}, {
		"siteModel": {
			"site": "ourlab.mario.site.CENTRIFUGE_1",
			"siteModel": "ourlab.siteModel1"
		}
	}, {
		"siteModel": {
			"site": "ourlab.mario.site.CENTRIFUGE_2",
			"siteModel": "ourlab.siteModel1"
		}
	}, {
		"siteModel": {
			"site": "ourlab.mario.site.CENTRIFUGE_3",
			"siteModel": "ourlab.siteModel1"
		}
	}, {
		"siteModel": {
			"site": "ourlab.mario.site.CENTRIFUGE_4",
			"siteModel": "ourlab.siteModel1"
		}
	},
	{"#for": {
		factors: {site: ["ourlab.mario.site.P2", "ourlab.mario.site.P3", "ourlab.mario.site.P4", "ourlab.mario.site.P5"]},
		output: {
			"siteModel": {
				"site": "{{site}}",
				"siteModel": "ourlab.siteModel1"
			}
		}
	}},
	{
		"siteModel": {
			"site": "ourlab.mario.site.READER",
			"siteModel": "ourlab.siteModel1"
		}
	}, {
		"siteModel": {
			"site": "ourlab.mario.site.REGRIP",
			"siteModel": "ourlab.siteModel1"
		}
	}, {
		"siteModel": {
			"site": "ourlab.mario.site.ROBOSEAL",
			"siteModel": "ourlab.siteModel1"
		}
	}, {
		"stackable": {
			"below": "ourlab.siteModel1",
			"above": "ourlab.model.plateModel_96_square_transparent_nunc"
		}
	}, {
		"stackable": {
			"below": "ourlab.siteModel1",
			"above": "ourlab.model.plateModel_384_square"
		}
	},
	_.map(["ourlab.model.plateModel_384_square"], function(model) {
		return {"centrifuge.canAgentEquipmentModelSite1Site2": {
			"agent": "ourlab.mario.evoware",
			"equipment": "ourlab.mario.centrifuge",
			"model": model,
			"site1": "ourlab.mario.site.CENTRIFUGE_2",
			"site2": "ourlab.mario.site.CENTRIFUGE_4"
		}};
	}),
	// ROMA1 Narrow
	_.map(["P1", "P2", "P3", "P4", "P5", "REGRIP"], function(s) {
		return {"siteCliqueSite": {"siteClique": "ourlab.mario.siteClique1", "site": "ourlab.mario.site."+s}};
	}),
	{
		"transporter.canAgentEquipmentProgramSites": {
			"agent": "ourlab.mario.evoware",
			"equipment": "ourlab.mario.roma1",
			"program": "Narrow",
			"siteClique": "ourlab.mario.siteClique1"
		}
	},
	// ROMA2 Narrow
	_.map(["P1", "P2", "P3", "P4", "P5", "ROBOSEAL", "REGRIP"], function(s) {
		return {"siteCliqueSite": {"siteClique": "ourlab.mario.siteClique2", "site": "ourlab.mario.site."+s}};
	}),
	{
		"transporter.canAgentEquipmentProgramSites": {
			"agent": "ourlab.mario.evoware",
			"equipment": "ourlab.mario.roma2",
			"program": "Narrow",
			"siteClique": "ourlab.mario.siteClique2"
		}
	},
	// Centrifuge ROMA1 Narrow
	_(["CENTRIFUGE_1", "CENTRIFUGE_2", "CENTRIFUGE_3", "CENTRIFUGE_4"]).map(function(s) {
		return [
			{"siteCliqueSite": {"siteClique": "ourlab.mario.siteClique_"+s, "site": "ourlab.mario.site.REGRIP"}},
			{"siteCliqueSite": {"siteClique": "ourlab.mario.siteClique_"+s, "site": "ourlab.mario.site."+s}},
			{
				"transporter.canAgentEquipmentProgramSites": {
					"agent": "ourlab.mario.evoware",
					"equipment": "ourlab.mario.roma1",
					"program": "Narrow",
					"siteClique": "ourlab.mario.siteClique_"+s
				}
			}
		];
	}).flatten().value(),
	// READER ROMA2 Wide
	_(["READER"]).map(function(s) {
		return [
			{"siteCliqueSite": {"siteClique": "ourlab.mario.siteClique_"+s, "site": "ourlab.mario.site.REGRIP"}},
			{"siteCliqueSite": {"siteClique": "ourlab.mario.siteClique_"+s, "site": "ourlab.mario.site."+s}},
			{
				"transporter.canAgentEquipmentProgramSites": {
					"agent": "ourlab.mario.evoware",
					"equipment": "ourlab.mario.roma2",
					"program": "Wide",
					"siteClique": "ourlab.mario.siteClique_"+s
				}
			}
		];
	}).flatten().value(),
	{"#for": {
		factors: {model: ["plateModel_384_square"]},
		output: {
			"fluorescenceReader.canAgentEquipmentModelSite": {
				"agent": "ourlab.mario.evoware",
				"equipment": "ourlab.mario.reader",
				"model": "ourlab.model.{{model}}",
				"site": "ourlab.mario.site.READER"
			}
		}
	}},
	{
		"sealer.canAgentEquipmentProgramModelSite": {
			"agent": "ourlab.mario.evoware",
			"equipment": "ourlab.mario.sealer",
			"program": "C:\\HJBioanalytikGmbH\\RoboSeal3\\RoboSeal_PlateParameters\\PerkinElmer_weiss.bcf",
			"model": "ourlab.model.plateModel_96_square_transparent_nunc",
			"site": "ourlab.mario.site.ROBOSEAL"
		}
	},
	{
		"sealer.canAgentEquipmentProgramModelSite": {
			"agent": "ourlab.mario.evoware",
			"equipment": "ourlab.mario.sealer",
			"program": "C:\\HJBioanalytikGmbH\\RoboSeal3\\RoboSeal_PlateParameters\\Greiner_384_schwarz.bcf",
			"model": "ourlab.model.plateModel_384_square",
			"site": "ourlab.mario.site.ROBOSEAL"
		}
	},
	{"#for": {
		factors: {site: ["P2", "P3", "P4", "P5", "R1", "R2", "R3", "R4", "R5", "R6", "SYSTEM", "T1", "T2", "T3"]},
		output: {
			"pipetter.canAgentEquipmentSite": {
				"agent": "ourlab.mario.evoware",
				"equipment": "ourlab.mario.liha",
				"site": "ourlab.mario.site.{{site}}"
			}
		}
	}},
	{
		"pipetter.canAgentEquipmentSite": {
			"agent": "ourlab.mario.evoware",
			"equipment": "ourlab.mario.liha",
			"site": "ourlab.mario.site.P3"
		}
	}, {
		"pipetter.canAgentEquipmentSite": {
			"agent": "ourlab.mario.evoware",
			"equipment": "ourlab.mario.liha",
			"site": "ourlab.mario.site.SYSTEM"
		}
	}, {
		"pipetter.cleanTips.canAgentEquipmentProgramModelIntensity": {
			"agent": "ourlab.mario.evoware",
			"equipment": "ourlab.mario.liha",
			"program": "ourlab.mario.washProgram.flush_1000",
			"model": "tipModel1000",
			"intensity": "flush"
		}
	}, {
		"pipetter.cleanTips.canAgentEquipmentProgramModelIntensity": {
			"agent": "ourlab.mario.evoware",
			"equipment": "ourlab.mario.liha",
			"program": "ourlab.mario.washProgram.light_1000",
			"model": "tipModel1000",
			"intensity": "light"
		}
	}, {
		"pipetter.cleanTips.canAgentEquipmentProgramModelIntensity": {
			"agent": "ourlab.mario.evoware",
			"equipment": "ourlab.mario.liha",
			"program": "ourlab.mario.washProgram.thorough_1000",
			"model": "tipModel1000",
			"intensity": "thorough"
		}
	},
	{"timer.canAgentEquipment": {
		"agent": "ourlab.mario.evoware",
		"equipment": "ourlab.mario.timer1",
	}},
	_.map([1,2,3,4], function(n) {
		return {"method": {"description": "generic.closeSite-CENTRIFUGE_"+n,
			"task": {"generic.closeSite": {"site": "?site"}},
			"preconditions": [
				{"same": {"thing1": "?site", "thing2": "ourlab.mario.site.CENTRIFUGE_"+n}}
			],
			"subtasks": {"ordered": [
				{"ourlab.mario.centrifuge.close": {"agent": "ourlab.mario.evoware", "equipment": "ourlab.mario.centrifuge"}}
			]}
		}}
	}),
	{"action": {"description": "ourlab.mario.centrifuge.close: close the centrifuge",
		"task": {"ourlab.mario.centrifuge.close": {"agent": "?agent", "equipment": "?equipment"}},
		"preconditions": [],
		"deletions": [],
		"additions": [
			{"siteIsClosed": {"site": "ourlab.mario.site.CENTRIFUGE_1"}},
			{"siteIsClosed": {"site": "ourlab.mario.site.CENTRIFUGE_2"}},
			{"siteIsClosed": {"site": "ourlab.mario.site.CENTRIFUGE_3"}},
			{"siteIsClosed": {"site": "ourlab.mario.site.CENTRIFUGE_4"}}
		]
	}},
	_.map([1,2,3,4], function(n) {
		return {"method": {"description": "generic.openSite-CENTRIFUGE_"+n,
			"task": {"generic.openSite": {"site": "?site"}},
			"preconditions": [{"same": {"thing1": "?site", "thing2": "ourlab.mario.site.CENTRIFUGE_"+n}}],
			"subtasks": {"ordered": [_.zipObject([["ourlab.mario.centrifuge.open"+n, {}]])]}
		}};
	}),
	_.map([1,2,3,4], function(n) {
		return {"action": {"description": "ourlab.mario.centrifuge.open: open an internal site on the centrifuge",
			"task": _.zipObject([["ourlab.mario.centrifuge.open"+n, {}]]),
			"preconditions": [],
			"deletions": [
				{"siteIsClosed": {"site": "ourlab.mario.site.CENTRIFUGE_"+n}}
			],
			"additions": _.map(_.without([1,2,3,4], n), function(n2) {
				return {"siteIsClosed": {"site": "ourlab.mario.site.CENTRIFUGE_"+n2}};
			})
		}};
	}),

	// Open READER
	{"method": {"description": "generic.openSite-READER",
		"task": {"generic.openSite": {"site": "?site"}},
		"preconditions": [{"same": {"thing1": "?site", "thing2": "ourlab.mario.site.READER"}}],
		"subtasks": {"ordered": [{"ourlab.mario.reader.open": {}}]}
	}},
	{"action": {"description": "ourlab.mario.reader.open: open the reader",
		"task": {"ourlab.mario.reader.open": {}},
		"preconditions": [],
		"deletions": [{"siteIsClosed": {"site": "ourlab.mario.site.READER"}}],
		"additions": []
	}},
	// Close READER
	{"method": {"description": "generic.closeSite-READER",
		"task": {"generic.closeSite": {"site": "?site"}},
		"preconditions": [{"same": {"thing1": "?site", "thing2": "ourlab.mario.site.READER"}}],
		"subtasks": {"ordered": [{"ourlab.mario.reader.close": {}}]}
	}},
	{"action": {"description": "ourlab.mario.reader.close: close the reader",
		"task": {"ourlab.mario.reader.close": {}},
		"preconditions": [],
		"deletions": [],
		"additions": [
			{"siteIsClosed": {"site": "ourlab.mario.site.READER"}}
		]
	}},

	]),

	commandSpecs: {
		"equipment.openSite|ourlab.mario.evoware|ourlab.mario.centrifuge": {
			properties: {
				agent: {description: "Agent identifier", type: "Agent"},
				equipment: {description: "Equipment identifier", type: "Equipment"},
				site: {description: "Site identifier", type: "Site"}
			},
			required: ["agent", "equipment", "site"]
		},
		"equipment.run|ourlab.mario.evoware|ourlab.mario.centrifuge": {
			properties: {
				program: {description: "Program for centrifuging"}
			},
			required: ["program"]
		},
		"equipment.run|ourlab.mario.evoware|ourlab.mario.sealer": {
			properties: {
				agent: {description: "Agent identifier", type: "Agent"},
				equipment: {description: "Equipment identifier", type: "Equipment"},
				program: {description: "Program identifier for sealing", type: "string"}
			},
			required: ["agent", "equipment", "program"]
		},
		"equipment.openSite|ourlab.mario.evoware|ourlab.mario.reader": {
			properties: {
				agent: {description: "Agent identifier", type: "Agent"},
				equipment: {description: "Equipment identifier", type: "Equipment"},
				site: {description: "Site identifier", type: "Site"}
			},
			required: ["agent", "equipment", "site"]
		},
		"equipment.run|ourlab.mario.evoware|ourlab.mario.reader": {
			description: "Run our Infinit M200 reader using either programFile or programData",
			properties: {
				agent: {description: "Agent identifier", type: "Agent"},
				equipment: {description: "Equipment identifier", type: "Equipment"},
				programFile: {description: "Program filename", type: "File"},
				programData: {description: "Program data"},
				outputFile: {description: "Filename for measured output", type: "string"}
			},
			required: ["outputFile"]
		},
	},

	commandHandlers: {
		"equipment.close|ourlab.mario.evoware|ourlab.mario.centrifuge": function(params, parsed, data) {
			return {expansion: [makeEvowareFacts(params, data, "Close")]};
		},
		"equipment.open|ourlab.mario.evoware|ourlab.mario.centrifuge": function(params, parsed, data) {
			return {expansion: [makeEvowareFacts(params, data, "Open")]};
		},
		"equipment.openSite|ourlab.mario.evoware|ourlab.mario.centrifuge": function(params, parsed, data) {
			var carrier = commandHelper.getParsedValue(parsed, data, "equipment", "evowareId");
			var sitesInternal = commandHelper.getParsedValue(parsed, data, "equipment", "sitesInternal");
			var siteIndex = sitesInternal.indexOf(parsed.site.objectName);
			expect.truthy({paramName: "site"}, siteIndex >= 0, "site must be one of the equipments internal sites: "+sitesInternal.join(", "));
			return {
				expansion: [
					{
						command: "evoware._facts",
						agent: parsed.agent.objectName,
						factsEquipment: carrier,
						factsVariable: carrier+"_MoveToPos",
						factsValue: (siteIndex+1).toString()
					},
					{
						command: "evoware._facts",
						agent: parsed.agent.objectName,
						factsEquipment: carrier,
						factsVariable: carrier+"_Open"
					},
				]
			};
		},
		"equipment.run|ourlab.mario.evoware|ourlab.mario.centrifuge": function(params, parsed, data) {
			//console.log({parsed, params})
			var parsedProgram = commandHelper.parseParams2(parsed.program.value, data, {
				properties: {
					rpm: {type: "number", default: 3000},
					duration: {type: "Duration", default: math.unit(30, 's')},
					spinUpTime: {type: "Duration", default: math.unit(9, 's')},
					spinDownTime: {type: "Duration", default: math.unit(9, 's')},
					temperature: {type: "number", default: 25}
				}
			});
			//console.log(parsedProgram);
			var list = [
				math.round(parsedProgram.rpm.value),
				math.round(parsedProgram.duration.value.toNumber('s')),
				math.round(parsedProgram.spinUpTime.value.toNumber('s')),
				math.round(parsedProgram.spinDownTime.value.toNumber('s')),
				math.round(parsedProgram.temperature.value)
			];
			var value = list.join(",");
			return {expansion: [makeEvowareFacts(params, data, "Execute1", value)]};
		},
		// Reader
		"equipment.close|ourlab.mario.evoware|ourlab.mario.reader": function(params, parsed, data) {
			return {expansion: [makeEvowareFacts(params, data, "Close")]};
		},
		"equipment.open|ourlab.mario.evoware|ourlab.mario.reader": function(params, parsed, data) {
			return {expansion: [makeEvowareFacts(params, data, "Open")]};
		},
		"equipment.openSite|ourlab.mario.evoware|ourlab.mario.reader": function(params, parsed, data) {
			var carrier = commandHelper.getParsedValue(parsed, data, "equipment", "evowareId");
			var sitesInternal = commandHelper.getParsedValue(parsed, data, "equipment", "sitesInternal");
			var siteIndex = sitesInternal.indexOf(parsed.site.objectName);
			expect.truthy({paramName: "site"}, siteIndex >= 0, "site must be one of the equipments internal sites: "+sitesInternal.join(", "));

			return {expansion: [makeEvowareFacts(params, data, "Open")]};
		},
		"equipment.run|ourlab.mario.evoware|ourlab.mario.reader": function(params, parsed, data) {
			var hasProgramFile = (parsed.programFile.value) ? 1 : 0;
			var hasProgramData = (parsed.programData.value) ? 1 : 0;
			//console.log(parsed);
			expect.truthy({}, hasProgramFile + hasProgramData >= 1, "either `programFile` or `programData` must be specified.");
			expect.truthy({}, hasProgramFile + hasProgramData <= 1, "only one of `programFile` or `programData` may be specified.");
			const content = (hasProgramData)
				? parsed.programData.value.toString('utf8')
				: parsed.programFile.value.toString('utf8');
			var start_i = content.indexOf("<TecanFile");
			if (start_i < 0)
				start_i = 0;
			var programData = content.substring(start_i).
				replace(/[\r\n]/g, "").
				replace(/&/g, "&amp;"). // "&amp;" is probably not needed, since I didn't find it in the XML files
				replace(/=/g, "&equal;").
				replace(/"/g, "&quote;").
				replace(/~/, "&tilde;").
				replace(/>[ \t]+</g, "><");
			// Token
			var value = parsed.outputFile.value + "|" + programData;
			return {expansion: [makeEvowareFacts(params, data, "Measure", value)]};
		},
		// Sealer
		"equipment.run|ourlab.mario.evoware|ourlab.mario.sealer": function(params, parsed, data) {
			var parsed = commandHelper.parseParams(params, data, {
				agent: "name",
				equipment: "name",
				program: "name"
			});
			var carrier = commandHelper.getParsedValue(parsed, data, "equipment", "evowareId");
			return {
				expansion: [{
					command: "evoware._facts",
					agent: parsed.agent.objectName,
					factsEquipment: carrier,
					factsVariable: carrier+"_Seal",
					factsValue: parsed.program.objectName
				}],
				//effects: _.zipObject([[params.object + ".sealed", true]])
			};
		},
		"evoware._facts": function() {}
	},

	planHandlers: {
		"ourlab.mario.centrifuge.close": function(params, parentParams, data) {
			return [{
				command: "equipment.close",
				agent: "ourlab.mario.evoware",
				equipment: "ourlab.mario.centrifuge"
			}];
		},
		"ourlab.mario.centrifuge.open1": function(params, parentParams, data) {
			return [{
				command: "equipment.openSite",
				agent: "ourlab.mario.evoware",
				equipment: "ourlab.mario.centrifuge",
				site: "ourlab.mario.site.CENTRIFUGE_1"
			}];
		},
		"ourlab.mario.centrifuge.open2": function(params, parentParams, data) {
			return [{
				command: "equipment.openSite",
				agent: "ourlab.mario.evoware",
				equipment: "ourlab.mario.centrifuge",
				site: "ourlab.mario.site.CENTRIFUGE_2"
			}];
		},
		"ourlab.mario.centrifuge.open3": function(params, parentParams, data) {
			return [{
				command: "equipment.openSite",
				agent: "ourlab.mario.evoware",
				equipment: "ourlab.mario.centrifuge",
				site: "ourlab.mario.site.CENTRIFUGE_3"
			}];
		},
		"ourlab.mario.centrifuge.open4": function(params, parentParams, data) {
			return [{
				command: "equipment.openSite",
				agent: "ourlab.mario.evoware",
				equipment: "ourlab.mario.centrifuge",
				site: "ourlab.mario.site.CENTRIFUGE_4"
			}];
		},
		"ourlab.mario.reader.close": function(params, parentParams, data) {
			return [{
				command: "equipment.close",
				agent: "ourlab.mario.evoware",
				equipment: "ourlab.mario.reader"
			}];
		},
		"ourlab.mario.reader.open": function(params, parentParams, data) {
			return [{
				command: "equipment.openSite",
				agent: "ourlab.mario.evoware",
				equipment: "ourlab.mario.reader",
				site: "ourlab.mario.site.READER"
			}];
		},
	}
}
