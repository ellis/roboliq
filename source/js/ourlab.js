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
				"washProgram_flush_1000": {
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
				"washProgram_flush_0050": {
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
				"washProgram_light_1000": {
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
				"washProgram_thorough_1000": {
					"type": "EvowareWashProgram",
					"script": "C:\\ProgramData\\TECAN\\EVOware\\database\\scripts\\Roboliq\\Roboliq_Clean_Thorough_1000.esc"
				},
				"washProgram_thorough_0050": {
					"type": "EvowareWashProgram",
					"script": "C:\\ProgramData\\TECAN\\EVOware\\database\\scripts\\Roboliq\\Roboliq_Clean_Thorough_0050.esc"
				}
			},
			"model1": {
				"type": "PlateModel",
				"rows": 8,
				"columns": 12,
				"evowareName": "Ellis Nunc F96 MicroWell"
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
			"site": "ourlab.mario.P2",
			"siteModel": "ourlab.siteModel1"
		}
	}, {
		"siteModel": {
			"site": "ourlab.mario.P3",
			"siteModel": "ourlab.siteModel1"
		}
	}, {
		"siteModel": {
			"site": "ourlab.mario.ROBOSEAL",
			"siteModel": "ourlab.siteModel1"
		}
	}, {
		"stackable": {
			"below": "ourlab.siteModel1",
			"above": "ourlab.model1"
		}
	}, {
		"movePlate_canAgentEquipmentProgramModelSite": {
			"agent": "ourlab.mario.evoware",
			"equipment": "ourlab.mario.roma1",
			"program": "Narrow",
			"model": "ourlab.model1",
			"site": "ourlab.mario.P2"
		}
	}, {
		"movePlate_canAgentEquipmentProgramModelSite": {
			"agent": "ourlab.mario.evoware",
			"equipment": "ourlab.mario.roma1",
			"program": "Narrow",
			"model": "ourlab.model1",
			"site": "ourlab.mario.P3"
		}
	}, {
		"movePlate_canAgentEquipmentProgramModelSite": {
			"agent": "ourlab.mario.evoware",
			"equipment": "ourlab.mario.roma2",
			"program": "Narrow",
			"model": "ourlab.model1",
			"site": "ourlab.mario.P2"
		}
	}, {
		"movePlate_canAgentEquipmentProgramModelSite": {
			"agent": "ourlab.mario.evoware",
			"equipment": "ourlab.mario.roma2",
			"program": "Narrow",
			"model": "ourlab.model1",
			"site": "ourlab.mario.ROBOSEAL"
		}
	}, {
		"movePlate_excludePath": {
			"siteA": "ourlab.mario.P3",
			"siteB": "ourlab.mario.ROBOSEAL"
		}
	}, {
		"sealer.canAgentEquipmentProgramModelSite": {
			"agent": "ourlab.mario.evoware",
			"equipment": "ourlab.mario.sealer",
			"program": "C:\\HJBioanalytikGmbH\\RoboSeal3\\RoboSeal_PlateParameters\\PerkinElmer_weiss.bcf",
			"model": "ourlab.model1",
			"site": "ourlab.mario.ROBOSEAL"
		}
	}, {
		"pipetter.canAgentEquipmentSite": {
			"agent": "ourlab.mario.evoware",
			"equipment": "ourlab.mario.liha",
			"site": "ourlab.mario.P2"
		}
	}, {
		"pipetter.canAgentEquipmentSite": {
			"agent": "ourlab.mario.evoware",
			"equipment": "ourlab.mario.liha",
			"site": "ourlab.mario.P3"
		}
	}, {
		"pipetter.cleanTips.canAgentEquipmentProgramModelIntensity": {
			"agent": "ourlab.mario.evoware",
			"equipment": "ourlab.mario.liha",
			"program": "ourlab.mario.washProgram_flush_1000",
			"model": "tipModel1000",
			"intensity": "flush"
		}
	}, {
		"pipetter.cleanTips.canAgentEquipmentProgramModelIntensity": {
			"agent": "ourlab.mario.evoware",
			"equipment": "ourlab.mario.liha",
			"program": "ourlab.mario.washProgram_light_1000",
			"model": "tipModel1000",
			"intensity": "light"
		}
	}, {
		"pipetter.cleanTips.canAgentEquipmentProgramModelIntensity": {
			"agent": "ourlab.mario.evoware",
			"equipment": "ourlab.mario.liha",
			"program": "ourlab.mario.washProgram_thorough_1000",
			"model": "tipModel1000",
			"intensity": "thorough"
		}
	}, ]
}
