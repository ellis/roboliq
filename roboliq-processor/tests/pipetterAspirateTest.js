var _ = require('lodash');
var should = require('should');
var roboliq = require('../src/roboliq.js')

describe('pipetter', function() {
	describe('pipetter._aspirate', function () {
		it("should update the syringe and source state", function() {
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
					}
				},
				steps: {
					"1": {
						command: "pipetter._aspirate",
						agent: "ourlab.mario.evoware",
						equipment: "ourlab.mario.liha",
						program: "\"Roboliq_Water_Dry_1000\"",
						items: [
							{
								syringe: "ourlab.mario.liha.syringe.1",
								source: "plate1(A01)",
								volume: "10 ul"
							}
						]
					}
				}
			};
			var result = roboliq.run(["--ourlab", "-o", "", "-T"], protocol, false);
			//console.log(JSON.stringify(result.output.effects, null, '\t'))
			should.deepEqual(result.output.errors, {});
			should.deepEqual(result.output.warnings, {});
			should.deepEqual(result.output.effects, {
				"1": {
					"ourlab.mario.liha.syringe.1.contaminants": [
						"plate1(A01)"
					],
					"ourlab.mario.liha.syringe.1.cleaned": null,
					"ourlab.mario.liha.syringe.1.contents": [
						"10 ul",
						"plate1(A01)"
					],
					"plate1.contents.A01": [
						"Infinity l",
						"plate1(A01)"
					],
					"__WELLS__.plate1.contents.A01": {
						"isSource": true,
						"volumeMin": "Infinity l",
						"volumeMax": "Infinity l",
						"volumeRemoved": "10 ul"
					}
				}
			});
		});
	});
});
