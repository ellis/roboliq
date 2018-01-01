/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017, ETH Zurich, Ellis Whitehead
 * @license GPL-3.0
 */

const _ = require('lodash');
const should = require('should');
// const experiment = require('../src/commands/data.js');
const roboliq = require('../src/roboliq.js');

const protocol0 = {
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
		},
		design1: {
			type: "Data",
			design: {
				"a*": ["A1", "A2"],
				"b*": ["B1", "B2"]
			}
		}
	},
	predicates: [
		{"timer.canAgentEquipment": {agent: "robot1", equipment: "timer1"}},
		{"timer.canAgentEquipment": {agent: "robot1", equipment: "timer2"}},
	]
};

describe('data', function() {
	describe("data.forEachGroup", function() {
		it("should manage without timing specifications", function() {
			const protocol = _.merge({}, protocol0, {
				roboliq: "v1",
				steps: {
					1: {
						data: "design1",
						command: "data.forEachGroup",
						groupBy: "b",
						steps: {
							1: {
								command: "system.echo",
								value: "$b_ONE"
							},
							2: {
								command: "system.echo",
								value: "$a"
							}
						}
					}
				}
			});
			var result = roboliq.run(["-o", "", "-T", "--no-ourlab"], protocol, false);
			//console.log(JSON.stringify(result.output.steps, null, '\t'))
			should.deepEqual(result.output.steps, {
				"1": {
					"1": {
						"1": {
							"1": { "command": "system._echo", "value": "B1" },
							"command": "system.echo",
							"value": "B1"
						},
						"2": {
							"1": { "command": "system._echo", "value": ["A1", "A2"] },
							"command": "system.echo",
							"value": ["A1", "A2"]
						},
						"@DATA": [ { "a": "A1", "b": "B1" }, { "a": "A2", "b": "B1" } ]
					},
					"2": {
						"1": {
							"1": { "command": "system._echo", "value": "B2" },
							"command": "system.echo",
							"value": "B2"
						},
						"2": {
							"1": { "command": "system._echo", "value": ["A1", "A2"] },
							"command": "system.echo",
							"value": ["A1", "A2"]
						},
						"@DATA": [ { "a": "A1", "b": "B2" }, { "a": "A2", "b": "B2" } ]
					},
					"command": "data.forEachGroup",
					"data": "design1",
					"groupBy": "b",
					"steps": {
						"1": { "command": "system.echo", "value": "$b_ONE" },
						"2": { "command": "system.echo", "value": "$a" }
					}
				}
			});
		});

		it("should handle a description template as an argument to steps", function() {
			const protocol = _.merge({}, protocol0, {
				roboliq: "v1",
				steps: {
					1: {
						data: "design1",
						command: "data.forEachGroup",
						groupBy: "b",
						steps: {
							description: "`Echo {{b_ONE}}`",
							1: {
								command: "system.echo",
								value: "$b_ONE"
							}
						}
					}
				}
			});
			var result = roboliq.run(["-o", "", "-T", "--no-ourlab"], protocol, false);
			//console.log(JSON.stringify(result.output.steps, null, '\t'))
			should.deepEqual(result.output.steps, {
				"1": {
					"1": {
						description: "Echo B1",
						"1": {
							"1": { "command": "system._echo", "value": "B1" },
							"command": "system.echo",
							"value": "B1"
						},
						"@DATA": [ { "a": "A1", "b": "B1" }, { "a": "A2", "b": "B1" } ]
					},
					"2": {
						description: "Echo B2",
						"1": {
							"1": { "command": "system._echo", "value": "B2" },
							"command": "system.echo",
							"value": "B2"
						},
						"@DATA": [ { "a": "A1", "b": "B2" }, { "a": "A2", "b": "B2" } ]
					},
					"command": "data.forEachGroup",
					"data": "design1",
					"groupBy": "b",
					"steps": {
						description: "`Echo {{b_ONE}}`",
						"1": { "command": "system.echo", "value": "$b_ONE" },
					}
				}
			});
		});

		it("should manage with durationTotal", function() {
			const protocol = _.merge({}, protocol0, {
				roboliq: "v1",
				steps: {
					1: {
						command: "data.forEachGroup",
						data: "design1",
						groupBy: "b",
						durationTotal: "1 minute",
						timers: ["timer1", "timer2"],
						steps: {
							1: {
								command: "system.echo",
								value: "$a"
							}
						}
					}
				}
			});
			var result = roboliq.run(["-o", "", "-T", "--no-ourlab"], protocol, false);
			//console.log(JSON.stringify(result.output.steps, null, '\t'))
			should.deepEqual(result.output.steps, {
				"1": {
					"0": {
						"1": {
							"command": "timer._start",
							"agent": "robot1",
							"equipment": "timer1"
						},
						"command": "timer.start",
						"equipment": "timer1"
					},
					"1": {
						"1": {
							"command": "system._echo",
							"value": [ "A1", "A2" ]
						},
						"command": "system.echo",
						"value": [ "A1", "A2" ],
						"@DATA": [ { "a": "A1", "b": "B1" }, { "a": "A2", "b": "B1" } ]
					},
					"2": {
						"1": {
							"command": "system._echo",
							"value": [ "A1", "A2" ]
						},
						"command": "system.echo",
						"value": [ "A1", "A2" ],
						"@DATA": [ { "a": "A1", "b": "B2" }, { "a": "A2", "b": "B2" } ]
					},
					"3": {
						"1": {
							"command": "timer._wait",
							"till": "1 minute",
							"stop": true,
							"agent": "robot1",
							"equipment": "timer1"
						},
						"command": "timer.wait",
						"equipment": "timer1",
						"till": "1 minute",
						"stop": true
					},
					"command": "data.forEachGroup",
					"data": "design1",
					"groupBy": "b",
					"durationTotal": "1 minute",
					"timers": [
						"timer1",
						"timer2"
					],
					"steps": {
						"1": {
							"command": "system.echo",
							"value": "$a"
						}
					}
				}
			});
		});

		it("should manage with durationGroup", function() {
			const protocol = _.merge({}, protocol0, {
				roboliq: "v1",
				steps: {
					1: {
						command: "data.forEachGroup",
						data: "design1",
						groupBy: "b",
						durationGroup: "1 minute",
						timers: ["timer1", "timer2"],
						steps: {
							1: {
								command: "system.echo",
								value: "$a"
							}
						}
					}
				}
			});
			var result = roboliq.run(["-o", "", "-T", "--no-ourlab"], protocol, false);
			//console.log(JSON.stringify(result.output.steps["1"], null, '\t'))
			should.deepEqual(result.output.steps["1"], {
				"1": {
					"1": {
						"command": "timer._start",
						"agent": "robot1",
						"equipment": "timer1"
					},
					"2": {
						"1": {
							"command": "system._echo",
							"value": [ "A1", "A2" ]
						},
						"command": "system.echo",
						"value": ["A1", "A2"],
						"@DATA": [ { "a": "A1", "b": "B1" }, { "a": "A2", "b": "B1" } ]
					},
					"3": {
						"command": "timer._wait",
						"agent": "robot1",
						"equipment": "timer1",
						"till": 60,
						"stop": true
					},
					"command": "timer.doAndWait",
					"equipment": "timer1",
					"duration": "1 minute",
					"steps": {
						"command": "system.echo",
						"value": ["A1", "A2"],
						"@DATA": [ { "a": "A1", "b": "B1" }, { "a": "A2", "b": "B1" } ]
					}
				},
				"2": {
					"1": {
						"command": "timer._start",
						"agent": "robot1",
						"equipment": "timer1"
					},
					"2": {
						"1": {
							"command": "system._echo",
							"value": [ "A1", "A2" ]
						},
						"command": "system.echo",
						"value": ["A1", "A2"],
						"@DATA": [ { "a": "A1", "b": "B2" }, { "a": "A2", "b": "B2" } ]
					},
					"3": {
						"command": "timer._wait",
						"agent": "robot1",
						"equipment": "timer1",
						"till": 60,
						"stop": true
					},
					"command": "timer.doAndWait",
					"equipment": "timer1",
					"duration": "1 minute",
					"steps": {
						"command": "system.echo",
						"value": ["A1", "A2"],
						"@DATA": [ { "a": "A1", "b": "B2" }, { "a": "A2", "b": "B2" } ]
					}
				},
				"command": "data.forEachGroup",
				"data": "design1",
				"groupBy": "b",
				"durationGroup": "1 minute",
				"timers": [
					"timer1",
					"timer2"
				],
				"steps": {
					"1": {
						"command": "system.echo",
						"value": "$a"
					}
				}
			});
		});

		it.skip("should handle timing with durationTotal and startTimerAfterStep", function() {

		});
	});

	describe("data.forEachRow", function() {
		it("should manage without timing specifications", function() {
			const protocol = _.merge({}, protocol0, {
				roboliq: "v1",
				steps: {
					1: {
						data: "design1",
						command: "data.forEachRow",
						steps: {
							command: "system.echo",
							value: "`{{a_ONE}} {{b}}`"
						}
					}
				}
			});
			var result = roboliq.run(["-o", "", "-T", "--no-ourlab"], protocol, false);
			//console.log(JSON.stringify(result.output.steps["1"], null, '\t'))
			should.deepEqual(result.output.steps["1"], {
				data: "design1",
				command: "data.forEachRow",
				steps: {
					command: "system.echo",
					value: "`{{a_ONE}} {{b}}`"
				},
				1: {
					'@DATA': [ { a: 'A1', b: 'B1' } ],
					command: "system.echo",
					value: "A1 B1",
					1: { command: "system._echo", value: "A1 B1" }
				},
				2: {
					'@DATA': [ { a: 'A1', b: 'B2' } ],
					command: "system.echo",
					value: "A1 B2",
					1: { command: "system._echo", value: "A1 B2" }
				},
				3: {
					'@DATA': [ { a: 'A2', b: 'B1' } ],
					command: "system.echo",
					value: "A2 B1",
					1: { command: "system._echo", value: "A2 B1" }
				},
				4: {
					'@DATA': [ { a: 'A2', b: 'B2' } ],
					command: "system.echo",
					value: "A2 B2",
					1: { command: "system._echo", value: "A2 B2" }
				}
			});
		});
	});
});
