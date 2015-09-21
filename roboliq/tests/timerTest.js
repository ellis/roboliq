var _ = require('lodash');
var should = require('should');
var roboliq = require('../src/roboliq.js')

describe('timer', function() {
	var protocol0 = {
		roboliq: "v1",
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

	describe('timer.sleep', function () {
		it('should emit a timer sleep instruction', function () {
			var protocol = _.merge({}, protocol0, {
				steps: {
					1: {
						command: "timer.sleep",
						duration: 10
					}
				}
			});
			var result = roboliq.run(["-o", "", "-T", "--no-ourlab"], protocol);
			should.deepEqual(result.output.steps[1][1], {
				command: "timer._sleep",
				agent: "robot1",
				equipment: "timer1",
				duration: 10
			});
		});
	});

	describe('timer.start', function () {
		it('should emit a timer start instruction', function () {
			var protocol = _.merge({}, protocol0, {
				steps: {
					1: {
						command: "timer.start"
					}
				}
			});
			var result = roboliq.run(["-o", "", "-T", "--no-ourlab"], protocol);
			should.deepEqual(result.output.steps[1][1], {
				command: "timer._start",
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
			var result = roboliq.run(["-o", "", "-T", "--no-ourlab"], protocol);
			should.deepEqual(result.output.steps[1][1], {
				command: "timer._stop",
				agent: "robot1",
				equipment: "timer2"
			});
		});
	});

	describe('timer.doAndWait', function () {
		it('should start a time, perform sub-steps, then wait', function () {
			var protocol = _.merge({}, protocol0, {
				steps: {
					1: {
						command: "timer.doAndWait",
						duration: 10,
						steps: {comment: "do something"}
					}
				}
			});
			var result = roboliq.run(["-o", "", "-T", "--no-ourlab"], protocol);
			should.deepEqual(result.output.steps[1], {
				command: "timer.doAndWait",
				duration: 10,
				steps: {comment: "do something"},
				1: {
					command: "timer._start",
					agent: "robot1",
					equipment: "timer1"
				},
				2: {comment: "do something"},
				3: {
					command: "timer._wait",
					agent: "robot1",
					equipment: "timer1",
					till: 10,
					stop: true
				},
			});
		});
	});
});
