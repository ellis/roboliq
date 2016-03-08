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

/**
 * Expect spec of this form:
 * ``{siteModel: string, sites: [string], labwareModels: [string]}``
 */
function makeSiteModelPredicates(spec) {
	return _.flatten([
		{isSiteModel: {model: spec.siteModel}},
		_.map(spec.sites, site => ({siteModel: {site, siteModel: spec.siteModel}})),
		_.map(spec.labwareModels, labwareModel => ({stackable: {below: spec.siteModel, above: labwareModel}}))
	]);
}

/**
 * Expect specs of this form:
 * ``{<transporter>: {<program>: [site names]}}``
 */
function makeTransporterPredicates(specs) {
	let siteCliqueId = 1;
	const l = [];
	_.forEach(specs, (programs, equipment) => {
		_.forEach(programs, (cliques, program) => {
			_.forEach(cliques, (sites) => {
				const siteClique = "ourlab.luigi.siteClique"+siteCliqueId;
				siteCliqueId++;
				_.forEach(sites, site => {
					l.push({"siteCliqueSite": {siteClique, site}});
				});
				l.push({
					"transporter.canAgentEquipmentProgramSites": {
						"agent": "ourlab.luigi.evoware",
						equipment,
						program,
						siteClique
					}
				});
			});
		});
	});
	return l;
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
					"type": "EvowareRobot",
					config: {
						pathToRoboliqRuntimeCli: "C:\\Users\\localadmin\\Documents\\Ellis\\roboliq\\runtime-server\\roboliq-runtime-cli.vbs"
					}
				},
				"reader": {
					"type": "Reader",
					"sitesInternal": ["ourlab.luigi.site.READER"],
					"evowareId": "ReaderNETwork"
				},
				"roma0": {
					"type": "Transporter",
					"description": "Fake transporter that allows for the logical transfer of a plate between REGRIP_ABOVE and REGRIP_BELOW, which are in fact the same location",
					"evowareRoma": -1
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
					HOTEL12_1: { type: "Site", evowareCarrier: "Hotel 3x4Pos Grid25to39", evowareGrid: 25, evowareSite: 4 },
					HOTEL12_2: { type: "Site", evowareCarrier: "Hotel 3x4Pos Grid25to39", evowareGrid: 25, evowareSite: 3 },
					HOTEL12_3: { type: "Site", evowareCarrier: "Hotel 3x4Pos Grid25to39", evowareGrid: 25, evowareSite: 2 },
					HOTEL12_4: { type: "Site", evowareCarrier: "Hotel 3x4Pos Grid25to39", evowareGrid: 25, evowareSite: 1 },
					HOTEL12_5: { type: "Site", evowareCarrier: "Hotel 3x4Pos Grid25to39", evowareGrid: 25, evowareSite: 8 },
					HOTEL12_6: { type: "Site", evowareCarrier: "Hotel 3x4Pos Grid25to39", evowareGrid: 25, evowareSite: 7 },
					HOTEL12_7: { type: "Site", evowareCarrier: "Hotel 3x4Pos Grid25to39", evowareGrid: 25, evowareSite: 6 },
					HOTEL12_8: { type: "Site", evowareCarrier: "Hotel 3x4Pos Grid25to39", evowareGrid: 25, evowareSite: 5 },
					HOTEL12_9: { type: "Site", evowareCarrier: "Hotel 3x4Pos Grid25to39", evowareGrid: 25, evowareSite: 12 },
					HOTEL12_10: { type: "Site", evowareCarrier: "Hotel 3x4Pos Grid25to39", evowareGrid: 25, evowareSite: 11 },
					HOTEL12_11: { type: "Site", evowareCarrier: "Hotel 3x4Pos Grid25to39", evowareGrid: 25, evowareSite: 10 },
					HOTEL12_12: { type: "Site", evowareCarrier: "Hotel 3x4Pos Grid25to39", evowareGrid: 25, evowareSite: 9 },
					INCUBATOR1_1: { type: "Site", evowareCarrier: "Incubator1", evowareGrid: 1, evowareSite: 4, closed: true },
					INCUBATOR1_2: { type: "Site", evowareCarrier: "Incubator1", evowareGrid: 1, evowareSite: 3, closed: true },
					INCUBATOR1_3: { type: "Site", evowareCarrier: "Incubator1", evowareGrid: 1, evowareSite: 2, closed: true },
					INCUBATOR1_4: { type: "Site", evowareCarrier: "Incubator1", evowareGrid: 1, evowareSite: 1, closed: true },
					LIGHT: { type: "Site", evowareCarrier: "Pickolo-Light-Table", evowareGrid: 29, evowareSite: 1 },
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
					REGRIP_ABOVE: { type: "Site", evowareCarrier: "ReGrip above", evowareGrid: 8, evowareSite: 1 },
					REGRIP_BELOW: { type: "Site", evowareCarrier: "ReGrip below", evowareGrid: 9, evowareSite: 1 },
					ROBOSEAL: { "type": "Site", "evowareCarrier": "RoboSeal", "evowareGrid": 45, "evowareSite": 1 },
					SHAKER: { type: "Site", evowareCarrier: "Te-Shake 1Pos Front", evowareGrid: 44, evowareSite: 1 },
					TRANSFER_1: { type: "Site", evowareCarrier: "Hotel 4Pos Transfer Grid 0", evowareGrid: 7, evowareSite: 1 },
					TRANSFER_2: { type: "Site", evowareCarrier: "Hotel 4Pos Transfer Grid 0", evowareGrid: 7, evowareSite: 2 },
					TRANSFER_3: { type: "Site", evowareCarrier: "Hotel 4Pos Transfer Grid 0", evowareGrid: 7, evowareSite: 3 },
					TRANSFER_4: { type: "Site", evowareCarrier: "Hotel 4Pos Transfer Grid 0", evowareGrid: 7, evowareSite: 4 },
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
					//CONTINUE
					"type": "Namespace",
					"flush_2500": { // FIXME: IS THIS REALLY FLUSH?
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
				}
			},
			"model": {
				"type": "Namespace",
				"plateModel_96_dwp": {
					"type": "PlateModel",
					"label": "96 well deep-well plate",
					"rows": 8,
					"columns": 12,
					"evowareName": "DM 96-DWB GripUp",
					"evowareName_sealed": "DM 96-DWB GripUp sealed"
				},
				"plateModel_96_round_transparent_nunc": {
					"type": "PlateModel",
					"label": "96 round-well transparent Nunc plate",
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
				"troughModel_1500ml": {
					"type": "PlateModel",
					"label": "Trough 1500ml Stainless",
					"rows": 8,
					"columns": 1,
					"evowareName": "Trough 1500ml Stainless"
				},
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

	predicates: _.flatten([
		// Deepwell plates only
		makeSiteModelPredicates({
			siteModel: "ourlab.luigi.siteModel_DWP",
			sites: ["ourlab.luigi.site.P3"],
			labwareModels: ["ourlab.model.plateModel_96_dwp"]
		}),
		// Shallow plates only
		makeSiteModelPredicates({
			siteModel: "ourlab.luigi.siteModel_shallow",
			sites: ["ourlab.luigi.site.HOTEL12_4", "ourlab.luigi.site.HOTEL12_8", "ourlab.luigi.site.HOTEL12_12"],
			labwareModels: ["ourlab.model.plateModel_96_round_transparent_nunc"]
		}),
		// Deepwell blocks + microwell
		makeSiteModelPredicates({
			siteModel: "ourlab.luigi.siteModel1",
			sites: [
				"ourlab.luigi.site.HOTEL12_1", "ourlab.luigi.site.HOTEL12_2", "ourlab.luigi.site.HOTEL12_3", "ourlab.luigi.site.HOTEL12_5", "ourlab.luigi.site.HOTEL12_6", "ourlab.luigi.site.HOTEL12_7", "ourlab.luigi.site.HOTEL12_9", "ourlab.luigi.site.HOTEL12_10", "ourlab.luigi.site.HOTEL12_11",
				"ourlab.luigi.site.P1", "ourlab.luigi.site.P2", "ourlab.luigi.site.P4", "ourlab.luigi.site.P5", "ourlab.luigi.site.P6",
				"ourlab.luigi.site.LIGHT",
				"ourlab.luigi.site.REGRIP_ABOVE", "ourlab.luigi.site.REGRIP_BELOW",
				"ourlab.luigi.site.TRANSFER_1", "ourlab.luigi.site.TRANSFER_2", "ourlab.luigi.site.TRANSFER_3", "ourlab.luigi.site.TRANSFER_4",
			],
			labwareModels: ["ourlab.model.plateModel_96_dwp", "ourlab.model.plateModel_96_round_transparent_nunc"]
		}),
		makeTransporterPredicates({
			"ourlab.luigi.roma0": {
				none: [
					["ourlab.luigi.site.REGRIP_ABOVE", "ourlab.luigi.site.REGRIP_BELOW"]
				]
			},
			"ourlab.luigi.roma1": {
				Narrow: [
					[
						"ourlab.luigi.site.REGRIP_ABOVE",
						"ourlab.luigi.site.P1", "ourlab.luigi.site.P2", "ourlab.luigi.site.P3",
						"ourlab.luigi.site.LIGHT",
						"ourlab.luigi.site.ROBOSEAL",
					],
					[
						"ourlab.luigi.site.HOTEL12_1", "ourlab.luigi.site.HOTEL12_2", "ourlab.luigi.site.HOTEL12_3", "ourlab.luigi.site.HOTEL12_4",
						"ourlab.luigi.site.HOTEL12_5", "ourlab.luigi.site.HOTEL12_6", "ourlab.luigi.site.HOTEL12_7", "ourlab.luigi.site.HOTEL12_8",
						"ourlab.luigi.site.HOTEL12_9", "ourlab.luigi.site.HOTEL12_10", "ourlab.luigi.site.HOTEL12_11", "ourlab.luigi.site.HOTEL12_12",
						"ourlab.luigi.site.P4", "ourlab.luigi.site.P5", "ourlab.luigi.site.P6",
						"ourlab.luigi.site.REGRIP_BELOW",
						"ourlab.luigi.site.TRANSFER_1", "ourlab.luigi.site.TRANSFER_2", "ourlab.luigi.site.TRANSFER_3", "ourlab.luigi.site.TRANSFER_4",
					],
				]
			},
			"ourlab.luigi.roma2": {
				Narrow: [
					[
						"ourlab.luigi.site.P1", "ourlab.luigi.site.P2", "ourlab.luigi.site.P4", "ourlab.luigi.site.P5", "ourlab.luigi.site.P6",
						"ourlab.luigi.site.LIGHT",
						"ourlab.luigi.site.ROBOSEAL", "ourlab.luigi.site.SHAKER",
					],
				]
			}
		}),
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
