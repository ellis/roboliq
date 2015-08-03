var _ = require('lodash');
var should = require('should');
var roboliq = require('../roboliq.js')

describe('system', function() {
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
