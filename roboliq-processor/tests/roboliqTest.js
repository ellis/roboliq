var _ = require('lodash');
var assert = require('assert');
var should = require('should');
var roboliq = require('../src/roboliq.js')

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

		it("should handle 'data' property with forEach=row", () => {
			const protocol = {
				roboliq: "v1",
				objects: {
					design1: {
						type: "Design",
						conditions: {
							"text*": ["hello", "world"]
						}
					}
				},
				steps: {
					1: {
						data: {
							source: "design1",
							forEach: "row"
						},
						command: "system.echo",
						value: "$text"
					}
				}
			};
			var result = roboliq.run([__dirname+"/ourlab.js", "-o", "", "-T"], protocol, false);
			//console.log("result:\n"+JSON.stringify(result.output.steps, null, '\t'));
			should.deepEqual(result.output.steps['1'], {
				data: {
					source: "design1",
					forEach: "row"
				},
				command: "system.echo",
				value: "$text",
				"1": { "1": { "command": "system._echo", "value": "hello" }, "command": "system.echo", "value": "hello" },
				"2": { "1": { "command": "system._echo", "value": "world" }, "command": "system.echo", "value": "world" }
			});
		});

		it("should handle 'data' property with forEach=group", () => {
			const protocol = {
				roboliq: "v1",
				objects: {
					design1: {
						type: "Design",
						conditions: {
							"greeting*": ["hello", "goodbye"],
							"name*": ["john", "jane"]
						}
					}
				},
				steps: {
					1: {
						data: {
							source: "design1",
							groupBy: "name",
							forEach: "group"
						},
						command: "system.echo",
						value: "$$greeting"
					}
				}
			};
			var result = roboliq.run([__dirname+"/ourlab.js", "-o", ""], protocol, false);
			//console.log("result:\n"+JSON.stringify(result.output.steps, null, '\t'));
			should.deepEqual(result.output.steps['1'], {
				data: {
					source: "design1",
					groupBy: "name",
					forEach: "group"
				},
				command: "system.echo",
				value: "$$greeting",
				1: { command: "system.echo", value: ["hello", "goodbye"], 1: { command: "system._echo", value: ["hello", "goodbye"] } },
				2: { command: "system.echo", value: ["hello", "goodbye"], 1: { command: "system._echo", value: ["hello", "goodbye"] } }
			});
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
						value: "$a"
					},
					2: {
						"@DATA": [
							{a: "A", b: 1},
							{a: "A", b: 2}
						],
						command: "system.echo",
						value: "$$b"
					},
					3: {
						"@DATA": [
							{a: "A", b: 1},
							{a: "A", b: 2}
						],
						1: {
							command: "system.echo",
							value: "$a"
						},
						2: {
							command: "system.echo",
							value: "$$b"
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
					"value": "$a"
				},
				"2": {
					"1": {
						"command": "system._echo",
						"value": [ 1, 2 ]
					},
					"@DATA": [ { "a": "A", "b": 1 }, { "a": "A", "b": 2 } ],
					"command": "system.echo",
					"value": "$$b"
				},
				"3": {
					"1": {
						"1": {
							"command": "system._echo",
							"value": "A"
						},
						"command": "system.echo",
						"value": "$a"
					},
					"2": {
						"1": {
							"command": "system._echo",
							"value": [ 1, 2 ]
						},
						"command": "system.echo",
						"value": "$$b"
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
						type: "Design",
						conditions: {
							"a*": {
								B: { b: 1, c: 1},
								C: { c: 2}
							}
						}
					}
				},
				steps: {
					1: {
						// "@DATA": [
						// 	{a: "B", b: 1, c: 1},
						// 	{a: "C", c: 2}
						// ],
						data: {source: "design"},
						1: {
							command: "system.echo",
							value: {"#data": {value: "a", join: ","}}
						},
						2: {
							data: {where: {a: "C"}},
							command: "system.echo",
							value: "$c"
						},
						3: {
							command: "system.echo",
							value: {"#data": {value: "b", join: ","}}
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
