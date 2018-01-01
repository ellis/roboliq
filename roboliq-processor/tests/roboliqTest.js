/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017, ETH Zurich, Ellis Whitehead
 * @license GPL-3.0
 */

const _ = require('lodash');
const assert = require('assert');
const should = require('should');
const roboliq = require('../src/roboliq.js')

describe('roboliq', function() {
	describe('roboliq.run', function () {

		it('should handle directives in objects correctly', function () {
			var protocol = {
				roboliq: "v1",
				objects: {
					plateModel1: {
						type: "PlateModel",
						rows: 8,
						columns: 12
					},
					plate1: {
						type: "Plate",
						model: "plateModel1",
						location: "ourlab.mario.site.P2"
					},
					plate2: {
						type: "Plate",
						model: "plateModel1",
						location: "ourlab.mario.site.P3"
					},
					mixtures: {
						type: 'Variable',
						value: [
							[{source: 'a'}, {source: 'b'}],
							[{source: 'c'}, {source: 'd'}],
						]
					},
					mixtureWells: {
						type: 'Variable',
						calculate: {"#take": {
							list: "#destinationWells#plate1(all)",
							count: "#length#mixtures"
						}}
					},
					balanceWells: {
						type: 'Variable',
						calculate: {"#replaceLabware": {
							list: 'mixtureWells',
							new: 'plate2'
						}}
					}
				}
			};
			var result = roboliq.run([__dirname+"/ourlab.js", "-o", ""], protocol, false);
			//console.log("result:\n"+JSON.stringify(result, null, '\t'));
			should.deepEqual(result.output.errors, {});
			should.deepEqual(result.output.warnings, {});
			should.deepEqual(result.output.objects.mixtureWells.value,
				['plate1(A01)', 'plate1(B01)']
			);
			should.deepEqual(result.output.objects.balanceWells.value,
				['plate2(A01)', 'plate2(B01)']
			);
		});

		it("should handle directives in steps (#1)", () => {
			var protocol = {
				roboliq: "v1",
				objects: {
					list1: {
						type: 'Variable',
						value: [1,2,3]
					},
				},
				steps: {
					1: {
						command: "timer.sleep",
						duration: "#length#list1"
					}
				}
			};
			var result = roboliq.run([__dirname+"/ourlab.js", "-o", ""], protocol, false);
			//console.log("result:\n"+JSON.stringify(result, null, '\t'));
			should.deepEqual(result.output.errors, {});
			should.deepEqual(result.output.warnings, {});
			should.deepEqual(result.output.steps[1].duration, "#length#list1");
			should.deepEqual(result.output.steps[1][1].duration, "3 s");
		});

		it("should handle imports", function() {
			var protocol1 = {
				roboliq: "v1",
				objects: {
					plateModel1: {
						type: "PlateModel",
						rows: 8,
						columns: 12
					},
					plate1: {
						type: "Plate",
						location: "ourlab.mario.site.P2"
					}
				}
			};
			var protocol2 = {
				roboliq: "v1",
				imports: ["./protocol1.json"],
				objects: {
					plate1: {
						model: "plateModel1"
					}
				}
			};
			var result = roboliq.run([__dirname+"/ourlab.js", "-o", "", "--file-json", "./protocol1.json:"+JSON.stringify(protocol1)], protocol2, false);
			should.deepEqual(result.output.errors, {});
			should.deepEqual(result.output.warnings, {});
			should.deepEqual(result.output.objects.plate1, {
				type: "Plate",
				location: "ourlab.mario.site.P2",
				model: "plateModel1"
			});
		});

		it("should handle $#-parameter pre-compiler substitution", () => {
			const protocol = {
				roboliq: "v1",
				parameters: {
					TEXT: { value: "Hello, World" },
					// WELLS: { value: ["A01", "A02", "A03", "A04"] }
					WELLS: {
						value: {
							"allocateWells()": { N: 4, rows: 8, columns: 12, from: "A01" }
						}
					}
				},
				steps: {
					1: {
						command: "system._echo",
						value: "$#TEXT"
					},
					2: {
						command: "system._echo",
						value: "$#WELLS"
					}
				}
			};
			var result = roboliq.run([__dirname+"/ourlab.js", "-o", "", "-T"], protocol, false);
			// console.log("parameters:\n"+JSON.stringify(result.output.parameters, null, '\t'));
			//console.log("result:\n"+JSON.stringify(result.output.steps, null, '\t'));
			should.deepEqual(result.output.steps, {
				"1": { "command": "system._echo", "value": "Hello, World" },
				"2": { "command": "system._echo", "value": ["A01", "B01", "C01", "D01"] }
			});
		});

		it("should handle $-parameter substitution", () => {
			const protocol = {
				roboliq: "v1",
				parameters: {
					TEXT: { value: "Hello, World" },
				},
				objects: {
					data1: {
						type: "Data",
						value: [
							{a: 1, b: 1},
							{a: 1, b: 2}
						]
					}
				},
				steps: {
					1: {
						data: "data1",
						command: "system.echo",
						value: {
							javascript: "${`${TEXT} ${a} ${__step.command}`}",
							javascriptLodash: "${_.map(b, x => x*2)}",
							javascriptMath: "${math.multiply(2, 3)}",
							math: "$(a_ONE * 10)",
							scopeParameter: "$TEXT",
							scopeColumn: "$b",
							scopeDataCommon: "$a_ONE",
							scopeData: "$__data[0].b",
							scopeObjects: "$__objects.data1.type",
							scopeParameters: "$__parameters.TEXT.value",
							scopeStep: "$__step.command",
							// templateString: "`Hello, {{a}} {{b}}`",
							// templateObject: "`{}`"
						}
					}
				}
			};
			var result = roboliq.run([__dirname+"/ourlab.js", "-o", "", "-T"], protocol, false);
			// console.log("parameters:\n"+JSON.stringify(result.output.parameters, null, '\t'));
			// console.log("result:\n"+JSON.stringify(result.output.steps, null, '\t'));
			should.deepEqual(result.output.steps, {
				"1": {
					"1": {
						"command": "system._echo",
						"value": {
							"javascript": "Hello, World 1,1 system.echo",
							"javascriptLodash": [2, 4],
							"javascriptMath": 6,
							"math": 10,
							"scopeParameter": "Hello, World",
							"scopeColumn": [ 1, 2 ],
							"scopeDataCommon": 1,
							"scopeData": 1,
							"scopeObjects": "Data",
							"scopeParameters": "Hello, World",
							"scopeStep": "system.echo",
						}
					},
					"data": "data1",
					"command": "system.echo",
					"value": protocol.steps["1"].value
				}
			});
		});

		it("should handle ?-properties in objects and steps", () => {
			var protocol = {
				roboliq: "v1",
				objects: {
					here: {
						type: 'Site',
					},
					plate1: {
						type: 'Plate',
						'model?': {
							description: "please provide a model"
						},
						'location?': {
							description: "please provide a location",
							'value!': "here"
						}
					},
				},
				steps: {
					1: {
						command: "timer.sleep",
						'duration?': {
							description: "please provide a duration"
						}
					}
				}
			};
			var result = roboliq.run([__dirname+"/ourlab.js", "-o", "", "--quiet"], protocol, false);
			//console.log("result:\n"+JSON.stringify(result, null, '\t'));
			assert(_.size(result.output.errors) > 0, "should have an error due to missing steps.1.duration");
			should.deepEqual(result.output.fillIns, {
				'objects.plate1.model': {description: "please provide a model"},
				'steps.1.duration': {description: "please provide a duration"},
			});
			should.deepEqual(result.output.warnings, {});
			should.deepEqual(result.output.objects.plate1.location, "here");
		});

		it("should handle !-properties in objects and steps", () => {
			var protocol = {
				roboliq: "v1",
				objects: {
					here: {
						type: 'Site',
					},
					there: {
						type: 'Site',
					},
					'plate1!': {
						type: 'Plate',
						location: "here"
					},
					'plate2': {
						type: 'Plate',
						'location!': "there"
					},
				},
				steps: {
					1: {
						command: "timer.sleep",
						'duration!': 3
					}
				}
			};
			var result = roboliq.run([__dirname+"/ourlab.js", "-o", ""], protocol, false);
			//console.log("result:\n"+JSON.stringify(result, null, '\t'));
			should.deepEqual(result.output.objects.plate1.location, "here");
			should.deepEqual(result.output.objects.plate2.location, "there");
			should.deepEqual(result.output.steps['1'].duration, 3);
		});

		it.skip("should handle 'data' property with groupBy", () => {
			assert(false);
		});

		it.skip("should handle 'data' property without groupBy or forEach", () => {
			assert(false);
		});

		it("should pass @DATA down to sub-commands", () => {
			const protocol = {
				roboliq: "v1",
				steps: {
					1: {
						"@DATA": [
							{a: "A", b: 1},
							{a: "A", b: 2}
						],
						command: "system.echo",
						value: "$a_ONE"
					},
					2: {
						"@DATA": [
							{a: "A", b: 1},
							{a: "A", b: 2}
						],
						command: "system.echo",
						value: "$b"
					},
					3: {
						"@DATA": [
							{a: "A", b: 1},
							{a: "A", b: 2}
						],
						1: {
							command: "system.echo",
							value: "$a_ONE"
						},
						2: {
							command: "system.echo",
							value: "$b"
						}
					}
				}
			};
			var result = roboliq.run([__dirname+"/ourlab.js", "-o", ""], protocol, false);
			// console.log("result:\n"+JSON.stringify(result.output.steps, null, '\t'));
			should.deepEqual(result.output.steps, {
				"1": {
					"1": {
						"command": "system._echo",
						"value": "A"
					},
					"@DATA": [ { "a": "A", "b": 1 }, { "a": "A", "b": 2 } ],
					"command": "system.echo",
					"value": "$a_ONE"
				},
				"2": {
					"1": {
						"command": "system._echo",
						"value": [ 1, 2 ]
					},
					"@DATA": [ { "a": "A", "b": 1 }, { "a": "A", "b": 2 } ],
					"command": "system.echo",
					"value": "$b"
				},
				"3": {
					"1": {
						"1": {
							"command": "system._echo",
							"value": "A"
						},
						"command": "system.echo",
						"value": "$a_ONE"
					},
					"2": {
						"1": {
							"command": "system._echo",
							"value": [ 1, 2 ]
						},
						"command": "system.echo",
						"value": "$b"
					},
					"@DATA": [ { "a": "A", "b": 1 }, { "a": "A", "b": 2 } ]
				}
			});
		});

		it("should post-pone evaluation of directives in steps that have a 'data' property", () => {
			const protocol = {
				roboliq: "v1",
				objects: {
					design: {
						type: "Data",
						design: {
							"a*": {
								B: {b: 1, c: 1},
								C: {c: 2}
							}
						}
					}
				},
				steps: {
					1: {
						data: {source: "design"},
						1: {
							command: "system.echo",
							value: {"data()": {map: "$a", join: ","}}
						},
						2: {
							data: {where: {a: '"C"'}},
							command: "system.echo",
							value: "$c_ONE"
						}
					}
				}
			};
			var result = roboliq.run([__dirname+"/ourlab.js", "-o", ""], protocol, false);
			// console.log("result:\n"+JSON.stringify(result.output.steps, null, '\t'));
			should.deepEqual(result.output.steps[1][1][1], {
				command: "system._echo",
				value: "B,C"
			});
			should.deepEqual(result.output.steps[1][2][1], {
				command: "system._echo",
				value: 2
			});
		});

		it("should handle suspension on `system.runtimeLoadVariables`", () => {
			const protocol = {
				roboliq: "v1",
				steps: {
					1: {
						command: "system.echo",
						value: "first"
					},
					2: {
						command: "system.runtimeLoadVariables",
						variables: ["a"],
						varset: "varset1"
					},
					3: {
						command: "system.echo",
						value: "$a"
					}
				}
			};
			var result = roboliq.run([__dirname+"/ourlab.js", "-o", ""], protocol, false);
			// console.log("result:\n"+JSON.stringify(result.output.steps, null, '\t'));
			// console.log("output COMPILER:\n"+JSON.stringify(result.output.COMPILER, null, '\t'));
			// console.log("dump COMPILER:\n"+JSON.stringify(result.dump.COMPILER, null, '\t'));
			should.deepEqual(result.output.steps, {
				"1": {
					"1": {
						"command": "system._echo",
						"value": "first"
					},
					"command": "system.echo",
					"value": "first"
				},
				"2": {
					"command": "system.runtimeLoadVariables",
					"variables": [
						"a"
					],
					"varset": "varset1"
				},
				"3": {
					"command": "system.echo",
					"value": "$a"
				}
			});
			should.deepEqual(result.output.COMPILER, {
				"suspendStepId": "2"
			});
			should.deepEqual(result.dump.COMPILER, {
				"resumeStepId": "2"
			});
		});

		it("should handle resuming on `system.runtimeLoadVariables`", () => {
			const protocol = {
				roboliq: "v1",
				objects: {
					SCOPE: {
						a: 3
					}
				},
				steps: {
					"1": {
						"1": {
							"command": "system._echo",
							"value": "!!!!!!!" // This is here because it will be overwritten if its parent command gets processed
						},
						"command": "system.echo",
						"value": "first"
					},
					"2": {
						"command": "system.runtimeLoadVariables",
						"variables": [
							"a"
						],
						"varset": "varset1"
					},
					"3": {
						"command": "system.echo",
						"value": "$a"
					}
				},
				COMPILER: {
					resumeStepId: "2"
				}
			};
			var result = roboliq.run([__dirname+"/ourlab.js", "-o", ""], protocol, false);
			// console.log("result:\n"+JSON.stringify(result.output.steps, null, '\t'));
			// console.log("COMPILER:\n"+JSON.stringify(result.output.COMPILER, null, '\t'));
			should.deepEqual(result.output.steps, {
				"1": {
					"1": {
						"command": "system._echo",
						"value": "!!!!!!!"
					},
					"command": "system.echo",
					"value": "first"
				},
				"2": {
					"command": "system.runtimeLoadVariables",
					"variables": [
						"a"
					],
					"varset": "varset1"
				},
				"3": {
					"1": {
						"command": "system._echo",
						"value": 3
					},
					"command": "system.echo",
					"value": "$a"
				}
			});
			should.deepEqual(result.output.COMPILER, {
				resumeStepId: "2"
			});
		});

		it("should support --varset option", () => {
			const protocol = {
				roboliq: "v1",
				steps: {
					"1": {
						"command": "system.echo",
						"value": "$a"
					}
				}
			};
			var result = roboliq.run([__dirname+"/ourlab.js", "-o", "", "-T", "--varset", '{"a": 3}'], protocol, false);
			// console.log("result:\n"+JSON.stringify(result.output.steps, null, '\t'));
			// console.log("scope:\n"+JSON.stringify(result.output.objects.SCOPE, null, '\t'));
			should.deepEqual(result.output.steps, {
				"1": {
					"1": {
						"command": "system._echo",
						"value": 3
					},
					"command": "system.echo",
					"value": "$a"
				}
			});
		});

	});
});
