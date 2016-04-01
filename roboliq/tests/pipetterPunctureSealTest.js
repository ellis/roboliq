var _ = require('lodash');
var should = require('should');
var roboliq = require('../src/roboliq.js')

describe('pipetter', function() {
	describe('pipetter._punctureSeal', function () {
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
						command: "pipetter._punctureSeal",
						agent: "ourlab.mario.evoware",
						equipment: "ourlab.mario.liha",
						program: "\"Roboliq_Water_Dry_1000\"",
						items: [
							{
								syringe: "ourlab.mario.liha.syringe.1",
								well: "plate1(A01)",
								distance: "3mm"
							}
						]
					}
				}
			};
			var result = roboliq.run(["-o", "", "-T"], protocol);
			//console.log(JSON.stringify(result.output.effects, null, '\t'))
			should.deepEqual(result.output.errors, {});
			should.deepEqual(result.output.warnings, {});
			should.deepEqual(result.output.effects, {
				"1": {
					"ourlab.mario.liha.syringe.1.contaminants": [
						"plate1(A01)"
					],
					"ourlab.mario.liha.syringe.1.cleaned": null,
					"plate1.sealPunctures.A01": true,
				}
			});
		});
	});
});
