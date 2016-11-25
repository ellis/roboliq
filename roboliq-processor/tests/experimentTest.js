import _ from 'lodash';
import should from 'should';
import experiment from '../src/commands/experiment.js';
import roboliq from '../src/roboliq.js';

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
			type: "Design",
			conditions: {
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

describe('experiment', function() {
	describe("experiment.forEachGroup", function() {
		it("should manage without timing specifications", function() {
			const protocol = _.merge({}, protocol0, {
				roboliq: "v1",
				steps: {
					1: {
						command: "experiment.forEachGroup",
						design: "design1",
						groupBy: "b",
						steps: {
							1: {
								command: "system.echo",
								value: "$b"
							},
							2: {
								command: "system.echo",
								value: "$$a"
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
					"command": "experiment.forEachGroup",
					"design": "design1",
					"groupBy": "b",
					"steps": {
						"1": { "command": "system.echo", "value": "$b" },
						"2": { "command": "system.echo", "value": "$$a" }
					}
				}
			});
		});

		it("should handle a description template as an argument to steps", function() {
			const protocol = _.merge({}, protocol0, {
				roboliq: "v1",
				steps: {
					1: {
						command: "experiment.forEachGroup",
						design: "design1",
						groupBy: "b",
						steps: {
							description: "`Echo {{$b}}`",
							1: {
								command: "system.echo",
								value: "$b"
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
					"command": "experiment.forEachGroup",
					"design": "design1",
					"groupBy": "b",
					"steps": {
						description: "`Echo {{$b}}`",
						"1": { "command": "system.echo", "value": "$b" },
					}
				}
			});
		});

		it("should manage with durationTotal", function() {
			const protocol = _.merge({}, protocol0, {
				roboliq: "v1",
				steps: {
					1: {
						command: "experiment.forEachGroup",
						design: "design1",
						groupBy: "b",
						durationTotal: "1 minute",
						timers: ["timer1", "timer2"],
						steps: {
							1: {
								command: "system.echo",
								value: "$$a"
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
					"command": "experiment.forEachGroup",
					"design": "design1",
					"groupBy": "b",
					"durationTotal": "1 minute",
					"timers": [
						"timer1",
						"timer2"
					],
					"steps": {
						"1": {
							"command": "system.echo",
							"value": "$$a"
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
						command: "experiment.forEachGroup",
						design: "design1",
						groupBy: "b",
						durationGroup: "1 minute",
						timers: ["timer1", "timer2"],
						steps: {
							1: {
								command: "system.echo",
								value: "$$a"
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
				"command": "experiment.forEachGroup",
				"design": "design1",
				"groupBy": "b",
				"durationGroup": "1 minute",
				"timers": [
					"timer1",
					"timer2"
				],
				"steps": {
					"1": {
						"command": "system.echo",
						"value": "$$a"
					}
				}
			});
		});

		it.skip("should handle timing with durationTotal and startTimerAfterStep", function() {

		});

		// it("should manage without timing specifications", function() {
		// 	const protocol = _.merge({}, protocol0, {
		// 		roboliq: "v1",
		// 		steps: {
		// 			1: {
		// 				command: "experiment.forEachGroup",
		// 				design: "design1",
		// 				groupBy: "b",
		// 				steps: {
		// 					description: "`Group for B={{$b}}`",
		// 					1: {
		// 						command: "system._echo",
		// 						value: "$$a"
		// 					}
		// 				}
		// 			}
		// 		}
		// 	});
		// 	var result = roboliq.run(["-o", "", "-T", "--no-ourlab"], protocol, false);
		// 	//console.log(JSON.stringify(result.output.steps["1"], null, '\t'))
		// 	should.deepEqual(result.output.steps["1"], {
		// 		command: "experiment.forEachGroup",
		// 		design: "design1",
		// 		groupBy: "b",
		// 		steps: {
		// 			description: "`Group for B={{$b}}`",
		// 			1: {
		// 				command: "system._echo",
		// 				value: "$$a"
		// 			}
		// 		},
		// 		1: {
		// 			'@DATA': [ { a: 'A1', b: 'B1' }, { a: 'A2', b: 'B1' } ],
		// 			description: "Group for B=B1",
		// 			1: {
		// 				command: "system._echo",
		// 				value: ["A1", "A2"]
		// 			}
		// 		},
		// 		2: {
		// 			'@DATA': [ { a: 'A1', b: 'B2' }, { a: 'A2', b: 'B2' } ],
		// 			description: "Group for B=B2",
		// 			1: {
		// 				command: "system._echo",
		// 				value: ["A1", "A2"]
		// 			}
		// 		}
		// 	});
		// });
	});

	describe("experiment.forEachRow", function() {
		it("should manage without timing specifications", function() {
			const protocol = _.merge({}, protocol0, {
				roboliq: "v1",
				steps: {
					1: {
						command: "experiment.forEachRow",
						design: "design1",
						steps: {
							command: "system.echo",
							value: "`{{$a}} {{$b}}`"
						}
					}
				}
			});
			var result = roboliq.run(["-o", "", "-T", "--no-ourlab"], protocol, false);
			//console.log(JSON.stringify(result.output.steps["1"], null, '\t'))
			should.deepEqual(result.output.steps["1"], {
				command: "experiment.forEachRow",
				design: "design1",
				steps: {
					command: "system.echo",
					value: "`{{$a}} {{$b}}`"
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
