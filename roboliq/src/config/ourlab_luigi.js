var _ = require('lodash');
var math = require('mathjs');
var commandHelper = require('../commandHelper.js');
var expect = require('../expect.js');

function makeEvowareFacts(parsed, data, variable, value) {
	const carrier = commandHelper.getParsedValue(parsed, data, "equipment", "evowareId");
	const result2 = {
		command: "evoware._facts",
		agent: parsed.objectName.agent,
		factsEquipment: carrier,
		factsVariable: carrier+"_"+variable
	};
	const value2 = (_.isFunction(value))
		? value(parsed, data)
		: value;
	return _.merge(result2, {factsValue: value2});
}

module.exports = {
	roboliq: "v1",
	imports: ["roboliq.js"],
	objects: {
		"ourlab": {
			"type": "Namespace",
			"luigi": {
				"type": "Namespace",
				"evoware": {
					"type": "EvowareRobot"
				},
				"reader": {
					"type": "Reader",
					"sitesInternal": ["ourlab.luigi.site.READER"],
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
					"syringe": {
						"1": {
							"type": "Syringe",
							"tipModel": "ourlab.luigi.tipModel2500",
							"tipModelPermanent": "ourlab.luigi.tipModel2500"
						},
						"2": {
							"type": "Syringe",
							"tipModel": "ourlab.luigi.tipModel2500",
							"tipModelPermanent": "ourlab.luigi.tipModel2500"
						},
						"3": {
							"type": "Syringe",
							"tipModel": "ourlab.luigi.tipModel2500",
							"tipModelPermanent": "ourlab.luigi.tipModel2500"
						},
						"4": {
							"type": "Syringe",
							"tipModel": "ourlab.luigi.tipModel2500",
							"tipModelPermanent": "ourlab.luigi.tipModel2500"
						},
						"5": {
							"type": "Syringe",
							"tipModel": "ourlab.luigi.tipModel2500",
							"tipModelPermanent": "ourlab.luigi.tipModel2500"
						},
						"6": {
							"type": "Syringe",
							"tipModel": "ourlab.luigi.tipModel2500",
							"tipModelPermanent": "ourlab.luigi.tipModel2500"
						},
						"7": {
							"type": "Syringe",
							"tipModel": "ourlab.luigi.tipModel2500",
							"tipModelPermanent": "ourlab.luigi.tipModel2500"
						},
						"8": {
							"type": "Syringe",
							"tipModel": "ourlab.luigi.tipModel2500",
							"tipModelPermanent": "ourlab.luigi.tipModel2500"
						}
					},
					"tipModelToSyringes": {
						"ourlab.luigi.tipModel2500": ["ourlab.luigi.liha.syringe.1", "ourlab.luigi.liha.syringe.2", "ourlab.luigi.liha.syringe.3", "ourlab.luigi.liha.syringe.4", "ourlab.luigi.liha.syringe.5", "ourlab.luigi.liha.syringe.6", "ourlab.luigi.liha.syringe.7", "ourlab.luigi.liha.syringe.8"]
					}
				},
				"sealer": {
					"type": "Sealer",
					"evowareId": "RoboSeal"
				},
				"site": {
					"type": "Namespace",
					P1: { type: "Site", evowareCarrier: "MP 2Pos Flat with Downholder", evowareGrid: 13, evowareSite: 1 },
					P2: { type: "Site", evowareCarrier: "MP 2Pos Flat with Downholder", evowareGrid: 13, evowareSite: 2 },
					P3: { type: "Site", evowareCarrier: "Downholder DWB", evowareGrid: 12, evowareSite: 1 },
					P4: { type: "Site", evowareCarrier: "MP 3Pos Flat Grid19", evowareGrid: 19, evowareSite: 1 },
					P5: { type: "Site", evowareCarrier: "MP 3Pos Flat Grid19", evowareGrid: 19, evowareSite: 2 },
					P6: { type: "Site", evowareCarrier: "MP 3Pos Flat Grid19", evowareGrid: 19, evowareSite: 3 },
					R1: { type: "Site", evowareCarrier: "Trough 1500ml StainlessSteel", evowareGrid: 37, evowareSite: 1 },
					R2: { type: "Site", evowareCarrier: "Trough 3Pos 25+100ml", evowareGrid: 40, evowareSite: 1 },
					R3: { type: "Site", evowareCarrier: "Trough 3Pos 25+100ml", evowareGrid: 40, evowareSite: 2 },
					R4: { type: "Site", evowareCarrier: "Trough 3Pos 25+100ml", evowareGrid: 40, evowareSite: 3 },
					"ROBOSEAL": {
						"type": "Site",
						"evowareCarrier": "RoboSeal",
						"evowareGrid": 45,
						"evowareSite": 1
					},
					"SYSTEM": {
						"type": "Site",
						"evowareCarrier": "System",
						"evowareGrid": -1,
						"evowareSite": 0
					},
				},
				"systemLiquidLabwareModel": {
					"type": "PlateModel",
					"description": "dummy labware model representing the system liquid source",
					"rows": 8,
					"columns": 1,
					"evowareName": "SystemLiquid"
				},
				"systemLiquid": {
					"type": "Liquid",
					"wells": [
						"ourlab.luigi.systemLiquidLabware(A01)",
						"ourlab.luigi.systemLiquidLabware(B01)",
						"ourlab.luigi.systemLiquidLabware(C01)",
						"ourlab.luigi.systemLiquidLabware(D01)",
						"ourlab.luigi.systemLiquidLabware(E01)",
						"ourlab.luigi.systemLiquidLabware(F01)",
						"ourlab.luigi.systemLiquidLabware(G01)",
						"ourlab.luigi.systemLiquidLabware(H01)"
					]
				},
				"systemLiquidLabware": {
					"type": "Plate",
					"description": "dummy labware representing the system liquid source",
					"model": "ourlab.luigi.systemLiquidLabwareModel",
					"location": "ourlab.luigi.site.SYSTEM",
					"contents": ["Infinity l", "systemLiquid"]
				},
				"timer1": {
					"type": "Timer",
					"evowareId": 1
				},
				"washProgram": {
					CONTINUE
					"type": "Namespace",
					"flush_2500": { IS THIS REALLY FLUSH?
						"type": "EvowareWashProgram",
						"wasteGrid": 42,
						"wasteSite": 2,
						"cleanerGrid": 42,
						"cleanerSite": 3,
						"wasteVolume": 6,
						"wasteDelay": 500,
						"cleanerVolume": 8,
						"cleanerDelay": 500,
						"airgapVolume": 10,
						"airgapSpeed": 70,
						"retractSpeed": 30,
						"fastWash": true
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
					"type": "PlateModel",
					"label": "96 well PCR plate",
					"rows": 8,
					"columns": 12,
					"evowareName": "D-BSSE 96 Well PCR Plate"
				},
				"plateModel_96_dwp": {
					"type": "PlateModel",
					"label": "96 well deep-well plate",
					"rows": 8,
					"columns": 12,
					"evowareName": "D-BSSE 96 Well DWP"
				},
				"plateModel_384_round": {
					"type": "PlateModel",
					"label": "384 round-well plate",
					"rows": 16,
					"columns": 24,
					"evowareName": "D-BSSE 384 Well Plate White"
				},
				"plateModel_384_square": {
					"type": "PlateModel",
					"label": "384 square-flat-well white plate",
					"rows": 16,
					"columns": 24,
					"evowareName": "D-BSSE 384 Well Plate White"
				},
				"plateModel_384_square_transparent_greiner": {
					"type": "PlateModel",
					"label": "384 square-flat-well transparent Greiner",
					"rows": 16,
					"columns": 24,
					"evowareName": "384 Sqr Flat Trans Greiner"
				},
				"plateModel_96_square_transparent_nunc": {
					"type": "PlateModel",
					"label": "96 square-well transparent Nunc plate",
					"rows": 8,
					"columns": 12,
					"evowareName": "Ellis Nunc F96 MicroWell"
				},
				"troughModel_100ml": {
					"type": "PlateModel",
					"label": "Trough 100ml",
					"rows": 8,
					"columns": 1,
					"evowareName": "Trough 100ml"
				},
				"troughModel_100ml_lowvol_tips": {
					"type": "PlateModel",
					"label": "Trough 100ml LowVol Tips",
					"rows": 8,
					"columns": 1,
					"evowareName": "Trough 100ml LowVol Tips"
				},
				"tubeHolderModel_1500ul": {
					"type": "PlateModel",
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
			"site": "ourlab.luigi.site.CENTRIFUGE_1",
			"siteModel": "ourlab.siteModel1"
		}
	}, {
		"siteModel": {
			"site": "ourlab.luigi.site.CENTRIFUGE_2",
			"siteModel": "ourlab.siteModel1"
		}
	}, {
		"siteModel": {
			"site": "ourlab.luigi.site.CENTRIFUGE_3",
			"siteModel": "ourlab.siteModel1"
		}
	}, {
		"siteModel": {
			"site": "ourlab.luigi.site.CENTRIFUGE_4",
			"siteModel": "ourlab.siteModel1"
		}
	},
	{"#for": {
		factors: {site: ["ourlab.luigi.site.P2", "ourlab.luigi.site.P3", "ourlab.luigi.site.P4", "ourlab.luigi.site.P5"]},
		output: {
			"siteModel": {
				"site": "{{site}}",
				"siteModel": "ourlab.siteModel1"
			}
		}
	}},
	{
		"siteModel": {
			"site": "ourlab.luigi.site.READER",
			"siteModel": "ourlab.siteModel1"
		}
	}, {
		"siteModel": {
			"site": "ourlab.luigi.site.REGRIP",
			"siteModel": "ourlab.siteModel1"
		}
	}, {
		"siteModel": {
			"site": "ourlab.luigi.site.ROBOSEAL",
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
			"agent": "ourlab.luigi.evoware",
			"equipment": "ourlab.luigi.centrifuge",
			"model": model,
			"site1": "ourlab.luigi.site.CENTRIFUGE_2",
			"site2": "ourlab.luigi.site.CENTRIFUGE_4"
		}};
	}),
	// ROMA1 Narrow
	_.map(["P1", "P2", "P3", "P4", "P5", "REGRIP"], function(s) {
		return {"siteCliqueSite": {"siteClique": "ourlab.luigi.siteClique1", "site": "ourlab.luigi.site."+s}};
	}),
	{
		"transporter.canAgentEquipmentProgramSites": {
			"agent": "ourlab.luigi.evoware",
			"equipment": "ourlab.luigi.roma1",
			"program": "Narrow",
			"siteClique": "ourlab.luigi.siteClique1"
		}
	},
	// ROMA2 Narrow
	_.map(["P1", "P2", "P3", "P4", "P5", "ROBOSEAL", "REGRIP"], function(s) {
		return {"siteCliqueSite": {"siteClique": "ourlab.luigi.siteClique2", "site": "ourlab.luigi.site."+s}};
	}),
	{
		"transporter.canAgentEquipmentProgramSites": {
			"agent": "ourlab.luigi.evoware",
			"equipment": "ourlab.luigi.roma2",
			"program": "Narrow",
			"siteClique": "ourlab.luigi.siteClique2"
		}
	},
	// Centrifuge ROMA1 Narrow
	_(["CENTRIFUGE_1", "CENTRIFUGE_2", "CENTRIFUGE_3", "CENTRIFUGE_4"]).map(function(s) {
		return [
			{"siteCliqueSite": {"siteClique": "ourlab.luigi.siteClique_"+s, "site": "ourlab.luigi.site.REGRIP"}},
			{"siteCliqueSite": {"siteClique": "ourlab.luigi.siteClique_"+s, "site": "ourlab.luigi.site."+s}},
			{
				"transporter.canAgentEquipmentProgramSites": {
					"agent": "ourlab.luigi.evoware",
					"equipment": "ourlab.luigi.roma1",
					"program": "Narrow",
					"siteClique": "ourlab.luigi.siteClique_"+s
				}
			}
		];
	}).flatten().value(),
	// READER ROMA2 Wide
	_(["READER"]).map(function(s) {
		return [
			{"siteCliqueSite": {"siteClique": "ourlab.luigi.siteClique_"+s, "site": "ourlab.luigi.site.REGRIP"}},
			{"siteCliqueSite": {"siteClique": "ourlab.luigi.siteClique_"+s, "site": "ourlab.luigi.site."+s}},
			{
				"transporter.canAgentEquipmentProgramSites": {
					"agent": "ourlab.luigi.evoware",
					"equipment": "ourlab.luigi.roma2",
					"program": "Wide",
					"siteClique": "ourlab.luigi.siteClique_"+s
				}
			}
		];
	}).flatten().value(),
	{"#for": {
		factors: {model: ["plateModel_384_square"]},
		output: {
			"fluorescenceReader.canAgentEquipmentModelSite": {
				"agent": "ourlab.luigi.evoware",
				"equipment": "ourlab.luigi.reader",
				"model": "ourlab.model.{{model}}",
				"site": "ourlab.luigi.site.READER"
			}
		}
	}},
	{
		"sealer.canAgentEquipmentProgramModelSite": {
			"agent": "ourlab.luigi.evoware",
			"equipment": "ourlab.luigi.sealer",
			"program": "C:\\HJBioanalytikGmbH\\RoboSeal3\\RoboSeal_PlateParameters\\PerkinElmer_weiss.bcf",
			"model": "ourlab.model.plateModel_96_square_transparent_nunc",
			"site": "ourlab.luigi.site.ROBOSEAL"
		}
	},
	{
		"sealer.canAgentEquipmentProgramModelSite": {
			"agent": "ourlab.luigi.evoware",
			"equipment": "ourlab.luigi.sealer",
			"program": "C:\\HJBioanalytikGmbH\\RoboSeal3\\RoboSeal_PlateParameters\\Greiner_384_schwarz.bcf",
			"model": "ourlab.model.plateModel_384_square",
			"site": "ourlab.luigi.site.ROBOSEAL"
		}
	},
	{
		"pipetter.canAgentEquipment": {
			"agent": "ourlab.luigi.evoware",
			"equipment": "ourlab.luigi.liha"
		}
	},
	{"#for": {
		factors: {site: ["P2", "P3", "P4", "P5", "R1", "R2", "R3", "R4", "R5", "R6", "SYSTEM", "T1", "T2", "T3"]},
		output: {
			"pipetter.canAgentEquipmentSite": {
				"agent": "ourlab.luigi.evoware",
				"equipment": "ourlab.luigi.liha",
				"site": "ourlab.luigi.site.{{site}}"
			}
		}
	}},
	{"#for": {
		factors: {i: [1, 2, 3, 4, 5, 6, 7, 8]},
		output: {
			"pipetter.canAgentEquipmentSyringe": {
				"agent": "ourlab.luigi.evoware",
				"equipment": "ourlab.luigi.liha",
				"syringe": "ourlab.luigi.liha.syringe.{{i}}"
			}
		}
	}},
	{"timer.canAgentEquipment": {
		"agent": "ourlab.luigi.evoware",
		"equipment": "ourlab.luigi.timer1",
	}},
	_.map([1,2,3,4], function(n) {
		return {"method": {"description": "generic.closeSite-CENTRIFUGE_"+n,
			"task": {"generic.closeSite": {"site": "?site"}},
			"preconditions": [
				{"same": {"thing1": "?site", "thing2": "ourlab.luigi.site.CENTRIFUGE_"+n}}
			],
			"subtasks": {"ordered": [
				{"ourlab.luigi.centrifuge.close": {"agent": "ourlab.luigi.evoware", "equipment": "ourlab.luigi.centrifuge"}}
			]}
		}}
	}),
	{"action": {"description": "ourlab.luigi.centrifuge.close: close the centrifuge",
		"task": {"ourlab.luigi.centrifuge.close": {"agent": "?agent", "equipment": "?equipment"}},
		"preconditions": [],
		"deletions": [],
		"additions": [
			{"siteIsClosed": {"site": "ourlab.luigi.site.CENTRIFUGE_1"}},
			{"siteIsClosed": {"site": "ourlab.luigi.site.CENTRIFUGE_2"}},
			{"siteIsClosed": {"site": "ourlab.luigi.site.CENTRIFUGE_3"}},
			{"siteIsClosed": {"site": "ourlab.luigi.site.CENTRIFUGE_4"}}
		]
	}},
	_.map([1,2,3,4], function(n) {
		return {"method": {"description": "generic.openSite-CENTRIFUGE_"+n,
			"task": {"generic.openSite": {"site": "?site"}},
			"preconditions": [{"same": {"thing1": "?site", "thing2": "ourlab.luigi.site.CENTRIFUGE_"+n}}],
			"subtasks": {"ordered": [_.fromPairs([["ourlab.luigi.centrifuge.open"+n, {}]])]}
		}};
	}),
	_.map([1,2,3,4], function(n) {
		return {"action": {"description": "ourlab.luigi.centrifuge.open: open an internal site on the centrifuge",
			"task": _.fromPairs([["ourlab.luigi.centrifuge.open"+n, {}]]),
			"preconditions": [],
			"deletions": [
				{"siteIsClosed": {"site": "ourlab.luigi.site.CENTRIFUGE_"+n}}
			],
			"additions": _.map(_.without([1,2,3,4], n), function(n2) {
				return {"siteIsClosed": {"site": "ourlab.luigi.site.CENTRIFUGE_"+n2}};
			})
		}};
	}),

	// Open READER
	{"method": {"description": "generic.openSite-READER",
		"task": {"generic.openSite": {"site": "?site"}},
		"preconditions": [{"same": {"thing1": "?site", "thing2": "ourlab.luigi.site.READER"}}],
		"subtasks": {"ordered": [{"ourlab.luigi.reader.open": {}}]}
	}},
	{"action": {"description": "ourlab.luigi.reader.open: open the reader",
		"task": {"ourlab.luigi.reader.open": {}},
		"preconditions": [],
		"deletions": [{"siteIsClosed": {"site": "ourlab.luigi.site.READER"}}],
		"additions": []
	}},
	// Close READER
	{"method": {"description": "generic.closeSite-READER",
		"task": {"generic.closeSite": {"site": "?site"}},
		"preconditions": [{"same": {"thing1": "?site", "thing2": "ourlab.luigi.site.READER"}}],
		"subtasks": {"ordered": [{"ourlab.luigi.reader.close": {}}]}
	}},
	{"action": {"description": "ourlab.luigi.reader.close: close the reader",
		"task": {"ourlab.luigi.reader.close": {}},
		"preconditions": [],
		"deletions": [],
		"additions": [
			{"siteIsClosed": {"site": "ourlab.luigi.site.READER"}}
		]
	}},

	]),

	schemas: {
		"EvowareRobot": {
			properties: {
				type: {enum: ["EvowareRobot"]}
			},
			required: ["type"]
		},
		"EvowareWashProgram": {
			properties: {
				type: {enum: ["EvowareWashProgram"]}
			},
			required: ["type"]
		},
		"equipment.close|ourlab.luigi.evoware|ourlab.luigi.centrifuge": {
			properties: {
				agent: {description: "Agent identifier", type: "Agent"},
				equipment: {description: "Equipment identifier", type: "Equipment"},
			},
			required: ["agent", "equipment"]
		},
		"equipment.open|ourlab.luigi.evoware|ourlab.luigi.centrifuge": {
			properties: {
				agent: {description: "Agent identifier", type: "Agent"},
				equipment: {description: "Equipment identifier", type: "Equipment"},
			},
			required: ["agent", "equipment"]
		},
		"equipment.openSite|ourlab.luigi.evoware|ourlab.luigi.centrifuge": {
			properties: {
				agent: {description: "Agent identifier", type: "Agent"},
				equipment: {description: "Equipment identifier", type: "Equipment"},
				site: {description: "Site identifier", type: "Site"}
			},
			required: ["agent", "equipment", "site"]
		},
		"equipment.run|ourlab.luigi.evoware|ourlab.luigi.centrifuge": {
			properties: {
				agent: {description: "Agent identifier", type: "Agent"},
				equipment: {description: "Equipment identifier", type: "Equipment"},
				program: {
					description: "Program for centrifuging",
					type: "object",
					properties: {
						rpm: {type: "number", default: 3000},
						duration: {type: "Duration", default: "30 s"},
						spinUpTime: {type: "Duration", default: "9 s"},
						spinDownTime: {type: "Duration", default: "9 s"},
						temperature: {type: "Temperature", default: "25 degC"}
					}
				}
			},
			required: ["program"]
		},
		"equipment.run|ourlab.luigi.evoware|ourlab.luigi.sealer": {
			properties: {
				agent: {description: "Agent identifier", type: "Agent"},
				equipment: {description: "Equipment identifier", type: "Equipment"},
				program: {description: "Program identifier for sealing", type: "string"}
			},
			required: ["agent", "equipment", "program"]
		},
		"equipment.close|ourlab.luigi.evoware|ourlab.luigi.reader": {
			properties: {
				agent: {description: "Agent identifier", type: "Agent"},
				equipment: {description: "Equipment identifier", type: "Equipment"},
			},
			required: ["agent", "equipment"]
		},
		"equipment.open|ourlab.luigi.evoware|ourlab.luigi.reader": {
			properties: {
				agent: {description: "Agent identifier", type: "Agent"},
				equipment: {description: "Equipment identifier", type: "Equipment"},
			},
			required: ["agent", "equipment"]
		},
		"equipment.openSite|ourlab.luigi.evoware|ourlab.luigi.reader": {
			properties: {
				agent: {description: "Agent identifier", type: "Agent"},
				equipment: {description: "Equipment identifier", type: "Equipment"},
				site: {description: "Site identifier", type: "Site"}
			},
			required: ["agent", "equipment", "site"]
		},
		"equipment.run|ourlab.luigi.evoware|ourlab.luigi.reader": {
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
		"equipment.run|ourlab.luigi.evoware|ourlab.luigi.shaker": {
			properties: {
				agent: {description: "Agent identifier", type: "Agent"},
				equipment: {description: "Equipment identifier", type: "Equipment"},
				program: {description: "Program identifier for shaking", type: "string"}
			}
		},
		"pipetter.cleanTips|ourlab.luigi.evoware|ourlab.luigi.liha": {
			description: "Clean the pipetter tips.",
			properties: {
				agent: {description: "Agent identifier", type: "Agent"},
				equipment: {description: "Equipment identifier", type: "Equipment"},
				program: {description: "Program identifier", type: "string"},
				items: {
					description: "List of which syringes to clean at which intensity",
					type: "array",
					items: {
						type: "object",
						properties: {
							syringe: {description: "Syringe identifier", type: "Syringe"},
							intensity: {description: "Intensity of the cleaning", type: "pipetter.CleaningIntensity"}
						},
						required: ["syringe", "intensity"]
					}
				}
			},
			required: ["agent", "equipment", "items"]
		}
	},

	commandHandlers: {
		"equipment.close|ourlab.luigi.evoware|ourlab.luigi.centrifuge": function(params, parsed, data) {
			return {expansion: [makeEvowareFacts(parsed, data, "Close")]};
		},
		"equipment.open|ourlab.luigi.evoware|ourlab.luigi.centrifuge": function(params, parsed, data) {
			return {expansion: [makeEvowareFacts(parsed, data, "Open")]};
		},
		"equipment.openSite|ourlab.luigi.evoware|ourlab.luigi.centrifuge": function(params, parsed, data) {
			var carrier = commandHelper.getParsedValue(parsed, data, "equipment", "evowareId");
			var sitesInternal = commandHelper.getParsedValue(parsed, data, "equipment", "sitesInternal");
			var siteIndex = sitesInternal.indexOf(parsed.objectName.site);
			expect.truthy({paramName: "site"}, siteIndex >= 0, "site must be one of the equipments internal sites: "+sitesInternal.join(", "));
			return {
				expansion: [
					{
						command: "evoware._facts",
						agent: parsed.objectName.agent,
						factsEquipment: carrier,
						factsVariable: carrier+"_MoveToPos",
						factsValue: (siteIndex+1).toString()
					},
					{
						command: "evoware._facts",
						agent: parsed.objectName.agent,
						factsEquipment: carrier,
						factsVariable: carrier+"_Open"
					},
				]
			};
		},
		"equipment.run|ourlab.luigi.evoware|ourlab.luigi.centrifuge": function(params, parsed, data) {
			//console.log("equipment.run|ourlab.luigi.evoware|ourlab.luigi.centrifuge:")
			//console.log({parsed, params})
			const parsedProgram = parsed.value.program;
			//console.log({parsedProgram});
			var list = [
				math.round(parsedProgram.rpm),
				math.round(parsedProgram.duration.toNumber('s')),
				math.round(parsedProgram.spinUpTime.toNumber('s')),
				math.round(parsedProgram.spinDownTime.toNumber('s')),
				math.round(parsedProgram.temperature.toNumber('degC'))
			];
			var value = list.join(",");
			return {expansion: [makeEvowareFacts(parsed, data, "Execute1", value)]};
		},
		// Reader
		"equipment.close|ourlab.luigi.evoware|ourlab.luigi.reader": function(params, parsed, data) {
			return {expansion: [makeEvowareFacts(parsed, data, "Close")]};
		},
		"equipment.open|ourlab.luigi.evoware|ourlab.luigi.reader": function(params, parsed, data) {
			return {expansion: [makeEvowareFacts(parsed, data, "Open")]};
		},
		"equipment.openSite|ourlab.luigi.evoware|ourlab.luigi.reader": function(params, parsed, data) {
			var carrier = commandHelper.getParsedValue(parsed, data, "equipment", "evowareId");
			var sitesInternal = commandHelper.getParsedValue(parsed, data, "equipment", "sitesInternal");
			var siteIndex = sitesInternal.indexOf(parsed.objectName.site);
			expect.truthy({paramName: "site"}, siteIndex >= 0, "site must be one of the equipments internal sites: "+sitesInternal.join(", "));

			return {expansion: [makeEvowareFacts(parsed, data, "Open")]};
		},
		"equipment.run|ourlab.luigi.evoware|ourlab.luigi.reader": function(params, parsed, data) {
			var hasProgramFile = (parsed.value.programFile) ? 1 : 0;
			var hasProgramData = (parsed.value.programData) ? 1 : 0;
			//console.log(parsed);
			expect.truthy({}, hasProgramFile + hasProgramData >= 1, "either `programFile` or `programData` must be specified.");
			expect.truthy({}, hasProgramFile + hasProgramData <= 1, "only one of `programFile` or `programData` may be specified.");
			const content = (hasProgramData)
				? parsed.value.programData.toString('utf8')
				: parsed.value.programFile.toString('utf8');
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
			var value = parsed.value.outputFile + "|" + programData;
			return {expansion: [makeEvowareFacts(parsed, data, "Measure", value)]};
		},
		// Sealer
		"equipment.run|ourlab.luigi.evoware|ourlab.luigi.sealer": function(params, parsed, data) {
			const carrier = commandHelper.getParsedValue(parsed, data, "equipment", "evowareId");
			return {
				expansion: [{
					command: "evoware._facts",
					agent: parsed.objectName.agent,
					factsEquipment: carrier,
					factsVariable: carrier+"_Seal",
					factsValue: parsed.value.program
				}],
				//effects: _.fromPairs([[params.object + ".sealed", true]])
			};
		},
		"evoware._facts": function(params, parsed, data) {},
		// Clean tips
		"pipetter.cleanTips|ourlab.luigi.evoware|ourlab.luigi.liha": function(params, parsed, data) {
			//console.log("pipetter.cleanTips|ourlab.luigi.evoware|ourlab.luigi.liha")
			//console.log(JSON.stringify(parsed, null, '  '))

			const cleaningIntensities = data.schemas["pipetter.CleaningIntensity"].enum;
			const syringeNameToItems = _.map(parsed.value.items, (item, index) => [parsed.objectName[`items.${index}.syringe`], item]);
			//console.log(syringeNameToItems);

			const expansionList = [];
			const sub = function(syringeNames, volume) {
				const syringeNameToItems2 = _.filter(syringeNameToItems, ([syringeName, ]) =>
					_.includes(syringeNames, syringeName)
				);
				//console.log({syringeNameToItems2})
				if (!_.isEmpty(syringeNameToItems2)) {
					const value = _.max(_.map(syringeNameToItems2, ([, item]) => cleaningIntensities.indexOf(item.intensity)));
					if (value >= 0) {
						const intensity = cleaningIntensities[value];
						const syringes = _.map(syringeNameToItems2, ([syringeName, ]) => syringeName);
						expansionList.push({
							command: "pipetter._washTips",
							agent: parsed.objectName.agent,
							equipment: parsed.objectName.equipment,
							program: `ourlab.luigi.washProgram.${intensity}_${volume}`,
							intensity: intensity,
							syringes: syringeNames
						});
					}
				}
			}
			sub(_.map([1, 2, 3, 4], n => `ourlab.luigi.liha.syringe.${n}`), "1000");
			sub(_.map([5, 6, 7, 8], n => `ourlab.luigi.liha.syringe.${n}`), "0050");
			return {expansion: expansionList};
		}
	},

	planHandlers: {
		"ourlab.luigi.centrifuge.close": function(params, parentParams, data) {
			return [{
				command: "equipment.close",
				agent: "ourlab.luigi.evoware",
				equipment: "ourlab.luigi.centrifuge"
			}];
		},
		"ourlab.luigi.centrifuge.open1": function(params, parentParams, data) {
			return [{
				command: "equipment.openSite",
				agent: "ourlab.luigi.evoware",
				equipment: "ourlab.luigi.centrifuge",
				site: "ourlab.luigi.site.CENTRIFUGE_1"
			}];
		},
		"ourlab.luigi.centrifuge.open2": function(params, parentParams, data) {
			return [{
				command: "equipment.openSite",
				agent: "ourlab.luigi.evoware",
				equipment: "ourlab.luigi.centrifuge",
				site: "ourlab.luigi.site.CENTRIFUGE_2"
			}];
		},
		"ourlab.luigi.centrifuge.open3": function(params, parentParams, data) {
			return [{
				command: "equipment.openSite",
				agent: "ourlab.luigi.evoware",
				equipment: "ourlab.luigi.centrifuge",
				site: "ourlab.luigi.site.CENTRIFUGE_3"
			}];
		},
		"ourlab.luigi.centrifuge.open4": function(params, parentParams, data) {
			return [{
				command: "equipment.openSite",
				agent: "ourlab.luigi.evoware",
				equipment: "ourlab.luigi.centrifuge",
				site: "ourlab.luigi.site.CENTRIFUGE_4"
			}];
		},
		"ourlab.luigi.reader.close": function(params, parentParams, data) {
			return [{
				command: "equipment.close",
				agent: "ourlab.luigi.evoware",
				equipment: "ourlab.luigi.reader"
			}];
		},
		"ourlab.luigi.reader.open": function(params, parentParams, data) {
			return [{
				command: "equipment.openSite",
				agent: "ourlab.luigi.evoware",
				equipment: "ourlab.luigi.reader",
				site: "ourlab.luigi.site.READER"
			}];
		},
	}
}
