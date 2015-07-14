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
						"NaN l",
						"plate1(A01)"
					],
					"plate1.contents.A02": [
						"20 ul",
						"plate1(A01)"
					],
					'__WELLS__.plate1.contents.A01': {
						isSource: true,
						volumeMax: 'NaN l',
						volumeMin: 'NaN l',
						volumeRemoved: '20 ul'
					}
				}
			});
		})




		it('should pipette from system liquid source', function () {
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
						sources: "ourlab.mario.systemLiquid",
						destinations: "plate1(A01 down 8)",
						volumes: "10ul",
						clean: "none"
					}
				}
			};
			var result = roboliq.run(["-o", ""], protocol);
			//console.log(JSON.stringify(result, null, '\t'));
			should.deepEqual(result.output.steps[1][1], {
				"command": "pipetter.instruction.pipette",
				"agent": "ourlab.mario.evoware",
				"equipment": "ourlab.mario.liha",
				"program": "\"Roboliq_Water_Dry_1000\"",
				"items": [
					{
						"syringe": 1,
						"source": "ourlab.mario.systemLiquidLabware(A01)",
						"destination": "plate1(A01)",
						"volume": "10ul"
					},
					{
						"syringe": 2,
						"source": "ourlab.mario.systemLiquidLabware(B01)",
						"destination": "plate1(B01)",
						"volume": "10ul"
					},
					{
						"syringe": 3,
						"source": "ourlab.mario.systemLiquidLabware(C01)",
						"destination": "plate1(C01)",
						"volume": "10ul"
					},
					{
						"syringe": 4,
						"source": "ourlab.mario.systemLiquidLabware(D01)",
						"destination": "plate1(D01)",
						"volume": "10ul"
					}
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
						"source": "ourlab.mario.systemLiquidLabware(A01)",
						"destination": "plate1(E01)",
						"volume": "10ul"
					},
					{
						"syringe": 2,
						"source": "ourlab.mario.systemLiquidLabware(B01)",
						"destination": "plate1(F01)",
						"volume": "10ul"
					},
					{
						"syringe": 3,
						"source": "ourlab.mario.systemLiquidLabware(C01)",
						"destination": "plate1(G01)",
						"volume": "10ul"
					},
					{
						"syringe": 4,
						"source": "ourlab.mario.systemLiquidLabware(D01)",
						"destination": "plate1(H01)",
						"volume": "10ul"
					}
				]
			});
			should.deepEqual(result.output.effects, {
				"1.1": {
					"ourlab.mario.systemLiquidLabware.contents": [
						"Infinity l",
						"systemLiquid"
					],
					"plate1.contents.A01": [
						"10 ul",
						"systemLiquid"
					],
					"__WELLS__.ourlab.mario.systemLiquidLabware.contents": {
						"isSource": true,
						"volumeMin": "Infinity l",
						"volumeMax": "Infinity l",
						"volumeRemoved": "40 ul"
					},
					"plate1.contents.B01": [
						"10 ul",
						"systemLiquid"
					],
					"plate1.contents.C01": [
						"10 ul",
						"systemLiquid"
					],
					"plate1.contents.D01": [
						"10 ul",
						"systemLiquid"
					]
				},
				"1.2": {
					"ourlab.mario.systemLiquidLabware.contents": [
						"Infinity l",
						"systemLiquid"
					],
					"plate1.contents.E01": [
						"10 ul",
						"systemLiquid"
					],
					"__WELLS__.ourlab.mario.systemLiquidLabware.contents": {
						"isSource": true,
						"volumeMin": "Infinity l",
						"volumeMax": "Infinity l",
						"volumeRemoved": "80 ul"
					},
					"plate1.contents.F01": [
						"10 ul",
						"systemLiquid"
					],
					"plate1.contents.G01": [
						"10 ul",
						"systemLiquid"
					],
					"plate1.contents.H01": [
						"10 ul",
						"systemLiquid"
					]
				}
			});
		})
	})
})
