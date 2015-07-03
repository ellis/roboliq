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
				}
			},
			"model1": {
				"type": "PlateModel",
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
			"equipment": "ourlab.mario.roma1",
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
	}, ]
}
