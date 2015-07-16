var _ = require('lodash');

module.exports = {
	objects: {
		"ourlab": {
			"type": "Namespace",
			"mario": {
				"type": "Namespace",
				"centrifuge": {
					"type": "Centrifuge",
					"sitesInternal": ["ourlab.mario.site.CENTRIFUGE_1", "ourlab.mario.site.CENTRIFUGE_2", "ourlab.mario.site.CENTRIFUGE_3", "ourlab.mario.site.CENTRIFUGE_4"]
				},
				"evoware": {
					"type": "EvowareRobot"
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
					"type": "Sealer"
				},
				"site": {
					"type": "Namespace",
		            "CENTRIFUGE_1": { "type": "Site", "evowareCarrier": "Centrifuge", "evowareGrid": 54, "site": 1, "closed": true },
		            "CENTRIFUGE_2": { "type": "Site", "evowareCarrier": "Centrifuge", "evowareGrid": 54, "site": 2, "closed": true },
		            "CENTRIFUGE_3": { "type": "Site", "evowareCarrier": "Centrifuge", "evowareGrid": 54, "site": 1, "closed": true },
		            "CENTRIFUGE_4": { "type": "Site", "evowareCarrier": "Centrifuge", "evowareGrid": 54, "site": 2, "closed": true },
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
					}
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
	}, {
		"siteModel": {
			"site": "ourlab.mario.site.P2",
			"siteModel": "ourlab.siteModel1"
		}
	}, {
		"siteModel": {
			"site": "ourlab.mario.site.P3",
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
	// ROMA1 Narrow
	_.map(["P1", "P2", "P3", "REGRIP"], function(s) {
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
	_.map(["P1", "P2", "P3", "ROBOSEAL", "REGRIP"], function(s) {
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
	{
		"sealer.canAgentEquipmentProgramModelSite": {
			"agent": "ourlab.mario.evoware",
			"equipment": "ourlab.mario.sealer",
			"program": "C:\\HJBioanalytikGmbH\\RoboSeal3\\RoboSeal_PlateParameters\\PerkinElmer_weiss.bcf",
			"model": "ourlab.model.plateModel_96_square_transparent_nunc",
			"site": "ourlab.mario.site.ROBOSEAL"
		}
	}, {
		"sealer.canAgentEquipmentProgramModelSite": {
			"agent": "ourlab.mario.evoware",
			"equipment": "ourlab.mario.sealer",
			"program": "C:\\HJBioanalytikGmbH\\RoboSeal3\\RoboSeal_PlateParameters\\Greiner_384_schwarz.bcf",
			"model": "ourlab.model.plateModel_384_square",
			"site": "ourlab.mario.site.ROBOSEAL"
		}
	}, {
		"pipetter.canAgentEquipmentSite": {
			"agent": "ourlab.mario.evoware",
			"equipment": "ourlab.mario.liha",
			"site": "ourlab.mario.site.P2"
		}
	}, {
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
	{"method": {"description": "generic.closeSite-CENTRIFUGE_4",
		"task": {"generic.closeSite": {"site": "?site"}},
		"preconditions": [
			{"same": {"thing1": "?site", "thing2": "ourlab.mario.site.CENTRIFUGE_4"}}
		],
		"subtasks": {"ordered": [
			{"ourlab.mario.centrifuge.close": {"agent": "ourlab.mario.evoware", "equipment": "ourlab.mario.centrifuge"}}
		]}
	}},
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
	{"method": {"description": "generic.openSite-CENTRIFUGE_4",
		"task": {"generic.openSite": {"site": "?site"}},
		"preconditions": [{"same": {"thing1": "?site", "thing2": "ourlab.mario.site.CENTRIFUGE_4"}}],
		"subtasks": {"ordered": [{"ourlab.mario.centrifuge.open4": {}}]}
	}},
	{"action": {"description": "centrifuge.instruction.openSite: open an internal site on the centrifuge",
		"task": {"ourlab.mario.centrifuge.open4": {"agent": "?agent", "equipment": "?equipment", "site": "?site"}},
		"preconditions": [],
		"deletions": [
			{"siteIsClosed": {"site": "ourlab.mario.site.CENTRIFUGE_4"}}
		],
		"additions": [
			{"siteIsClosed": {"site": "ourlab.mario.site.CENTRIFUGE_1"}},
			{"siteIsClosed": {"site": "ourlab.mario.site.CENTRIFUGE_2"}},
			{"siteIsClosed": {"site": "ourlab.mario.site.CENTRIFUGE_3"}}
		]
	}},
	]),
	planHandlers: {
		"ourlab.mario.centrifuge.close": function(params, parentParams, data) {
			return [{
				command: "centrifuge.instruction.close",
				agent: "ourlab.mario.evoware",
				equipment: "ourlab.mario.centrifuge"
			}];
		},
		"ourlab.mario.centrifuge.open1": function(params, parentParams, data) {
			return [{
				command: "centrifuge.instruction.openSite",
				agent: "ourlab.mario.evoware",
				equipment: "ourlab.mario.centrifuge",
				site: "ourlab.mario.site.CENTRIFUGE_1"
			}];
		},
		"ourlab.mario.centrifuge.open2": function(params, parentParams, data) {
			return [{
				command: "centrifuge.instruction.openSite",
				agent: "ourlab.mario.evoware",
				equipment: "ourlab.mario.centrifuge",
				site: "ourlab.mario.site.CENTRIFUGE_2"
			}];
		},
		"ourlab.mario.centrifuge.open3": function(params, parentParams, data) {
			return [{
				command: "centrifuge.instruction.openSite",
				agent: "ourlab.mario.evoware",
				equipment: "ourlab.mario.centrifuge",
				site: "ourlab.mario.site.CENTRIFUGE_3"
			}];
		},
		"ourlab.mario.centrifuge.open4": function(params, parentParams, data) {
			return [{
				command: "centrifuge.instruction.openSite",
				agent: "ourlab.mario.evoware",
				equipment: "ourlab.mario.centrifuge",
				site: "ourlab.mario.site.CENTRIFUGE_4"
			}];
		},
	}
}
