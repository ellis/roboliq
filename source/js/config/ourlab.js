module.exports = {
	objects: {
		"ourlab": {
			"type": "Namespace",
			"mario": {
				"type": "Namespace",
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
					"wells": ["systemLiquidLabware(A01)", "systemLiquidLabware(B01)", "systemLiquidLabware(C01)", "systemLiquidLabware(D01)"]
				},
				"systemLiquidLabware": {
					"description": "dummy labware representing the system liquid source",
					"model": "SystemLiquidLabwareModel",
					"location": "SYSTEM",
					"contents": ["0ul"]
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
				},
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
		},
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

	predicates: [{
		"isSiteModel": {
			"model": "ourlab.siteModel1"
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
			"site": "ourlab.mario.site.ROBOSEAL",
			"siteModel": "ourlab.siteModel1"
		}
	}, {
		"stackable": {
			"below": "ourlab.siteModel1",
			"above": "ourlab.model.plateModel_96_square_transparent_nunc"
		}
	}, {
		"movePlate_canAgentEquipmentProgramModelSite": {
			"agent": "ourlab.mario.evoware",
			"equipment": "ourlab.mario.roma1",
			"program": "Narrow",
			"model": "ourlab.model.plateModel_96_square_transparent_nunc",
			"site": "ourlab.mario.site.P2"
		}
	}, {
		"movePlate_canAgentEquipmentProgramModelSite": {
			"agent": "ourlab.mario.evoware",
			"equipment": "ourlab.mario.roma1",
			"program": "Narrow",
			"model": "ourlab.model.plateModel_96_square_transparent_nunc",
			"site": "ourlab.mario.site.P3"
		}
	}, {
		"movePlate_canAgentEquipmentProgramModelSite": {
			"agent": "ourlab.mario.evoware",
			"equipment": "ourlab.mario.roma2",
			"program": "Narrow",
			"model": "ourlab.model.plateModel_96_square_transparent_nunc",
			"site": "ourlab.mario.site.P2"
		}
	}, {
		"movePlate_canAgentEquipmentProgramModelSite": {
			"agent": "ourlab.mario.evoware",
			"equipment": "ourlab.mario.roma2",
			"program": "Narrow",
			"model": "ourlab.model.plateModel_96_square_transparent_nunc",
			"site": "ourlab.mario.site.ROBOSEAL"
		}
	}, {
		"movePlate_excludePath": {
			"siteA": "ourlab.mario.site.P3",
			"siteB": "ourlab.mario.site.ROBOSEAL"
		}
	}, {
		"sealer.canAgentEquipmentProgramModelSite": {
			"agent": "ourlab.mario.evoware",
			"equipment": "ourlab.mario.sealer",
			"program": "C:\\HJBioanalytikGmbH\\RoboSeal3\\RoboSeal_PlateParameters\\PerkinElmer_weiss.bcf",
			"model": "ourlab.model.plateModel_96_square_transparent_nunc",
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
	}, ]
}
