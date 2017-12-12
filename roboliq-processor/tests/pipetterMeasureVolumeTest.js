/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017 Ellis Whitehead
 * @license GPL-3.0
 */

var _ = require('lodash');
var should = require('should');
var roboliq = require('../src/roboliq.js')

describe('pipetter', function() {
	describe('pipetter._measureVolume', function () {
		it("should update the syringe state", function() {
			var protocol = {
				roboliq: "v1",
				objects: {
					plate1: {
						type: "Plate",
						model: "ourlab.model.plateModel_96_square_transparent_nunc",
						location: "ourlab.mario.site.P2"
					},
					/*source1: {
						type: "Liquid",
						wells: "plate1(A01)"
					},*/
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
					}
				},
				steps: {
					"1": {
						command: "pipetter._measureVolume",
						agent: "ourlab.mario.evoware",
						equipment: "ourlab.mario.liha",
						items: [
							{
								syringe: "ourlab.mario.liha.syringe.1",
								well: "plate1(A01)"
							}
						]
					}
				}
			};
			var result = roboliq.run([__dirname+"/ourlab.js", "-o", "", "-T"], protocol, false);
			//console.log(JSON.stringify(result.output.effects, null, '\t'))
			should.deepEqual(result.output.errors, {});
			should.deepEqual(result.output.warnings, {});
			should.deepEqual(result.output.effects, {
				"1": {
					"ourlab.mario.liha.syringe.1.contaminants": [
						"plate1(A01)"
					],
					"ourlab.mario.liha.syringe.1.cleaned": null
				}
			});
		});
	});

	describe('pipetter.measureVolume', function () {
		it("should handle pipetting of a single well", function() {
			var protocol = {
				roboliq: "v1",
				objects: {
					plate1: {
						type: "Plate",
						model: "ourlab.model.plateModel_96_square_transparent_nunc",
						location: "ourlab.mario.site.P2",
						sealed: true
					},
					source1: {
						type: "Liquid",
						wells: "plate1(A01)"
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
					}
				},
				steps: {
					"1": {
						command: "pipetter.measureVolume",
						wells: "plate1(A01)"
					}
				}
			};
			var result = roboliq.run([__dirname+"/ourlab.js", "-o", "", "-T"], protocol, false);
			// console.log(JSON.stringify(result.output.steps[1], null, '\t'))
			//console.log(JSON.stringify(result.output.effects, null, '\t'))
			should.deepEqual(result.output.errors, {});
			should.deepEqual(result.output.warnings, {});
			should.deepEqual(result.output.steps[1][1], {
				"command": "pipetter._measureVolume",
				"agent": "ourlab.mario.evoware",
				"equipment": "ourlab.mario.liha",
				"items": [
					{
						"syringe": "ourlab.mario.liha.syringe.1",
						"well": "plate1(A01)"
					}
				]
			});
			should.deepEqual(result.output.effects["1.1"], {
				'ourlab.mario.liha.syringe.1.cleaned': null,
				'ourlab.mario.liha.syringe.1.contaminants': [ 'source1' ]
			});
		});
	});

});
