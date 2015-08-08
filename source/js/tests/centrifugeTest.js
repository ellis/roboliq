var _ = require('lodash');
var should = require('should');
var roboliq = require('../roboliq.js')

describe('commands/centrifgue', function() {
	var protocol0 = {
		objects: {
			plate1: {
				type: "Plate",
				model: "ourlab.model.plateModel_384_square",
				location: "ourlab.mario.site.P2"
			},
			plate2: {
				type: "Plate",
				model: "ourlab.model.plateModel_384_square",
				location: "ourlab.mario.site.P3"
			}
		}
	};

	describe('centrifuge.centrifuge2', function () {
		it('should put two plates in the centrifuge, run, then move the plates back', function () {
			var protocol = _.merge({}, protocol0, {
				steps: {
					1: {
						command: "centrifuge.centrifuge2",
						object1: "plate1",
						object2: "plate2",
						program: {
							rpm: 3000,
							duration: 120,
							temperature: 25
						}
					}
				}
			});
			var result = roboliq.run(["-o", "", "-T"], protocol);
			//console.log("result:\n"+JSON.stringify(result.output.steps, null, '\t'))
			should.deepEqual(result.output.steps[1],
				{
					"1": {
						"1": {
							"1": {
								"command": "evoware._facts",
								"agent": "ourlab.mario.evoware",
								"factsEquipment": "Centrifuge",
								"factsVariable": "Centrifuge_MoveToPos",
								"factsValue": "2"
							},
							"2": {
								"command": "evoware._facts",
								"agent": "ourlab.mario.evoware",
								"factsEquipment": "Centrifuge",
								"factsVariable": "Centrifuge_Open"
							},
							"command": "equipment.openSite|ourlab.mario.evoware|ourlab.mario.centrifuge",
							"agent": "ourlab.mario.evoware",
							"equipment": "ourlab.mario.centrifuge",
							"site": "ourlab.mario.site.CENTRIFUGE_2"
						},
						"command": "equipment.openSite",
						"agent": "ourlab.mario.evoware",
						"equipment": "ourlab.mario.centrifuge",
						"site": "ourlab.mario.site.CENTRIFUGE_2"
					},
					"2": {
						"1": {
							"command": "transporter._movePlate",
							"agent": "ourlab.mario.evoware",
							"equipment": "ourlab.mario.roma1",
							"program": "Narrow",
							"object": "plate1",
							"destination": "ourlab.mario.site.REGRIP"
						},
						"2": {
							"command": "transporter._movePlate",
							"agent": "ourlab.mario.evoware",
							"equipment": "ourlab.mario.roma1",
							"program": "Narrow",
							"object": "plate1",
							"destination": "ourlab.mario.site.CENTRIFUGE_2"
						},
						"command": "transporter.movePlate",
						"object": "plate1",
						"destination": "ourlab.mario.site.CENTRIFUGE_2"
					},
					"3": {
						"1": {
							"1": {
								"command": "evoware._facts",
								"agent": "ourlab.mario.evoware",
								"factsEquipment": "Centrifuge",
								"factsVariable": "Centrifuge_MoveToPos",
								"factsValue": "4"
							},
							"2": {
								"command": "evoware._facts",
								"agent": "ourlab.mario.evoware",
								"factsEquipment": "Centrifuge",
								"factsVariable": "Centrifuge_Open"
							},
							"command": "equipment.openSite|ourlab.mario.evoware|ourlab.mario.centrifuge",
							"agent": "ourlab.mario.evoware",
							"equipment": "ourlab.mario.centrifuge",
							"site": "ourlab.mario.site.CENTRIFUGE_4"
						},
						"command": "equipment.openSite",
						"agent": "ourlab.mario.evoware",
						"equipment": "ourlab.mario.centrifuge",
						"site": "ourlab.mario.site.CENTRIFUGE_4"
					},
					"4": {
						"1": {
							"command": "transporter._movePlate",
							"agent": "ourlab.mario.evoware",
							"equipment": "ourlab.mario.roma1",
							"program": "Narrow",
							"object": "plate2",
							"destination": "ourlab.mario.site.REGRIP"
						},
						"2": {
							"command": "transporter._movePlate",
							"agent": "ourlab.mario.evoware",
							"equipment": "ourlab.mario.roma1",
							"program": "Narrow",
							"object": "plate2",
							"destination": "ourlab.mario.site.CENTRIFUGE_4"
						},
						"command": "transporter.movePlate",
						"object": "plate2",
						"destination": "ourlab.mario.site.CENTRIFUGE_4"
					},
					"5": {
						"command": "centrifuge._run",
						"agent": "ourlab.mario.evoware",
						"equipment": "ourlab.mario.centrifuge",
						"program": {
							"rpm": 3000,
							"duration": 120,
							"temperature": 25
						}
					},
					"6": {
						"1": {
							"1": {
								"command": "evoware._facts",
								"agent": "ourlab.mario.evoware",
								"factsEquipment": "Centrifuge",
								"factsVariable": "Centrifuge_MoveToPos",
								"factsValue": "2"
							},
							"2": {
								"command": "evoware._facts",
								"agent": "ourlab.mario.evoware",
								"factsEquipment": "Centrifuge",
								"factsVariable": "Centrifuge_Open"
							},
							"command": "equipment.openSite|ourlab.mario.evoware|ourlab.mario.centrifuge",
							"agent": "ourlab.mario.evoware",
							"equipment": "ourlab.mario.centrifuge",
							"site": "ourlab.mario.site.CENTRIFUGE_2"
						},
						"command": "equipment.openSite",
						"agent": "ourlab.mario.evoware",
						"equipment": "ourlab.mario.centrifuge",
						"site": "ourlab.mario.site.CENTRIFUGE_2"
					},
					"7": {
						"1": {
							"command": "transporter._movePlate",
							"agent": "ourlab.mario.evoware",
							"equipment": "ourlab.mario.roma1",
							"program": "Narrow",
							"object": "plate1",
							"destination": "ourlab.mario.site.REGRIP"
						},
						"2": {
							"command": "transporter._movePlate",
							"agent": "ourlab.mario.evoware",
							"equipment": "ourlab.mario.roma1",
							"program": "Narrow",
							"object": "plate1",
							"destination": "ourlab.mario.site.P2"
						},
						"command": "transporter.movePlate",
						"object": "plate1",
						"destination": "ourlab.mario.site.P2"
					},
					"8": {
						"1": {
							"1": {
								"command": "evoware._facts",
								"agent": "ourlab.mario.evoware",
								"factsEquipment": "Centrifuge",
								"factsVariable": "Centrifuge_MoveToPos",
								"factsValue": "4"
							},
							"2": {
								"command": "evoware._facts",
								"agent": "ourlab.mario.evoware",
								"factsEquipment": "Centrifuge",
								"factsVariable": "Centrifuge_Open"
							},
							"command": "equipment.openSite|ourlab.mario.evoware|ourlab.mario.centrifuge",
							"agent": "ourlab.mario.evoware",
							"equipment": "ourlab.mario.centrifuge",
							"site": "ourlab.mario.site.CENTRIFUGE_4"
						},
						"command": "equipment.openSite",
						"agent": "ourlab.mario.evoware",
						"equipment": "ourlab.mario.centrifuge",
						"site": "ourlab.mario.site.CENTRIFUGE_4"
					},
					"9": {
						"1": {
							"command": "transporter._movePlate",
							"agent": "ourlab.mario.evoware",
							"equipment": "ourlab.mario.roma1",
							"program": "Narrow",
							"object": "plate2",
							"destination": "ourlab.mario.site.REGRIP"
						},
						"2": {
							"command": "transporter._movePlate",
							"agent": "ourlab.mario.evoware",
							"equipment": "ourlab.mario.roma1",
							"program": "Narrow",
							"object": "plate2",
							"destination": "ourlab.mario.site.P3"
						},
						"command": "transporter.movePlate",
						"object": "plate2",
						"destination": "ourlab.mario.site.P3"
					},
					"10": {
						"1": {
							"1": {
								"command": "evoware._facts",
								"agent": "ourlab.mario.evoware",
								"factsEquipment": "Centrifuge",
								"factsVariable": "Centrifuge_Close"
							},
							"command": "equipment.close|ourlab.mario.evoware|ourlab.mario.centrifuge",
							"agent": "ourlab.mario.evoware",
							"equipment": "ourlab.mario.centrifuge"
						},
						"command": "equipment.close",
						"agent": "ourlab.mario.evoware",
						"equipment": "ourlab.mario.centrifuge"
					},
					"command": "centrifuge.centrifuge2",
					"object1": "plate1",
					"object2": "plate2",
					"program": {
						"rpm": 3000,
						"duration": 120,
						"temperature": 25
					}
				}
			);
		});
	});
});
