var _ = require('lodash');
var should = require('should');
var roboliq = require('../src/roboliq.js')

describe('system', function() {
	describe("system.call", function() {
		it("should handle template objects", function() {
			var protocol = {
				roboliq: "v1",
				objects: {
					mySteps1: {
						type: "Template",
						template: {
							1: {comment: "Hello, {{name}}!"},
							2: {comment: "comment 2"}
						}
					}
				},
				steps: {
					1: {
						command: "system.call",
						name: "mySteps1",
						lazyParams: {name: "World"}
					}
				}
			};
			const result = roboliq.run([__dirname+"/ourlab.js", "-o", "", "-T"], protocol, false);
			//console.log(JSON.stringify(result, null, '\t'))
			should.deepEqual(result.output.steps, {
				1: {
					1: {comment: "Hello, World!"},
					2: {comment: "comment 2"},
					command: "system.call",
					name: "mySteps1",
					lazyParams: {name: "World"}
				}
			});
		});

		it("should handle substitution within lazyParams", () => {
			var protocol = {
				roboliq: "v1",
				objects: {
					mySteps1: {
						type: "Template",
						template: {
							1: {comment: "Hello, {{name}}!"},
							2: {comment: "Welcome to {{place}}."}
						}
					}
				},
				steps: {
					"@SCOPE": {myName: "John"},
					1: {
						command: "system.call",
						name: "mySteps1",
						lazyParams: {name: "$myName", place: "Earth"}
					}
				}
			};
			const result = roboliq.run([__dirname+"/ourlab.js", "-o", "", "-T"], protocol, false);
			//console.log(JSON.stringify(result, null, '\t'))
			should.deepEqual(result.output.steps, {
				"@SCOPE": {myName: "John"},
				1: {
					1: {comment: "Hello, John!"},
					2: {comment: "Welcome to Earth."},
					command: "system.call",
					name: "mySteps1",
					lazyParams: {name: "$myName", place: "Earth"}
				}
			});
		});
	});

	describe.skip("system.description", () => {
		it("should process a ``-string", () => {
			const protocol = {
				roboliq: "v1",
				steps: {
					"1": {
						command: "system.description",
						value: "`Hello {{}}`",
						then: {comment: "yep"},
						else: {"1": {comment: "nope"}}
					},
					"2": {
						command: "system.if",
						test: false,
						then: {comment: "yep"},
						else: {"1": {comment: "nope"}}
					}
				}
			};
			const result = roboliq.run([__dirname+"/ourlab.js", "-o", "", "-T"], protocol, false);
			should.deepEqual(result.output.steps[1], {
				command: "system.if",
				test: true,
				then: {comment: "yep"},
				else: {"1": {comment: "nope"}},
				1: {comment: "yep"},
			});
		});
	});

	describe('system.if', function () {
		it('should branch based on `test`', function () {
			const protocol = {
				roboliq: "v1",
				steps: {
					"1": {
						command: "system.if",
						test: true,
						then: {comment: "yep"},
						else: {"1": {comment: "nope"}}
					},
					"2": {
						command: "system.if",
						test: false,
						then: {comment: "yep"},
						else: {"1": {comment: "nope"}}
					}
				}
			};
			const result = roboliq.run([__dirname+"/ourlab.js", "-o", "", "-T"], protocol, false);
			should.deepEqual(result.output.steps[1], {
				command: "system.if",
				test: true,
				then: {comment: "yep"},
				else: {"1": {comment: "nope"}},
				1: {comment: "yep"},
			});
			should.deepEqual(result.output.steps[2], {
				command: "system.if",
				test: false,
				then: {comment: "yep"},
				else: {"1": {comment: "nope"}},
				1: {comment: "nope"},
			});
		});
	});

	describe('system.repeat', function () {

		it('should repeat `body`, `count` times', function () {
			var protocol = {
				roboliq: "v1",
				steps: {
					"1": {
						command: "system.repeat",
						count: 2,
						steps: {comment: "empty"}
					}
				}
			};
			var result = roboliq.run([__dirname+"/ourlab.js", "-o", "", "-T"], protocol, false);
			should.deepEqual(result.output.steps[1], {
				command: "system.repeat",
				count: 2,
				steps: {comment: "empty"},
				1: {comment: "empty"},
				2: {comment: "empty"}
			});

			var protocol = {
				roboliq: "v1",
				steps: {
					"1": {
						command: "system.repeat",
						count: 2,
						steps: {
							1: {comment: "empty"}
						}
					}
				}
			};
			var result = roboliq.run([__dirname+"/ourlab.js", "-o", "", "-T"], protocol, false);
			should.deepEqual(result.output.steps[1], {
				command: "system.repeat",
				count: 2,
				steps: {
					1: {comment: "empty"}
				},
				1: {1: {comment: "empty"}},
				2: {1: {comment: "empty"}}
			});
		});

		it("should handle system.repeat with system.runtimeExitLoop", () => {
			var protocol = {
				roboliq: "v1",
				steps: {
					"1": {
						command: "system.repeat",
						count: 2,
						steps: {
							1: {comment: "empty"},
							2: {command: "system.runtimeExitLoop", testType: "R", test: "cat('true')"}
						}
					}
				}
			};
			var result = roboliq.run([__dirname+"/ourlab.js", "-o", "", "-T"], protocol, false);
			// console.log(JSON.stringify(result.output, null, '\t'));
			should.deepEqual(_.pick(result.output.steps[1], ["1", "2"]), {
				1: {1: {comment: "empty"}, 2: {command: "system.runtimeExitLoop", testType: "R", test: "cat('true')"}},
				2: {1: {comment: "empty"}, 2: {command: "system.runtimeExitLoop", testType: "R", test: "cat('true')"}}
			});
		});

	});
});
