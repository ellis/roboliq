/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017, ETH Zurich, Ellis Whitehead
 * @license GPL-3.0
 */

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
						duration: "10s"
					}
				}
			});
			var result = roboliq.run(["-o", "", "-T", "--no-ourlab"], protocol, false);
			should.deepEqual(result.output.steps[1][1], {
				command: "timer._sleep",
				agent: "robot1",
				equipment: "timer1",
				duration: "10 s"
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
			var result = roboliq.run(["-o", "", "-T", "--no-ourlab"], protocol, false);
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
			var result = roboliq.run(["-o", "", "-T", "--no-ourlab"], protocol, false);
			should.deepEqual(result.output.steps[1][1], {
				command: "timer._stop",
				agent: "robot1",
				equipment: "timer2"
			});
		});
	});

	describe('timer.doAndWait', function () {
		var protocol1 = {
			steps: {
				1: {
					command: "timer.doAndWait",
					duration: 1800,
					steps: {comment: "do something"}
				}
			}
		};
		var expected1 = {
			command: "timer.doAndWait",
			duration: 1800,
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
				till: 1800,
				stop: true
			},
		};

		it('should start a time, perform sub-steps, then wait', function () {
			var protocol = _.merge({}, protocol0, protocol1);
			var result = roboliq.run(["-o", "", "-T", "--no-ourlab"], protocol, false);
			should.deepEqual(result.output.steps[1], expected1);
		});
		it('should support different time units', function () {
			var protocol2 = _.merge({}, protocol0, protocol1, {steps: {1: {duration: "0.5h"}}});
			var expected2 = _.merge({}, expected1, {duration: "0.5h"});
			var result2 = roboliq.run(["-o", "", "-T", "--no-ourlab"], protocol2, false);
			should.deepEqual(result2.output.steps[1], expected2);

			var protocol3 = _.merge({}, protocol0, protocol1, {steps: {1: {duration: "30minutes"}}});
			var expected3 = _.merge({}, expected1, {duration: "30minutes"});
			var result3 = roboliq.run(["-o", "", "-T", "--no-ourlab"], protocol3, false);
			should.deepEqual(result3.output.steps[1], expected3);
		});
		it('should handle specification of explicit timer equipment', function () {
			var protocol2 = _.merge({}, protocol0, protocol1, {steps: {1: {equipment: "timer1"}}});
			var expected2 = _.merge({}, expected1, {equipment: "timer1"});
			var result2 = roboliq.run(["-o", "", "-T", "--no-ourlab"], protocol2, false);
			should.deepEqual(result2.output.steps[1], expected2);
		});
	});
});
