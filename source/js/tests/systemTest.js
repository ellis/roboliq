var _ = require('lodash');
var should = require('should');
var roboliq = require('../roboliq.js')

describe('system', function() {
	describe("system.call", function() {
		it("should handle template objects", function() {
			var protocol = {
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
						params: {name: "World"}
					}
				}
			};
			var result = roboliq.run(["-o", "", "-T"], protocol);
			//console.log(JSON.stringify(result, null, '\t'))
			should.deepEqual(result.output.steps, {
				1: {
					1: {comment: "Hello, World!"},
					2: {comment: "comment 2"},
					command: "system.call",
					name: "mySteps1",
					params: {name: "World"}
				}
			})
		});
	});

	describe('system.repeat', function () {

		it('should repeat `body`, `count` times', function () {
			var protocol = {
				steps: {
					"1": {
						command: "system.repeat",
						count: 2,
						steps: {comment: "empty"}
					}
				}
			};
			var result = roboliq.run(["-o", "", "-T"], protocol);
			should.deepEqual(result.output.steps[1], {
				command: "system.repeat",
				count: 2,
				steps: {comment: "empty"},
				1: {comment: "empty"},
				2: {comment: "empty"}
			});

			var protocol = {
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
			var result = roboliq.run(["-o", "", "-T"], protocol);
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
	});
});
