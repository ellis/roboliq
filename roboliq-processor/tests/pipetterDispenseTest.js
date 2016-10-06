var _ = require('lodash');
var should = require('should');
var roboliq = require('../src/roboliq.js')

describe('pipetter', function() {
	describe('pipetter._dispense', function () {
		it("should update the syringe and destination state when fully dispensing syringe contents", function() {
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
										contaminants: ["water"],
										contents: ["10 ul", "water"]
									}
								}
							}
						}
					}
				},
				steps: {
					"1": {
						command: "pipetter._dispense",
						agent: "ourlab.mario.evoware",
						equipment: "ourlab.mario.liha",
						program: "\"Roboliq_Water_Dry_1000\"",
						items: [
							{
								syringe: "ourlab.mario.liha.syringe.1",
								destination: "plate1(A01)",
								volume: "10 ul"
							}
						]
					}
				}
			};
			var result = roboliq.run(["--ourlab", "-o", "", "-T"], protocol);
			//console.log(JSON.stringify(result.output.effects, null, '\t'))
			should.deepEqual(result.output.errors, {});
			should.deepEqual(result.output.warnings, {});
			should.deepEqual(result.output.effects, {
				"1": {
					"ourlab.mario.liha.syringe.1.contents": null,
					"plate1.contents.A01": [
						"10 ul",
						"water"
					],
					"__WELLS__.plate1.contents.A01": {
						"isSource": false,
						"volumeMin": "0 l",
						"volumeMax": "10 ul",
						"volumeAdded": "10 ul"
					}
				}
			});
		});


		it("should update the syringe and destination state when partially dispensing syringe contents", function() {
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
										contaminants: ["water"],
										contents: ["20 ul", "water"]
									}
								}
							}
						}
					}
				},
				steps: {
					"1": {
						command: "pipetter._dispense",
						agent: "ourlab.mario.evoware",
						equipment: "ourlab.mario.liha",
						program: "\"Roboliq_Water_Dry_1000\"",
						items: [
							{
								syringe: "ourlab.mario.liha.syringe.1",
								destination: "plate1(A01)",
								volume: "10 ul"
							}
						]
					}
				}
			};
			var result = roboliq.run(["--ourlab", "-o", "", "-T"], protocol);
			//console.log(JSON.stringify(result.output.effects, null, '\t'))
			should.deepEqual(result.output.errors, {});
			should.deepEqual(result.output.warnings, {});
			should.deepEqual(result.output.effects, {
				"1": {
					"ourlab.mario.liha.syringe.1.contents": [
						"10 ul",
						"water"
					],
					"plate1.contents.A01": [
						"10 ul",
						"water"
					],
					"__WELLS__.plate1.contents.A01": {
						"isSource": false,
						"volumeMin": "0 l",
						"volumeMax": "10 ul",
						"volumeAdded": "10 ul"
					}
				}
			});
		});


		it("should update the syringe and destination state when multi-dispensing", function() {
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
										contaminants: ["water"],
										contents: ["20 ul", "water"]
									}
								}
							}
						}
					}
				},
				steps: {
					"1": {
						command: "pipetter._dispense",
						agent: "ourlab.mario.evoware",
						equipment: "ourlab.mario.liha",
						program: "\"Roboliq_Water_Dry_1000\"",
						items: [
							{
								syringe: "ourlab.mario.liha.syringe.1",
								destination: "plate1(A01)",
								volume: "10 ul"
							},
							{
								syringe: "ourlab.mario.liha.syringe.1",
								destination: "plate1(B01)",
								volume: "10 ul"
							},
						]
					}
				}
			};
			var result = roboliq.run(["--ourlab", "-o", "", "-T"], protocol);
			//console.log(JSON.stringify(result.output.effects, null, '\t'))
			should.deepEqual(result.output.errors, {});
			should.deepEqual(result.output.warnings, {});
			should.deepEqual(result.output.effects, {
				"1": {
					"ourlab.mario.liha.syringe.1.contents": null,
					"plate1.contents.A01": [
						"10 ul",
						"water"
					],
					"plate1.contents.B01": [
						"10 ul",
						"water"
					],
					"__WELLS__.plate1.contents.A01": {
						"isSource": false,
						"volumeMin": "0 l",
						"volumeMax": "10 ul",
						"volumeAdded": "10 ul"
					},
					"__WELLS__.plate1.contents.B01": {
						"isSource": false,
						"volumeMin": "0 l",
						"volumeMax": "10 ul",
						"volumeAdded": "10 ul"
					}
				}
			});
		});


		it("should update the syringe contaminants when wet despensing", function() {
			var protocol = {
				roboliq: "v1",
				objects: {
					plate1: {
						type: "Plate",
						model: "ourlab.model.plateModel_96_square_transparent_nunc",
						location: "ourlab.mario.site.P2",
						contents: {
							A01: ["10 ul", "oil"]
						}
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
										contaminants: ["water"],
										contents: ["10 ul", "water"]
									}
								}
							}
						}
					}
				},
				steps: {
					"1": {
						command: "pipetter._dispense",
						agent: "ourlab.mario.evoware",
						equipment: "ourlab.mario.liha",
						program: "\"Roboliq_Water_Wet_1000\"",
						items: [
							{
								syringe: "ourlab.mario.liha.syringe.1",
								destination: "plate1(A01)",
								volume: "10 ul"
							}
						]
					}
				}
			};
			var result = roboliq.run(["--ourlab", "-o", "", "-T"], protocol);
			//console.log(JSON.stringify(result.output.effects, null, '\t'))
			should.deepEqual(result.output.errors, {});
			should.deepEqual(result.output.warnings, {});
			should.deepEqual(result.output.effects, {
				"1": {
					"ourlab.mario.liha.syringe.1.contents": null,
					"ourlab.mario.liha.syringe.1.contaminants": ["water", "oil"],
					"plate1.contents.A01": ["20 ul", ["10 ul", "oil"], ["10 ul", "water"]],
					"__WELLS__.plate1.contents.A01": {
						"isSource": false,
						"volumeMin": "10 ul",
						"volumeMax": "20 ul",
						"volumeAdded": "10 ul"
					}
				}
			});
		});


	});
});
