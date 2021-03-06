/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017, ETH Zurich, Ellis Whitehead
 * @license GPL-3.0
 */

const _ = require('lodash');
const should = require('should');
const roboliq = require('../src/roboliq.js')

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
			var result = roboliq.run([__dirname+"/ourlab.js", "-o", "", "-T"], protocol, false);
			//console.log(JSON.stringify(result.output.steps, null, '\t'));
			//console.log(JSON.stringify(result.output.effects, null, '\t'));
			should.deepEqual(result.output.errors, {});
			should.deepEqual(result.output.warnings, {});
			should.deepEqual(result.output.steps[1][1][1], {
				"1": {
					"command": "pipetter._washTips",
					"agent": "ourlab.mario.evoware",
					"equipment": "ourlab.mario.liha",
					"program": "ourlab.mario.washProgram.thorough_1000",
					"intensity": "thorough",
					"syringes": [
						"ourlab.mario.liha.syringe.1",
						"ourlab.mario.liha.syringe.2",
						"ourlab.mario.liha.syringe.3",
						"ourlab.mario.liha.syringe.4"
					]
				},
				"command": "pipetter.cleanTips|ourlab.mario.evoware|ourlab.mario.liha",
				"agent": "ourlab.mario.evoware",
				"equipment": "ourlab.mario.liha",
				"items": [
					{
						"syringe": "ourlab.mario.liha.syringe.1",
						"intensity": "thorough"
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
						"syringe": "ourlab.mario.liha.syringe.1",
						"source": "plate1(A01)",
						"destination": "plate1(A02)",
						"volume": "20 ul"
					}
				]
			});
			should.deepEqual(result.output.steps[1][3][1], {
				"1": {
					"command": "pipetter._washTips",
					"agent": "ourlab.mario.evoware",
					"equipment": "ourlab.mario.liha",
					"program": "ourlab.mario.washProgram.thorough_1000",
					"intensity": "thorough",
					"syringes": [
						"ourlab.mario.liha.syringe.1",
						"ourlab.mario.liha.syringe.2",
						"ourlab.mario.liha.syringe.3",
						"ourlab.mario.liha.syringe.4"
					]
				},
				"command": "pipetter.cleanTips|ourlab.mario.evoware|ourlab.mario.liha",
				"agent": "ourlab.mario.evoware",
				"equipment": "ourlab.mario.liha",
				"items": [
					{
						"syringe": "ourlab.mario.liha.syringe.1",
						"intensity": "thorough"
					}
				]
			});
			should.deepEqual(result.output.effects, {
				"1.1.1.1": {
					"ourlab.mario.liha.syringe.1.cleaned": "thorough",
					"ourlab.mario.liha.syringe.2.cleaned": "thorough",
					"ourlab.mario.liha.syringe.3.cleaned": "thorough",
					"ourlab.mario.liha.syringe.4.cleaned": "thorough"
				},
				"1.2": {
					"ourlab.mario.liha.syringe.1.contaminants": [
						"plate1(A01)"
					],
					"ourlab.mario.liha.syringe.1.cleaned": null,
					"plate1.contents.A01": [
						"Infinity l",
						"plate1(A01)"
					],
					"plate1.contents.A02": [
						"20 ul",
						"plate1(A01)"
					],
					"__WELLS__.plate1.contents.A01": {
						"isSource": true,
						"volumeMin": "Infinity l",
						"volumeMax": "Infinity l",
						"volumeRemoved": "20 ul"
					},
					"__WELLS__.plate1.contents.A02": {
						"isSource": false,
						"volumeMin": "0 l",
						"volumeMax": "20 ul",
						"volumeAdded": "20 ul"
					}
				},
				"1.3.1.1": {
					"ourlab.mario.liha.syringe.1.contaminants": null,
					"ourlab.mario.liha.syringe.1.cleaned": "thorough"
				}
			});
			should.deepEqual(result.output.tables.labware, [
				{
					labware: 'ourlab.mario.systemLiquidLabware',
					locationFinal: 'ourlab.mario.site.SYSTEM',
					locationInitial: 'ourlab.mario.site.SYSTEM',
					model: 'ourlab.mario.systemLiquidLabwareModel',
					type: 'Plate'
				},
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
					systemLiquid: 'Infinity l',
					well: 'ourlab.mario.systemLiquidLabware'
				},
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
			var result = roboliq.run([__dirname+"/ourlab.js", "-o", ""], protocol, false);
			//console.log(JSON.stringify(result.output.steps, null, '\t'));
			should.deepEqual(result.output.steps[1][1], {
				"command": "pipetter._pipette",
				"agent": "ourlab.mario.evoware",
				"equipment": "ourlab.mario.liha",
				"program": "\"Roboliq_Water_Dry_1000\"",
				"items": [
					{
						"syringe": "ourlab.mario.liha.syringe.1",
						"source": "ourlab.mario.systemLiquidLabware(A01)",
						"destination": "plate1(A01)",
						"volume": "10 ul"
					},
					{
						"syringe": "ourlab.mario.liha.syringe.2",
						"source": "ourlab.mario.systemLiquidLabware(B01)",
						"destination": "plate1(B01)",
						"volume": "10 ul"
					},
					{
						"syringe": "ourlab.mario.liha.syringe.3",
						"source": "ourlab.mario.systemLiquidLabware(C01)",
						"destination": "plate1(C01)",
						"volume": "10 ul"
					},
					{
						"syringe": "ourlab.mario.liha.syringe.4",
						"source": "ourlab.mario.systemLiquidLabware(D01)",
						"destination": "plate1(D01)",
						"volume": "10 ul"
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
						"syringe": "ourlab.mario.liha.syringe.1",
						"source": "ourlab.mario.systemLiquidLabware(A01)",
						"destination": "plate1(E01)",
						"volume": "10 ul"
					},
					{
						"syringe": "ourlab.mario.liha.syringe.2",
						"source": "ourlab.mario.systemLiquidLabware(B01)",
						"destination": "plate1(F01)",
						"volume": "10 ul"
					},
					{
						"syringe": "ourlab.mario.liha.syringe.3",
						"source": "ourlab.mario.systemLiquidLabware(C01)",
						"destination": "plate1(G01)",
						"volume": "10 ul"
					},
					{
						"syringe": "ourlab.mario.liha.syringe.4",
						"source": "ourlab.mario.systemLiquidLabware(D01)",
						"destination": "plate1(H01)",
						"volume": "10 ul"
					}
				]
			});
			should.deepEqual(result.output.effects, {
				"1.1": {
					"ourlab.mario.systemLiquidLabware.contents": [
						"Infinity l",
						"systemLiquid"
					],
					"ourlab.mario.liha.syringe.1.contaminants": ["systemLiquid"],
					"ourlab.mario.liha.syringe.2.contaminants": ["systemLiquid"],
					"ourlab.mario.liha.syringe.3.contaminants": ["systemLiquid"],
					"ourlab.mario.liha.syringe.4.contaminants": ["systemLiquid"],
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
		});



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
			var result = roboliq.run([__dirname+"/ourlab.js", "-o", ""], protocol, false);
			should.deepEqual(result.output.steps[1][1], {
				"command": "pipetter._pipette",
				"agent": "ourlab.mario.evoware",
				"equipment": "ourlab.mario.liha",
				"program": "\"Roboliq_Water_Dry_1000\"",
				"items": [
					{
						"syringe": "ourlab.mario.liha.syringe.1",
						"source": "trough1(A01)",
						"destination": "plate1(A02)",
						"volume": "20 ul"
					},
					{
						"syringe": "ourlab.mario.liha.syringe.2",
						"source": "trough1(B01)",
						"destination": "plate1(B02)",
						"volume": "20 ul"
					},
					{
						"syringe": "ourlab.mario.liha.syringe.3",
						"source": "trough1(C01)",
						"destination": "plate1(C02)",
						"volume": "20 ul"
					},
					{
						"syringe": "ourlab.mario.liha.syringe.4",
						"source": "trough1(D01)",
						"destination": "plate1(D02)",
						"volume": "20 ul"
					},
				]
			});
		})



		it('should pipette from a variable that refers to the system liquid source', function () {
			var protocol = {
				roboliq: "v1",
				objects: {
					plate1: {
						type: "Plate",
						model: "ourlab.model.plateModel_96_square_transparent_nunc",
						location: "ourlab.mario.site.P2"
					},
					liquid1: {
						type: "Variable",
						value: "ourlab.mario.systemLiquid"
					}
				},
				steps: {
					"1": {
						command: "pipetter.pipette",
						sources: "liquid1",
						//sources: "ourlab.mario.systemLiquid",
						destinations: "plate1(A01)",
						volumes: "10ul",
						clean: "none"
					}
				}
			};
			var result = roboliq.run([__dirname+"/ourlab.js", "-o", ""], protocol, false);
			should.deepEqual(result.output.effects, {
				"1.1": {
					"ourlab.mario.systemLiquidLabware.contents": [
						"Infinity l",
						"systemLiquid"
					],
					"ourlab.mario.liha.syringe.1.contaminants": ["systemLiquid"],
					"plate1.contents.A01": [
						"10 ul",
						"systemLiquid"
					],
					"__WELLS__.ourlab.mario.systemLiquidLabware.contents": {
						"isSource": true,
						"volumeMin": "Infinity l",
						"volumeMax": "Infinity l",
						"volumeRemoved": "10 ul"
					},
					"__WELLS__.plate1.contents.A01": {
						"isSource": false,
						"volumeMin": "0 l",
						"volumeMax": "10 ul",
						"volumeAdded": "10 ul"
					},
				}
			});
		});

		it("should set the syringe state when aspirating", function() {
			var protocol = {
				roboliq: "v1",
				objects: {
					plate1: {
						type: "Plate",
						model: "ourlab.model.plateModel_96_square_transparent_nunc",
						location: "ourlab.mario.site.P2"
					},
					liquid1: {
						type: "Variable",
						value: "ourlab.mario.systemLiquid"
					},
					ourlab: {
						mario: {
							liha: {
								syringe: {
									1: {
										contaminants: undefined,
										contents: undefined,
										cleaned: "thorough"
									}
								}
							}
						}
					}.mario.liha.syring
				},
				steps: {
					"1": {
						command: "pipetter.pipette",
						sources: "liquid1",
						//sources: "ourlab.mario.systemLiquid",
						destinations: "plate1(A01)",
						volumes: "10ul",
						clean: "none"
					}
				}
			};
			var result = roboliq.run([__dirname+"/ourlab.js", "-o", ""], protocol, false);
			//console.log(JSON.stringify(result.output.steps, null, '\t'));
			should.deepEqual(result.output.effects, {
				"1.1": {
					"ourlab.mario.systemLiquidLabware.contents": [
						"Infinity l",
						"systemLiquid"
					],
					"ourlab.mario.liha.syringe.1.contaminants": ["systemLiquid"],
					"plate1.contents.A01": [
						"10 ul",
						"systemLiquid"
					],
					"__WELLS__.ourlab.mario.systemLiquidLabware.contents": {
						"isSource": true,
						"volumeMin": "Infinity l",
						"volumeMax": "Infinity l",
						"volumeRemoved": "10 ul"
					},
					"__WELLS__.plate1.contents.A01": {
						"isSource": false,
						"volumeMin": "0 l",
						"volumeMax": "10 ul",
						"volumeAdded": "10 ul"
					},
				}
			});
		});



		it("should pipette in layers", function() {
			const protocol = {
				roboliq: "v1",
				objects: {
					trough1: {
						type: "Plate",
						model: "ourlab.model.troughModel_100ml",
						location: "ourlab.mario.site.R6",
						contents: ['Infinity l', 'source1']
					},
					trough2: {
						type: "Plate",
						model: "ourlab.model.troughModel_100ml",
						location: "ourlab.mario.site.R6",
						contents: ['Infinity l', 'source2']
					},
					plate1: {
						type: "Plate",
						model: "ourlab.model.plateModel_96_square_transparent_nunc",
						location: "ourlab.mario.site.P2"
					},
					source1: {
						type: "Liquid",
						wells: "trough1(A01 down to H01)"
					},
					source2: {
						type: "Liquid",
						wells: "trough2(A01 down to H01)"
					},
				},
				steps: {
					"1": {
						command: "pipetter.pipette",
						clean: "none",
						items: [
							{layer: 1, source: "source1", destination: "plate1(A01)", volume: "10ul"},
							{layer: 2, source: "source2", destination: "plate1(A01)", volume: "10ul"},
							{layer: 1, source: "source1", destination: "plate1(B01)", volume: "10ul"},
							{layer: 2, source: "source2", destination: "plate1(B01)", volume: "10ul"},
							{layer: 1, source: "source1", destination: "plate1(C01)", volume: "10ul"},
							{layer: 2, source: "source2", destination: "plate1(C01)", volume: "10ul"},
							{layer: 1, source: "source1", destination: "plate1(D01)", volume: "10ul"},
							{layer: 2, source: "source2", destination: "plate1(D01)", volume: "10ul"},
							{layer: 1, source: "source1", destination: "plate1(E01)", volume: "10ul"},
							{layer: 2, source: "source2", destination: "plate1(E01)", volume: "10ul"},
							{layer: 1, source: "source1", destination: "plate1(F01)", volume: "10ul"},
							{layer: 2, source: "source2", destination: "plate1(F01)", volume: "10ul"},
							{layer: 1, source: "source1", destination: "plate1(G01)", volume: "10ul"},
							{layer: 2, source: "source2", destination: "plate1(G01)", volume: "10ul"},
							{layer: 1, source: "source1", destination: "plate1(H01)", volume: "10ul"},
							{layer: 2, source: "source2", destination: "plate1(H01)", volume: "10ul"},
						]
					}
				}
			};
			const result = roboliq.run([__dirname+"/ourlab.js", "-o", ""], protocol, false);
			//console.log(JSON.stringify(result.output.steps, null, '\t'));
			should.deepEqual(result.output.steps[1], {
				"1": {
					"command": "pipetter._pipette",
					"agent": "ourlab.mario.evoware",
					"equipment": "ourlab.mario.liha",
					"program": "\"Roboliq_Water_Dry_1000\"",
					"items": [
						{
							"syringe": "ourlab.mario.liha.syringe.1",
							"source": "trough1(A01)",
							"destination": "plate1(A01)",
							"volume": "10 ul"
						},
						{
							"syringe": "ourlab.mario.liha.syringe.2",
							"source": "trough1(B01)",
							"destination": "plate1(B01)",
							"volume": "10 ul"
						},
						{
							"syringe": "ourlab.mario.liha.syringe.3",
							"source": "trough1(C01)",
							"destination": "plate1(C01)",
							"volume": "10 ul"
						},
						{
							"syringe": "ourlab.mario.liha.syringe.4",
							"source": "trough1(D01)",
							"destination": "plate1(D01)",
							"volume": "10 ul"
						}
					]
				},
				"2": {
					"command": "pipetter._pipette",
					"agent": "ourlab.mario.evoware",
					"equipment": "ourlab.mario.liha",
					"program": "\"Roboliq_Water_Dry_1000\"",
					"items": [
						{
							"syringe": "ourlab.mario.liha.syringe.1",
							"source": "trough2(A01)",
							"destination": "plate1(A01)",
							"volume": "10 ul"
						},
						{
							"syringe": "ourlab.mario.liha.syringe.2",
							"source": "trough2(B01)",
							"destination": "plate1(B01)",
							"volume": "10 ul"
						},
						{
							"syringe": "ourlab.mario.liha.syringe.3",
							"source": "trough2(C01)",
							"destination": "plate1(C01)",
							"volume": "10 ul"
						},
						{
							"syringe": "ourlab.mario.liha.syringe.4",
							"source": "trough2(D01)",
							"destination": "plate1(D01)",
							"volume": "10 ul"
						}
					]
				},
				"3": {
					"command": "pipetter._pipette",
					"agent": "ourlab.mario.evoware",
					"equipment": "ourlab.mario.liha",
					"program": "\"Roboliq_Water_Dry_1000\"",
					"items": [
						{
							"syringe": "ourlab.mario.liha.syringe.1",
							"source": "trough1(A01)",
							"destination": "plate1(E01)",
							"volume": "10 ul"
						},
						{
							"syringe": "ourlab.mario.liha.syringe.2",
							"source": "trough1(B01)",
							"destination": "plate1(F01)",
							"volume": "10 ul"
						},
						{
							"syringe": "ourlab.mario.liha.syringe.3",
							"source": "trough1(C01)",
							"destination": "plate1(G01)",
							"volume": "10 ul"
						},
						{
							"syringe": "ourlab.mario.liha.syringe.4",
							"source": "trough1(D01)",
							"destination": "plate1(H01)",
							"volume": "10 ul"
						}
					]
				},
				"4": {
					"command": "pipetter._pipette",
					"agent": "ourlab.mario.evoware",
					"equipment": "ourlab.mario.liha",
					"program": "\"Roboliq_Water_Dry_1000\"",
					"items": [
						{
							"syringe": "ourlab.mario.liha.syringe.1",
							"source": "trough2(A01)",
							"destination": "plate1(E01)",
							"volume": "10 ul"
						},
						{
							"syringe": "ourlab.mario.liha.syringe.2",
							"source": "trough2(B01)",
							"destination": "plate1(F01)",
							"volume": "10 ul"
						},
						{
							"syringe": "ourlab.mario.liha.syringe.3",
							"source": "trough2(C01)",
							"destination": "plate1(G01)",
							"volume": "10 ul"
						},
						{
							"syringe": "ourlab.mario.liha.syringe.4",
							"source": "trough2(D01)",
							"destination": "plate1(H01)",
							"volume": "10 ul"
						}
					]
				},
				"command": "pipetter.pipette",
				"clean": "none",
				"items": [
					{
						"layer": 1,
						"source": "source1",
						"destination": "plate1(A01)",
						"volume": "10ul"
					},
					{
						"layer": 2,
						"source": "source2",
						"destination": "plate1(A01)",
						"volume": "10ul"
					},
					{
						"layer": 1,
						"source": "source1",
						"destination": "plate1(B01)",
						"volume": "10ul"
					},
					{
						"layer": 2,
						"source": "source2",
						"destination": "plate1(B01)",
						"volume": "10ul"
					},
					{
						"layer": 1,
						"source": "source1",
						"destination": "plate1(C01)",
						"volume": "10ul"
					},
					{
						"layer": 2,
						"source": "source2",
						"destination": "plate1(C01)",
						"volume": "10ul"
					},
					{
						"layer": 1,
						"source": "source1",
						"destination": "plate1(D01)",
						"volume": "10ul"
					},
					{
						"layer": 2,
						"source": "source2",
						"destination": "plate1(D01)",
						"volume": "10ul"
					},
					{
						"layer": 1,
						"source": "source1",
						"destination": "plate1(E01)",
						"volume": "10ul"
					},
					{
						"layer": 2,
						"source": "source2",
						"destination": "plate1(E01)",
						"volume": "10ul"
					},
					{
						"layer": 1,
						"source": "source1",
						"destination": "plate1(F01)",
						"volume": "10ul"
					},
					{
						"layer": 2,
						"source": "source2",
						"destination": "plate1(F01)",
						"volume": "10ul"
					},
					{
						"layer": 1,
						"source": "source1",
						"destination": "plate1(G01)",
						"volume": "10ul"
					},
					{
						"layer": 2,
						"source": "source2",
						"destination": "plate1(G01)",
						"volume": "10ul"
					},
					{
						"layer": 1,
						"source": "source1",
						"destination": "plate1(H01)",
						"volume": "10ul"
					},
					{
						"layer": 2,
						"source": "source2",
						"destination": "plate1(H01)",
						"volume": "10ul"
					}
				]
			});
		});

		it("should pipette in layers #2 with syringes specified", () => {
			const protocol = {
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
					1: {
						"command": "pipetter.pipette",
						"clean": "none",
						"destinationLabware": "plate1",
						"items": [
							{ "layer": 1, "source": "ourlab.mario.systemLiquid", "destination": "plate1(A02)", "volume": "50 ul", "syringe": "ourlab.mario.liha.syringe.1" },
							{ "layer": 2, "source": "ourlab.mario.systemLiquid", "destination": "plate1(A03)", "volume": "50 ul", "syringe": "ourlab.mario.liha.syringe.1" },
							{ "layer": 3, "source": "ourlab.mario.systemLiquid", "destination": "plate1(A04)", "volume": "50 ul", "syringe": "ourlab.mario.liha.syringe.1" },
							{ "layer": 1, "source": "ourlab.mario.systemLiquid", "destination": "plate1(B02)", "volume": "50 ul", "syringe": "ourlab.mario.liha.syringe.2" },
							{ "layer": 2, "source": "ourlab.mario.systemLiquid", "destination": "plate1(B03)", "volume": "50 ul", "syringe": "ourlab.mario.liha.syringe.2" },
							{ "layer": 3, "source": "ourlab.mario.systemLiquid", "destination": "plate1(B04)", "volume": "50 ul", "syringe": "ourlab.mario.liha.syringe.2" }
						]
					}
				}
			};
			const result = roboliq.run([__dirname+"/ourlab.js", "-o", ""], protocol, false);
			// console.log(JSON.stringify(result.output.steps, null, '\t'));
			should.deepEqual(_.pick(result.output.steps[1], "1", "2", "3"), {
				"1": {
					"command": "pipetter._pipette",
					"agent": "ourlab.mario.evoware",
					"equipment": "ourlab.mario.liha",
					"program": "\"Roboliq_Water_Dry_1000\"",
					"items": [
						{
							"syringe": "ourlab.mario.liha.syringe.1",
							"source": "ourlab.mario.systemLiquidLabware(A01)",
							"destination": "plate1(A02)",
							"volume": "50 ul"
						},
						{
							"syringe": "ourlab.mario.liha.syringe.2",
							"source": "ourlab.mario.systemLiquidLabware(B01)",
							"destination": "plate1(B02)",
							"volume": "50 ul"
						}
					]
				},
				"2": {
					"command": "pipetter._pipette",
					"agent": "ourlab.mario.evoware",
					"equipment": "ourlab.mario.liha",
					"program": "\"Roboliq_Water_Dry_1000\"",
					"items": [
						{
							"syringe": "ourlab.mario.liha.syringe.1",
							"source": "ourlab.mario.systemLiquidLabware(A01)",
							"destination": "plate1(A03)",
							"volume": "50 ul"
						},
						{
							"syringe": "ourlab.mario.liha.syringe.2",
							"source": "ourlab.mario.systemLiquidLabware(B01)",
							"destination": "plate1(B03)",
							"volume": "50 ul"
						}
					]
				},
				"3": {
					"command": "pipetter._pipette",
					"agent": "ourlab.mario.evoware",
					"equipment": "ourlab.mario.liha",
					"program": "\"Roboliq_Water_Dry_1000\"",
					"items": [
						{
							"syringe": "ourlab.mario.liha.syringe.1",
							"source": "ourlab.mario.systemLiquidLabware(A01)",
							"destination": "plate1(A04)",
							"volume": "50 ul"
						},
						{
							"syringe": "ourlab.mario.liha.syringe.2",
							"source": "ourlab.mario.systemLiquidLabware(B01)",
							"destination": "plate1(B04)",
							"volume": "50 ul"
						}
					]
				},
			});
		});

		it("should allow for syringes to be specified in a parameter", () => {
			const protocol = {
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
					1: {
						"command": "pipetter.pipette",
						"sources": "ourlab.mario.systemLiquid",
						"destinationLabware": "plate1",
						"destinations": [ "B02", "B07" ],
						"volumes": "50ul",
						"syringes": [ 2, 4 ],
						"clean": "none"
					}
				}
			};
			const result = roboliq.run([__dirname+"/ourlab.js", "-o", "", "-T"], protocol, false);
			// console.log(JSON.stringify(result.output.steps, null, '\t'));
			should.deepEqual(result.output.steps[1][1], {
				"command": "pipetter._pipette",
				"agent": "ourlab.mario.evoware",
				"equipment": "ourlab.mario.liha",
				"program": "\"Roboliq_Water_Dry_1000\"",
				"items": [
					{
						"syringe": 2,
						"source": "ourlab.mario.systemLiquidLabware(A01)",
						"destination": "plate1(B02)",
						"volume": "50 ul"
					},
					{
						"syringe": 4,
						"source": "ourlab.mario.systemLiquidLabware(B01)",
						"destination": "plate1(B07)",
						"volume": "50 ul"
					}
				]
			});
		});

		it("should handle destinationMixing={} and =true and =false", () => {
			const protocol = {
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
					1: {
						"command": "pipetter.pipette",
						"sources": "plate1(A01,B01)",
						"destinations": "plate1(A02,B02)",
						"volumes": "50ul",
						"destinationMixing": {},
						"clean": "none"
					}
				}
			};
			const expected = {
				"1": {
					"command": "pipetter._pipette",
					"agent": "ourlab.mario.evoware",
					"equipment": "ourlab.mario.liha",
					"program": "\"Roboliq_Water_Dry_1000\"",
					"items": [
						{ "syringe": "ourlab.mario.liha.syringe.1", "source": "plate1(A01)", "destination": "plate1(A02)", "volume": "50 ul", destinationMixing: {volume: "35 ul", count: 3} },
						{ "syringe": "ourlab.mario.liha.syringe.2", "source": "plate1(B01)", "destination": "plate1(B02)", "volume": "50 ul", destinationMixing: {volume: "35 ul", count: 3} }
					]
				}
			};
			const result1 = roboliq.run([__dirname+"/ourlab.js", "-o", "", "-T"], protocol, false);
			// console.log(JSON.stringify(result1.output.steps, null, '\t'));
			should.deepEqual(_.pick(result1.output.steps[1], [1, 2, 3]), expected);

			protocol.steps[1].destinationMixing = true;
			const result2 = roboliq.run([__dirname+"/ourlab.js", "-o", "", "-T"], protocol, false);
			// console.log(JSON.stringify(result2.output.steps, null, '\t'));
			should.deepEqual(_.pick(result2.output.steps[1], [1, 2, 3]), expected);

			protocol.steps[1].destinationMixing = false;
			const result3 = roboliq.run([__dirname+"/ourlab.js", "-o", "", "-T"], protocol, false);
			// console.log(JSON.stringify(result3.output.steps, null, '\t'));
			should.deepEqual(result3.output.steps[1][1].items, expected[1].items.map(x => _.omit(x, "destinationMixing")));
		});

		it("should handle sourceMixing={} and =true and =false", () => {
			const protocol = {
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
					1: {
						"command": "pipetter.pipette",
						"sources": "plate1(A01,B01)",
						"destinations": "plate1(A02,B02)",
						"volumes": "50ul",
						"sourceMixing": {},
						"clean": "none"
					}
				}
			};
			const expected = {
				"1": {
					"command": "pipetter._pipette",
					"agent": "ourlab.mario.evoware",
					"equipment": "ourlab.mario.liha",
					"program": "\"Roboliq_Water_Dry_1000\"",
					"items": [
						{ "syringe": "ourlab.mario.liha.syringe.1", "source": "plate1(A01)", "destination": "plate1(A02)", "volume": "50 ul", sourceMixing: {volume: "70 ul", count: 3} },
						{ "syringe": "ourlab.mario.liha.syringe.2", "source": "plate1(B01)", "destination": "plate1(B02)", "volume": "50 ul", sourceMixing: {volume: "70 ul", count: 3} }
					]
				},
			};
			const result1 = roboliq.run([__dirname+"/ourlab.js", "-o", "", "-T"], protocol, false);
			// console.log(JSON.stringify(result1.output.steps, null, '\t'));
			should.deepEqual(_.pick(result1.output.steps[1], [1, 2, 3]), expected);

			protocol.steps[1].sourceMixing = true;
			const result2 = roboliq.run([__dirname+"/ourlab.js", "-o", "", "-T"], protocol, false);
			// console.log(JSON.stringify(result2.output.steps, null, '\t'));
			should.deepEqual(_.pick(result2.output.steps[1], [1, 2, 3]), expected);

			protocol.steps[1].sourceMixing = false;
			const result3 = roboliq.run([__dirname+"/ourlab.js", "-o", "", "-T"], protocol, false);
			// console.log(JSON.stringify(result3.output.steps, null, '\t'));
			should.deepEqual(result3.output.steps[1][1].items, expected[1].items.map(x => _.omit(x, "sourceMixing")));
		});

		it("should translate calibration data to volumes", () => {
			const protocol = {
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
					1: {
						command: "pipetter.pipette",
						clean: 'none',
						calibrators: {
							absorbance: {
								calibratorVariable: "absorbance",
								calibratorData: [
									{absorbance: 0.5, volume: "50ul"},
									{absorbance: 1.5, volume: "150ul"},
								]
							}
						},
						items: [
							{source: "source1", destination: "plate1(A02)", volumeCalibrated: {calibrator: "absorbance", value: 1.0}},
							{source: "source2", destination: "plate1(A02)", volumeTotal: "175ul"}
						],
					}
				}
			};
			const result = roboliq.run([__dirname+"/ourlab.js", "-o", ""], protocol, false);
			// console.log(JSON.stringify(result.output.steps[1][1], null, '\t'));
			should.deepEqual(_.omit(result.output.steps[1][1], "1"), {
				"command": "pipetter._pipette",
				"agent": "ourlab.mario.evoware",
				"equipment": "ourlab.mario.liha",
				"program": "\"Roboliq_Water_Dry_1000\"",
				"items": [
					{
						"syringe": "ourlab.mario.liha.syringe.1",
						"source": "plate1(A01)",
						"destination": "plate1(A02)",
						"volume": "100 ul"
					},
					{
						"syringe": "ourlab.mario.liha.syringe.2",
						"source": "plate1(B01)",
						"destination": "plate1(A02)",
						"volume": "75 ul"
					}
				]
			});
		});

		it('should accept items from a data table', function () {
			var protocol = {
				roboliq: "v1",
				objects: {
					plate1: {
						type: "Plate",
						model: "ourlab.model.plateModel_96_square_transparent_nunc",
						location: "ourlab.mario.site.P2"
					},
					items1: {
						type: "Data",
						value: [{
							source: "plate1(A01)",
							destination: "plate1(A02)",
							volume: "20ul"
						}]
					}
				},
				steps: {
					"1": {
						command: "pipetter.pipette",
						items: "items1"
					}
				}
			};
			var result = roboliq.run([__dirname+"/ourlab.js", "-o", "", "-T"], protocol, false);
			//console.log(JSON.stringify(result.output.steps, null, '\t'));
			//console.log(JSON.stringify(result.output.effects, null, '\t'));
			should.deepEqual(result.output.errors, {});
			should.deepEqual(result.output.warnings, {});
			should.deepEqual(result.output.steps[1][2], {
				"command": "pipetter._pipette",
				"agent": "ourlab.mario.evoware",
				"equipment": "ourlab.mario.liha",
				"program": "\"Roboliq_Water_Dry_1000\"",
				"items": [
					{
						"syringe": "ourlab.mario.liha.syringe.1",
						"source": "plate1(A01)",
						"destination": "plate1(A02)",
						"volume": "20 ul"
					}
				]
			});
		});

	});
});
