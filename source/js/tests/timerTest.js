var _ = require('lodash');
var should = require('should');
var roboliq = require('../roboliq.js')

describe('timer', function() {
	describe('timer.start', function () {

		it('should emit a timer start instruction', function () {
			var protocol = {
				objects: {
					robot1: {
						type: "Agent"
					},
					timer1: {
						type: "Timer"
					}
				},
				steps: {
					1: {
						command: "timer.start"
					}
				},
				predicates: [
					{"timer.canAgentEquipment": {agent: "robot1", equipment: "timer1"}}
				]
			};
			var result = roboliq.run(["-o", "", "-T", "-0"], protocol);
			should.deepEqual(result.output.steps[1][1], {
				command: "timer.instruction.start",
				agent: "robot1",
				equipment: "timer1"
			});
		});
	});
});
