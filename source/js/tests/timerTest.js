var _ = require('lodash');
var should = require('should');
var roboliq = require('../roboliq.js')

describe('timer', function() {
	var protocol0 = {
		objects: {
			robot1: {
				type: "Agent"
			},
			timer1: {
				type: "Timer"
			},
			timer2: {
				type: "Timer"
			}
		},
		predicates: [
			{"timer.canAgentEquipment": {agent: "robot1", equipment: "timer1"}},
			{"timer.canAgentEquipment": {agent: "robot1", equipment: "timer2"}},
		]
	};

	describe('timer.start', function () {
		it('should emit a timer start instruction', function () {
			var protocol = _.merge({}, protocol0, {
				steps: {
					1: {
						command: "timer.start"
					}
				}
			});
			var result = roboliq.run(["-o", "", "-T", "-0"], protocol);
			should.deepEqual(result.output.steps[1][1], {
				command: "timer.instruction.start",
				agent: "robot1",
				equipment: "timer1"
			});
		});
	});

	describe('timer.stop', function () {
		it('should emit a timer stop instruction', function () {
			var protocol = _.merge({}, protocol0, {
				steps: {
					1: {
						command: "timer.stop"
					}
				},
			});
			protocol.predicates = protocol.predicates.concat([
				{running: {equipment: "timer2"}}
			]);
			var result = roboliq.run(["-o", "", "-T", "-0"], protocol);
			should.deepEqual(result.output.steps[1][1], {
				command: "timer.instruction.stop",
				agent: "robot1",
				equipment: "timer2"
			});
		});
	});

	describe('timer.doAndWait', function () {
		it('should emit a timer stop instruction', function () {
			var protocol = _.merge({}, protocol0, {
				steps: {
					1: {
						command: "timer.doAndWait",
						duration: 10,
						steps: {comment: "do something"}
					}
				}
			});
			var result = roboliq.run(["-o", "", "-T", "-0"], protocol);
			should.deepEqual(result.output.steps[1], {
				command: "timer.doAndWait",
				duration: 10,
				steps: {comment: "do something"},
				1: {
					command: "timer.instruction.start",
					agent: "robot1",
					equipment: "timer1"
				},
				2: {comment: "do something"},
				3: {
					command: "timer.instruction.stop",
					agent: "robot1",
					equipment: "timer1"
				},
			});
		});
	});
});
