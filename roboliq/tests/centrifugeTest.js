var _ = require('lodash');
var should = require('should');
var roboliq = require('../src/roboliq.js')

const protocol0 = {
	roboliq: "v1",
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

describe('commands/centrifgue', function() {

	describe('centrifuge.centrifuge2', function () {
		it('should put two plates in the centrifuge, run, then move the plates back', function () {
			var protocol = _.merge({}, protocol0, {
				roboliq: "v1",
				steps: {
					1: {
						command: "centrifuge.centrifuge2",
						//command: "centrifuge.insertPlates2",
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
						"1": {
							"agent": "ourlab.mario.evoware",
							"command": "evoware._facts",
							"factsEquipment": "Centrifuge",
							"factsValue": "3000,120,9,9,25",
							"factsVariable": "Centrifuge_Execute1"
						},
						"command": "equipment.run|ourlab.mario.evoware|ourlab.mario.centrifuge",
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

	describe.only('centrifuge.insertPlates2', function () {
		it('should put plate1 in the centrifuge', function () {
			var protocol = _.merge({}, protocol0, {
				roboliq: "v1",
				steps: {
					1: {
						command: "centrifuge.insertPlates2",
						object1: "plate1"
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
					"command": "centrifuge.insertPlates2",
					"object1": "plate1"
				}
			);
		});
		it('should put plate2 in the centrifuge', function () {
			var protocol = _.merge({}, protocol0, {
				roboliq: "v1",
				steps: {
					1: {
						command: "centrifuge.insertPlates2",
						object2: "plate2"
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
					"2": {
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
					"command": "centrifuge.insertPlates2",
					"object2": "plate2"
				}
			);
		});
		it('should put both plates in the centrifuge', function () {
			var protocol = _.merge({}, protocol0, {
				roboliq: "v1",
				steps: {
					1: {
						command: "centrifuge.insertPlates2",
						object1: "plate1",
						object2: "plate2"
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
					"command": "centrifuge.insertPlates2",
					"object1": "plate1",
					"object2": "plate2"
				}
			);
		});
	});
});
