var _ = require('lodash');
var should = require('should');
var roboliq = require('../roboliq.js')

describe('transporter', function() {
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

	describe('transporter.movePlate', function () {
		it('should handle movePlate into the centrifuge', function () {
			var protocol = _.merge({}, protocol0, {
				steps: {
					1: {
						"command": "transporter.action.movePlate",
						"object": "plate2",
						"destination": "ourlab.mario.site.CENTRIFUGE_4"
					}
				}
			});
			var result = roboliq.run(["-o", "", "-T"], protocol);
			//console.log("result:\n"+JSON.stringify(result.output.steps, null, '\t'))
			should.deepEqual(result.output.steps[1], {
				"1": {
					"command": "transporter.instruction.movePlate",
					"agent": "ourlab.mario.evoware",
					"equipment": "ourlab.mario.roma1",
					"program": "Narrow",
					"object": "plate2",
					"destination": "ourlab.mario.site.REGRIP"
				},
				"2": {
					"command": "centrifuge.instruction.openSite",
					"agent": "ourlab.mario.evoware",
					"equipment": "ourlab.mario.centrifuge",
					"site": "ourlab.mario.site.CENTRIFUGE_4"
				},
				"3": {
					"command": "transporter.instruction.movePlate",
					"agent": "ourlab.mario.evoware",
					"equipment": "ourlab.mario.roma1",
					"program": "Narrow",
					"object": "plate2",
					"destination": "ourlab.mario.site.CENTRIFUGE_4"
				},
				"4": {
					"command": "centrifuge.instruction.close",
					"agent": "ourlab.mario.evoware",
					"equipment": "ourlab.mario.centrifuge"
				},
				"command": "transporter.action.movePlate",
				"object": "plate2",
				"destination": "ourlab.mario.site.CENTRIFUGE_4"
			});
		});

		it('should handle movePlate into the centrifuge when the destination site is already open', function () {
			var protocol = _.merge({}, protocol0, {
				steps: {
					1: {
						"command": "centrifuge.instruction.openSite",
						"agent": "ourlab.mario.evoware",
						"equipment": "ourlab.mario.centrifuge",
						"site": "ourlab.mario.site.CENTRIFUGE_4"
					},
					2: {
						"command": "transporter.action.movePlate",
						"object": "plate2",
						"destination": "ourlab.mario.site.CENTRIFUGE_4"
					}
				}
			});
			var result = roboliq.run(["-o", "", "-T"], protocol);
			//console.log("result:\n"+JSON.stringify(result.output.steps, null, '\t'))
			should.deepEqual(result.output.steps,
				{
					"1": {
						"command": "centrifuge.instruction.openSite",
						"agent": "ourlab.mario.evoware",
						"equipment": "ourlab.mario.centrifuge",
						"site": "ourlab.mario.site.CENTRIFUGE_4"
					},
					"2": {
						"1": {
							"command": "transporter.instruction.movePlate",
							"agent": "ourlab.mario.evoware",
							"equipment": "ourlab.mario.roma1",
							"program": "Narrow",
							"object": "plate2",
							"destination": "ourlab.mario.site.REGRIP"
						},
						"2": {
							"command": "transporter.instruction.movePlate",
							"agent": "ourlab.mario.evoware",
							"equipment": "ourlab.mario.roma1",
							"program": "Narrow",
							"object": "plate2",
							"destination": "ourlab.mario.site.CENTRIFUGE_4"
						},
						"command": "transporter.action.movePlate",
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
						"command": "transporter.action.movePlate",
						"object": "plate1",
						"destination": "ourlab.mario.site.CENTRIFUGE_2"
					},
					2: {
						"command": "transporter.action.movePlate",
						"object": "plate2",
						"destination": "ourlab.mario.site.CENTRIFUGE_4"
					}
				}
			});
			var result = roboliq.run(["-o", "", "-T"], protocol);
			//console.log("result:\n"+JSON.stringify(result.output.steps, null, '\t'))
			should.deepEqual(result.output.steps,
				{
					"1": {
						"1": {
							"command": "transporter.instruction.movePlate",
							"agent": "ourlab.mario.evoware",
							"equipment": "ourlab.mario.roma1",
							"program": "Narrow",
							"object": "plate1",
							"destination": "ourlab.mario.site.REGRIP"
						},
						"2": {
							"command": "centrifuge.instruction.openSite",
							"agent": "ourlab.mario.evoware",
							"equipment": "ourlab.mario.centrifuge",
							"site": "ourlab.mario.site.CENTRIFUGE_2"
						},
						"3": {
							"command": "transporter.instruction.movePlate",
							"agent": "ourlab.mario.evoware",
							"equipment": "ourlab.mario.roma1",
							"program": "Narrow",
							"object": "plate1",
							"destination": "ourlab.mario.site.CENTRIFUGE_2"
						},
						"4": {
							"command": "centrifuge.instruction.close",
							"agent": "ourlab.mario.evoware",
							"equipment": "ourlab.mario.centrifuge"
						},
						"command": "transporter.action.movePlate",
						"object": "plate1",
						"destination": "ourlab.mario.site.CENTRIFUGE_2"
					},
					"2": {
						"1": {
							"command": "transporter.instruction.movePlate",
							"agent": "ourlab.mario.evoware",
							"equipment": "ourlab.mario.roma1",
							"program": "Narrow",
							"object": "plate2",
							"destination": "ourlab.mario.site.REGRIP"
						},
						"2": {
							"command": "centrifuge.instruction.openSite",
							"agent": "ourlab.mario.evoware",
							"equipment": "ourlab.mario.centrifuge",
							"site": "ourlab.mario.site.CENTRIFUGE_4"
						},
						"3": {
							"command": "transporter.instruction.movePlate",
							"agent": "ourlab.mario.evoware",
							"equipment": "ourlab.mario.roma1",
							"program": "Narrow",
							"object": "plate2",
							"destination": "ourlab.mario.site.CENTRIFUGE_4"
						},
						"4": {
							"command": "centrifuge.instruction.close",
							"agent": "ourlab.mario.evoware",
							"equipment": "ourlab.mario.centrifuge"
						},
						"command": "transporter.action.movePlate",
						"object": "plate2",
						"destination": "ourlab.mario.site.CENTRIFUGE_4"
					}
				}
			);
		});

		/*it('should handle previously buggy plate move operation #1', function () {
			var protocol = _.merge({}, protocol0, {
				steps: {
					"1": {
						"command": "centrifuge.instruction.openSite",
						"agent": "ourlab.mario.evoware",
						"equipment": "ourlab.mario.centrifuge",
						"site": "ourlab.mario.site.CENTRIFUGE_2"
					},
					"2": {
						"command": "transporter.action.movePlate",
						"object": "plate2",
						"destination": "ourlab.mario.site.CENTRIFUGE_2"
					},
					"3": {
						"command": "centrifuge.instruction.openSite",
						"agent": "ourlab.mario.evoware",
						"equipment": "ourlab.mario.centrifuge",
						"site": "ourlab.mario.site.CENTRIFUGE_4"
					},
					"4": {
						"command": "transporter.action.movePlate",
						"object": "plate2",
						"destination": "ourlab.mario.site.CENTRIFUGE_4"
					},
				}
			});
			var result = roboliq.run(["-o", "", "-T"], protocol);
			console.log("result:\n"+JSON.stringify(result.output.steps, null, '\t'))
			console.log("effects:\n"+JSON.stringify(result.output.effects, null, '\t'))
			should.deepEqual(result.output.steps[4],
				{}
			);
		});*/

	});
});
