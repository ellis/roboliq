var _ = require('lodash');
var should = require('should');
var roboliq = require('../src/roboliq.js')

describe('pipetter', function() {
	describe('pipetter.pipette', function () {

		it('should pipette between two wells on plate1 without specifying well contents', function () {
			var protocol = {
				roboliq: "v1",
				objects: {
					plate1: {
						type: "Plate",
						model: "ourlab.model.plateModel_96_square_transparent_nunc",
						location: "ourlab.mario.site.P2"
					}
				},
				steps: {
					"1": {
						command: "pipetter.pipette",
						items: [{
							source: "plate1(A01)",
							destination: "plate1(A02)",
							volume: "20ul"
						}]
					}
				}
			};
			var result = roboliq.run(["-o", ""], protocol);
			should.deepEqual(result.output.errors, {});
			should.deepEqual(result.output.warnings, {});
			should.deepEqual(result.output.steps[1][1][1], {
				"command": "pipetter._cleanTips",
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
				"command": "pipetter._pipette",
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
				"command": "pipetter._cleanTips",
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
						"Infinity l",
						"plate1(A01)"
					],
					"plate1.contents.A02": [
						"20 ul",
						"plate1(A01)"
					],
					'__WELLS__.plate1.contents.A01': {
						isSource: true,
						volumeMax: 'Infinity l',
						volumeMin: 'Infinity l',
						volumeRemoved: '20 ul'
					},
					'__WELLS__.plate1.contents.A02': {
						isSource: false,
						volumeMax: '20 ul',
						volumeMin: '0 l',
						volumeAdded: '20 ul'
					}
				}
			});
			should.deepEqual(result.output.tables.labware, [
				{
					labware: 'plate1',
					locationFinal: 'ourlab.mario.site.P2',
					locationInitial: 'ourlab.mario.site.P2',
					model: 'ourlab.model.plateModel_96_square_transparent_nunc',
					type: 'Plate'
				}
			]);
			should.deepEqual(result.output.tables.sourceWells, [
				{
					source: 'plate1(A01)',
					volumeFinal: 'Infinity l',
					volumeInitial: '0ul',
					volumeRemoved: '20 ul',
					well: 'plate1(A01)'
				}
			]);
			should.deepEqual(result.output.tables.wellContentsFinal, [
				{
					well: 'plate1(A01)',
					'plate1(A01)': 'Infinity l'
				},
				{
					well: 'plate1(A02)',
					'plate1(A01)': '20 ul'
				},
			]);
		})




		it('should pipette from system liquid source', function () {
			var protocol = {
				roboliq: "v1",
				objects: {
					plate1: {
						type: "Plate",
						model: "ourlab.model.plateModel_96_square_transparent_nunc",
						location: "ourlab.mario.site.P2"
					}
				},
				steps: {
					"1": {
						command: "pipetter.pipette",
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
				"command": "pipetter._pipette",
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
				"command": "pipetter._pipette",
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
					"__WELLS__.plate1.contents.A01": {
						"isSource": false,
						"volumeMin": "0 l",
						"volumeMax": "10 ul",
						"volumeAdded": "10 ul"
					},
					"plate1.contents.B01": [
						"10 ul",
						"systemLiquid"
					],
					"__WELLS__.plate1.contents.B01": {
						"isSource": false,
						"volumeMin": "0 l",
						"volumeMax": "10 ul",
						"volumeAdded": "10 ul"
					},
					"plate1.contents.C01": [
						"10 ul",
						"systemLiquid"
					],
					"__WELLS__.plate1.contents.C01": {
						"isSource": false,
						"volumeMin": "0 l",
						"volumeMax": "10 ul",
						"volumeAdded": "10 ul"
					},
					"plate1.contents.D01": [
						"10 ul",
						"systemLiquid"
					],
					"__WELLS__.plate1.contents.D01": {
						"isSource": false,
						"volumeMin": "0 l",
						"volumeMax": "10 ul",
						"volumeAdded": "10 ul"
					}
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
					"__WELLS__.plate1.contents.E01": {
						"isSource": false,
						"volumeMin": "0 l",
						"volumeMax": "10 ul",
						"volumeAdded": "10 ul"
					},
					"plate1.contents.F01": [
						"10 ul",
						"systemLiquid"
					],
					"__WELLS__.plate1.contents.F01": {
						"isSource": false,
						"volumeMin": "0 l",
						"volumeMax": "10 ul",
						"volumeAdded": "10 ul"
					},
					"plate1.contents.G01": [
						"10 ul",
						"systemLiquid"
					],
					"__WELLS__.plate1.contents.G01": {
						"isSource": false,
						"volumeMin": "0 l",
						"volumeMax": "10 ul",
						"volumeAdded": "10 ul"
					},
					"plate1.contents.H01": [
						"10 ul",
						"systemLiquid"
					],
					"__WELLS__.plate1.contents.H01": {
						"isSource": false,
						"volumeMin": "0 l",
						"volumeMax": "10 ul",
						"volumeAdded": "10 ul"
					}
				}
			});
		})



		it('should pipette from a multi-well source', function () {
			var protocol = {
				roboliq: "v1",
				objects: {
					trough1: {
						type: "Plate",
						model: "ourlab.model.troughModel_100ml",
						location: "ourlab.mario.site.R6",
						//contents: ['Infinity l', 'saltwater']
					},
					plate1: {
						type: "Plate",
						model: "ourlab.model.plateModel_96_square_transparent_nunc",
						location: "ourlab.mario.site.P2"
					},
					source1: {
						type: "Liquid",
						wells: "trough1(A01 down to H01)"
					}
				},
				steps: {
					"1": {
						command: "pipetter.pipette",
						clean: "none",
						sources: "source1",
						destinations: "plate1(A02 down to D02)",
						volumes: "20ul"
					}
				}
			};
			var result = roboliq.run(["-o", ""], protocol);
			should.deepEqual(result.output.steps[1][1], {
				"command": "pipetter._pipette",
				"agent": "ourlab.mario.evoware",
				"equipment": "ourlab.mario.liha",
				"program": "\"Roboliq_Water_Dry_1000\"",
				"items": [
					{
						"syringe": 1,
						"source": "trough1(A01)",
						"destination": "plate1(A02)",
						"volume": "20ul"
					},
					{
						"syringe": 2,
						"source": "trough1(B01)",
						"destination": "plate1(B02)",
						"volume": "20ul"
					},
					{
						"syringe": 3,
						"source": "trough1(C01)",
						"destination": "plate1(C02)",
						"volume": "20ul"
					},
					{
						"syringe": 4,
						"source": "trough1(D01)",
						"destination": "plate1(D02)",
						"volume": "20ul"
					},
				]
			});
		})
	});




	describe('pipetter.pipetteMixtures', function () {
		it("should pipette mixtures to destination wells", function() {
			var protocol = {
				roboliq: "v1",
				objects: {
					plate1: {
						type: "Plate",
						model: "ourlab.model.plateModel_96_square_transparent_nunc",
						location: "ourlab.mario.site.P2",
						contents: {
							A01: ['100ul', 'source1'],
							B01: ['100ul', 'source2']
						}
					},
					source1: {
						type: 'Liquid',
						wells: 'plate1(A01)'
					},
					source2: {
						type: 'Liquid',
						wells: 'plate1(B01)'
					},
				},
				steps: {
					"1": {
						command: "pipetter.pipetteMixtures",
						clean: 'none',
						mixtures: [
							[{source: 'source1', volume: '10ul'}, {source: 'source2', volume: '10ul'}],
							[{source: 'source1', volume: '10ul'}, {source: 'source2', volume: '20ul'}],
						],
						destinations: "plate1(A02 down to D02)"
					}
				}
			};
			var result = roboliq.run(["-o", ""], protocol);
			should.deepEqual(_.omit(result.output.steps[1][1], "1"), {
				command: "pipetter.pipette",
				clean: 'none',
				items: [
					{source: 'source1', volume: '10ul', destination: 'plate1(A02)'},
					{source: 'source1', volume: '10ul', destination: 'plate1(B02)'},
					{source: 'source2', volume: '10ul', destination: 'plate1(A02)'},
					{source: 'source2', volume: '20ul', destination: 'plate1(B02)'},
				]
			});

			protocol.steps["1"].order = "destination";
			result = roboliq.run(["-o", ""], protocol);
			should.deepEqual(_.omit(result.output.steps[1][1], "1"), {
				command: "pipetter.pipette",
				clean: 'none',
				items: [
					{source: 'source1', volume: '10ul', destination: 'plate1(A02)'},
					{source: 'source2', volume: '10ul', destination: 'plate1(A02)'},
					{source: 'source1', volume: '10ul', destination: 'plate1(B02)'},
					{source: 'source2', volume: '20ul', destination: 'plate1(B02)'},
				]
			});
			//console.log(JSON.stringify(result.output.tables.wellContentsFinal, null, '\t'));
			should.deepEqual(result.output.tables.wellContentsFinal,
				[
					{"well": "plate1(A01)", "source1": "80 ul"},
					{"well": "plate1(B01)", "source2": "70 ul"},
					{"well": "plate1(A02)", "source1": "10 ul", "source2": "10 ul"},
					{"well": "plate1(B02)", "source1": "10 ul", "source2": "20 ul"}
				]
			)
		});
	});
});
