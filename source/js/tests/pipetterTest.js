var roboliq = require('../roboliq.js')
var should = require('should');

describe('pipetter', function() {
	describe('pipetter.action.pipette', function () {

		it('should pipette between two wells on plate1 without specify well contents', function () {
			var protocol = {
				objects: {
				  plate1: {
				    type: "Plate",
				    model: "ourlab.model.plateModel_96_square_transparent_nunc",
				    location: "ourlab.mario.site.P2"
				  }
				},
				steps: {
				  "1": {
				    command: "pipetter.action.pipette",
				    items: [{
				      source: "plate1(A01)",
				      destination: "plate1(A02)",
				      volume: "20ul"
					}]
				  }
			    }
			};
			var result = roboliq.run(["-o", ""], protocol);
			should.deepEqual(result.output.steps[1][1][1], {
				"command": "pipetter.instruction.cleanTips",
				"agent": "ourlab.mario.evoware",
				"equipment": "ourlab.mario.liha",
				"program": "ourlab.mario.washProgram.thorough_1000",
				"syringes": [
					1,
					2,
					3,
					4
				]
			});
			should.deepEqual(result.output.steps[1][2], {
				"command": "pipetter.instruction.pipette",
				"agent": "ourlab.mario.evoware",
				"equipment": "ourlab.mario.liha",
				"program": "\"Roboliq_Water_Dry_1000\"",
				"items": [
					{
						"syringe": 1,
						"source": "plate1(A01)",
						"destination": "plate1(A02)",
						"volume": "20ul"
					}
				]
			});
			should.deepEqual(result.output.steps[1][3][1], {
				"command": "pipetter.instruction.cleanTips",
				"agent": "ourlab.mario.evoware",
				"equipment": "ourlab.mario.liha",
				"program": "ourlab.mario.washProgram.thorough_1000",
				"syringes": [
					1,
					2,
					3,
					4
				]
			});
			should.deepEqual(result.output.effects, {
				"1.2": {
					"plate1.contents.A01": [
						"-20 ul",
						"plate1(A01)"
					],
					"plate1.contents.A02": [
						"20 ul",
						"plate1(A01)"
					]
				}
			});
		})
	})
})
