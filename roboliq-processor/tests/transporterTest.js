var _ = require('lodash');
var should = require('should');
var roboliq = require('../src/roboliq.js')

describe('transporter', function() {
	var protocol0 = {
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

	describe('transporter.movePlate', function () {
		it("should handle movePlate to the plate's current location", function() {
			var protocol = _.merge({}, protocol0, {
				steps: {
					1: {
						"command": "transporter.movePlate",
						"object": "plate1",
						"destination": "ourlab.mario.site.P2"
					}
				}
			});
			var result = roboliq.run([__dirname+"/ourlab.js", "-o", ""], protocol, false);
			//console.log("result:\n"+JSON.stringify(result.output.steps, null, '\t'))
			should.deepEqual(result.output.errors, {});
			should.deepEqual(result.output.steps[1], {
				"command": "transporter.movePlate",
				"object": "plate1",
				"destination": "ourlab.mario.site.P2"
			});
		});

		it('should handle movePlate into the centrifuge', function () {
			var protocol = _.merge({}, protocol0, {
				steps: {
					1: {
						"command": "transporter.movePlate",
						"object": "plate2",
						"destination": "ourlab.mario.site.CENTRIFUGE_4"
					}
				}
			});
			var result = roboliq.run([__dirname+"/ourlab.js", "-o", "", "-T"], protocol, false);
			//console.log("result:\n"+JSON.stringify(result.output.steps, null, '\t'))
			should.deepEqual(result.output.steps[1], {
				"1": {
					"command": "transporter._movePlate",
					"agent": "ourlab.mario.evoware",
					"equipment": "ourlab.mario.roma1",
					"program": "Narrow",
					"object": "plate2",
					"destination": "ourlab.mario.site.REGRIP"
				},
				"2": {
					"1": {
						"1": {
							"agent": "ourlab.mario.evoware",
							"command": "evoware._facts",
							"factsEquipment": "Centrifuge",
							"factsValue": "4",
							"factsVariable": "Centrifuge_MoveToPos"
						},
						"2": {
							"agent": "ourlab.mario.evoware",
							"command": "evoware._facts",
							"factsEquipment": "Centrifuge",
							"factsVariable": "Centrifuge_Open"
						},
						"agent": "ourlab.mario.evoware",
						"command": "equipment.openSite|ourlab.mario.evoware|ourlab.mario.centrifuge",
						"equipment": "ourlab.mario.centrifuge",
						"site": "ourlab.mario.site.CENTRIFUGE_4"
					},
					"command": "equipment.openSite",
					"agent": "ourlab.mario.evoware",
					"equipment": "ourlab.mario.centrifuge",
					"site": "ourlab.mario.site.CENTRIFUGE_4"
				},
				"3": {
					"command": "transporter._movePlate",
					"agent": "ourlab.mario.evoware",
					"equipment": "ourlab.mario.roma1",
					"program": "Narrow",
					"object": "plate2",
					"destination": "ourlab.mario.site.CENTRIFUGE_4"
				},
				"4": {
					"1": {
						"1": {
							"agent": "ourlab.mario.evoware",
							"command": "evoware._facts",
							"factsEquipment": "Centrifuge",
							"factsVariable": "Centrifuge_Close"
						},
						"agent": "ourlab.mario.evoware",
						"command": "equipment.close|ourlab.mario.evoware|ourlab.mario.centrifuge",
						"equipment": "ourlab.mario.centrifuge"
					},
					"command": "equipment.close",
					"agent": "ourlab.mario.evoware",
					"equipment": "ourlab.mario.centrifuge"
				},
				"command": "transporter.movePlate",
				"object": "plate2",
				"destination": "ourlab.mario.site.CENTRIFUGE_4"
			});
		});

		it('should handle movePlate from REGRIP into the centrifuge using roma1', function () {
			var protocol = _.merge({}, protocol0, {
				objects: {
					plate2: { location: "ourlab.mario.site.REGRIP" }
				},
				steps: {
					1: {
						"command": "transporter.movePlate",
						"equipment": "ourlab.mario.roma1",
						"object": "plate2",
						"destination": "ourlab.mario.site.CENTRIFUGE_4"
					}
				}
			});
			var result = roboliq.run([__dirname+"/ourlab.js", "-o", "", "-T"], protocol, false);
			//console.log("result:\n"+JSON.stringify(result.output.steps, null, '\t'))
			should.deepEqual(result.output.steps[1], {
				"1": {
					"1": {
						"1": {
							"agent": "ourlab.mario.evoware",
							"command": "evoware._facts",
							"factsEquipment": "Centrifuge",
							"factsValue": "4",
							"factsVariable": "Centrifuge_MoveToPos"
						},
						"2": {
							"agent": "ourlab.mario.evoware",
							"command": "evoware._facts",
							"factsEquipment": "Centrifuge",
							"factsVariable": "Centrifuge_Open"
						},
						"agent": "ourlab.mario.evoware",
						"command": "equipment.openSite|ourlab.mario.evoware|ourlab.mario.centrifuge",
						"equipment": "ourlab.mario.centrifuge",
						"site": "ourlab.mario.site.CENTRIFUGE_4"
					},
					"command": "equipment.openSite",
					"agent": "ourlab.mario.evoware",
					"equipment": "ourlab.mario.centrifuge",
					"site": "ourlab.mario.site.CENTRIFUGE_4"
				},
				"2": {
					"command": "transporter._movePlate",
					"agent": "ourlab.mario.evoware",
					"equipment": "ourlab.mario.roma1",
					"program": "Narrow",
					"object": "plate2",
					"destination": "ourlab.mario.site.CENTRIFUGE_4"
				},
				"3": {
					"1": {
						"1": {
							"agent": "ourlab.mario.evoware",
							"command": "evoware._facts",
							"factsEquipment": "Centrifuge",
							"factsVariable": "Centrifuge_Close"
						},
						"agent": "ourlab.mario.evoware",
						"command": "equipment.close|ourlab.mario.evoware|ourlab.mario.centrifuge",
						"equipment": "ourlab.mario.centrifuge"
					},
					"command": "equipment.close",
					"agent": "ourlab.mario.evoware",
					"equipment": "ourlab.mario.centrifuge"
				},
				"command": "transporter.movePlate",
				"equipment": "ourlab.mario.roma1",
				"object": "plate2",
				"destination": "ourlab.mario.site.CENTRIFUGE_4"
			});
		});

		it('should handle movePlate into the centrifuge when the destination site is already open', function () {
			var protocol = _.merge({}, protocol0, {
				steps: {
					1: {
						"command": "equipment.openSite",
						"agent": "ourlab.mario.evoware",
						"equipment": "ourlab.mario.centrifuge",
						"site": "ourlab.mario.site.CENTRIFUGE_4"
					},
					2: {
						"command": "transporter.movePlate",
						"object": "plate2",
						"destination": "ourlab.mario.site.CENTRIFUGE_4"
					}
				}
			});
			var result = roboliq.run([__dirname+"/ourlab.js", "-o", "", "-T"], protocol, false);
			//console.log("result:\n"+JSON.stringify(result.output.steps, null, '\t'))
			should.deepEqual(result.output.steps,
				{
					"1": {
						"1": {
							"1": {
								"agent": "ourlab.mario.evoware",
								"command": "evoware._facts",
								"factsEquipment": "Centrifuge",
								"factsValue": "4",
								"factsVariable": "Centrifuge_MoveToPos"
							},
							"2": {
								"agent": "ourlab.mario.evoware",
								"command": "evoware._facts",
								"factsEquipment": "Centrifuge",
								"factsVariable": "Centrifuge_Open"
							},
							"agent": "ourlab.mario.evoware",
							"command": "equipment.openSite|ourlab.mario.evoware|ourlab.mario.centrifuge",
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
					}
				}
			);
		});

		it('should handle two movePlates into the centrifuge', function () {
			var protocol = _.merge({}, protocol0, {
				steps: {
					1: {
						"command": "transporter.movePlate",
						"object": "plate1",
						"destination": "ourlab.mario.site.CENTRIFUGE_2"
					},
					2: {
						"command": "transporter.movePlate",
						"object": "plate2",
						"destination": "ourlab.mario.site.CENTRIFUGE_4"
					}
				}
			});
			var result = roboliq.run([__dirname+"/ourlab.js", "-o", "", "-T"], protocol, false);
			//console.log("result:\n"+JSON.stringify(result.output.steps, null, '\t'))
			should.deepEqual(result.output.steps,
				{
					"1": {
						"1": {
							"command": "transporter._movePlate",
							"agent": "ourlab.mario.evoware",
							"equipment": "ourlab.mario.roma1",
							"program": "Narrow",
							"object": "plate1",
							"destination": "ourlab.mario.site.REGRIP"
						},
						"2": {
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
						"3": {
							"command": "transporter._movePlate",
							"agent": "ourlab.mario.evoware",
							"equipment": "ourlab.mario.roma1",
							"program": "Narrow",
							"object": "plate1",
							"destination": "ourlab.mario.site.CENTRIFUGE_2"
						},
						"4": {
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
						"command": "transporter.movePlate",
						"object": "plate1",
						"destination": "ourlab.mario.site.CENTRIFUGE_2"
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
						"3": {
							"command": "transporter._movePlate",
							"agent": "ourlab.mario.evoware",
							"equipment": "ourlab.mario.roma1",
							"program": "Narrow",
							"object": "plate2",
							"destination": "ourlab.mario.site.CENTRIFUGE_4"
						},
						"4": {
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
						"command": "transporter.movePlate",
						"object": "plate2",
						"destination": "ourlab.mario.site.CENTRIFUGE_4"
					}
				}
			);
		});

	});

	describe('transporter.doThenRestoreLocation', function () {
		it("should handle doThenRestoreLocation", function() {
			var protocol = _.merge({}, protocol0, {
				steps: {
					1: {
						"command": "transporter.doThenRestoreLocation",
						"objects": ["plate1"],
						steps: {
							1: {
								description: "`A simple description`",
								"command": "transporter.movePlate",
								"object": "plate1",
								"destination": "ourlab.mario.site.P4"
							}
						}
					}
				}
			});
			var result = roboliq.run([__dirname+"/ourlab.js", "-o", ""], protocol, false);
			//console.log("result:\n"+JSON.stringify(result.output.steps, null, '\t'))
			should.deepEqual(result.output.errors, {});
			should.deepEqual(result.output.steps[1], {
				"command": "transporter.doThenRestoreLocation",
				"objects": ["plate1"],
				steps: {
					1: {
						description: "`A simple description`",
						"command": "transporter.movePlate",
						"object": "plate1",
						"destination": "ourlab.mario.site.P4"
					}
				},
				1: {
					description: "A simple description",
					"command": "transporter.movePlate",
					"object": "plate1",
					"destination": "ourlab.mario.site.P4",
					1: {
						"command": "transporter._movePlate",
						"agent": "ourlab.mario.evoware",
						"equipment": "ourlab.mario.roma1",
						"program": "Narrow",
						"object": "plate1",
						"destination": "ourlab.mario.site.P4"
					}
				},
				2: {
					"command": "transporter.movePlate",
					"object": "plate1",
					"destination": "ourlab.mario.site.P2",
					1: {
						"command": "transporter._movePlate",
						"agent": "ourlab.mario.evoware",
						"equipment": "ourlab.mario.roma1",
						"program": "Narrow",
						"object": "plate1",
						"destination": "ourlab.mario.site.P2"
					}
				}
			});
		});
	});

	describe('transporter._moveLidFromContainerToSite', function () {
		it("should handle _moveLidFromContainerToSite from plate to empty site", function() {
			var protocol = _.merge({}, protocol0, {
				objects: {
					plate1: {
						hasLid: true
					},
					plate1Lid: {
						type: "Lid",
						location: "plate1"
					}
				},
				steps: {
					1: {
						"command": "transporter._moveLidFromContainerToSite",
						agent: "ourlab.mario.evoware",
						equipment: "ourlab.mario.roma1",
						program: "Narrow",
						"object": "plate1Lid",
						"container": "plate1",
						"destination": "ourlab.mario.site.P3"
					}
				}
			});
			var result = roboliq.run([__dirname+"/ourlab.js", "-o", ""], protocol, false);
			//console.log("result:\n"+JSON.stringify(result.output.steps, null, '\t'))
			should.deepEqual(result.output.errors, {});
			should.deepEqual(result.output.effects, {
				1: {
					"plate1.hasLid": false,
					"plate1Lid.location": "ourlab.mario.site.P3"
				}
			});
		});
	});

	describe('transporter.moveLidFromContainerToSite', function () {
		it.only("should handle moveLidFromContainerToSite from plate to empty site", function() {
			var protocol = _.merge({}, protocol0, {
				objects: {
					plate1: {
						hasLid: true
					},
					plate1Lid: {
						type: "Lid",
						model: "ourlab.model.lidModel_384_square",
						location: "plate1",
					}
				},
				steps: {
					1: {
						"command": "transporter.moveLidFromContainerToSite",
						"object": "plate1Lid",
						"container": "plate1",
						"destination": "ourlab.mario.site.P3"
					}
				}
			});
			var result = roboliq.run([__dirname+"/ourlab.js", "-o", ""], protocol, false);
			//console.log("result:\n"+JSON.stringify(result.output.steps, null, '\t'))
			should.deepEqual(result.output.errors, {});
			should.deepEqual(result.output.effects, {
				1: {
					"plate1.hasLid": false,
					"plate1Lid.location": "ourlab.mario.site.P3"
				}
			});
		});
	});
});
