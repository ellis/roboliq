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
	describe("experiment.run", function() {
		it("should manage without timing specifications", function() {
			const protocol = _.merge({}, protocol0, {
				roboliq: "v1",
				steps: {
					1: {
						command: "experiment.run",
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
			var result = roboliq.run(["-o", "", "-T", "--no-ourlab"], protocol);
			//console.log(JSON.stringify(result.output.steps, null, '\t'))
			should.deepEqual(result.output.steps, {
				"1": {
					"1": {
						"1": {
							"1": {
								"command": "system._echo",
								"value": "B1"
							},
							"command": "system.echo",
							"value": "$b"
						},
						"2": {
							"1": {
								"command": "system._echo",
								"value": ["A1", "A2"]
							},
							"command": "system.echo",
							"value": "$$a"
						},
						"@DATA": [ { "a": "A1", "b": "B1" }, { "a": "A2", "b": "B1" } ]
					},
					"2": {
						"1": {
							"1": {
								"command": "system._echo",
								"value": "B2"
							},
							"command": "system.echo",
							"value": "$b"
						},
						"2": {
							"1": {
								"command": "system._echo",
								"value": ["A1", "A2"]
							},
							"command": "system.echo",
							"value": "$$a"
						},
						"@DATA": [ { "a": "A1", "b": "B2" }, { "a": "A2", "b": "B2" } ]
					},
					"command": "experiment.run",
					"design": "design1",
					"groupBy": "b",
					"steps": {
						"1": {
							"command": "system.echo",
							"value": "$b"
						},
						"2": {
							"command": "system.echo",
							"value": "$$a"
						}
					}
				}
			});
		});

		it.only("should manage with duration", function() {
			const protocol = _.merge({}, protocol0, {
				roboliq: "v1",
				steps: {
					1: {
						command: "experiment.run",
						design: "design1",
						groupBy: "b",
						duration: "1 minute",
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
			var result = roboliq.run(["-o", "", "-T", "--no-ourlab"], protocol);
			console.log(JSON.stringify(result.output.steps, null, '\t'))
			should.deepEqual(result.output.steps, {
				"1": {
					"0": {
						"command": "timer.start",
						"equipment": {
							"type": "Timer"
						}
					},
					"1": {
						"1": {
							"1": {
								"command": "system._echo",
								"value": [
									"A1",
									"A2"
								]
							},
							"command": "system.echo",
							"value": "$$a"
						},
						"@DATA": [ { "a": "A1", "b": "B1" }, { "a": "A2", "b": "B1" } ]
					},
					"2": {
						"1": {
							"1": {
								"command": "system._echo",
								"value": [
									"A1",
									"A2"
								]
							},
							"command": "system.echo",
							"value": "$$a"
						},
						"@DATA": [ { "a": "A1", "b": "B2" }, { "a": "A2", "b": "B2" } ]
					},
					"3": {
						"command": "timer.wait",
						"equipment": {
							"type": "Timer"
						},
						"till": "1 minute",
						"stop": true
					},
					"command": "experiment.run",
					"design": "design1",
					"groupBy": "b",
					"duration": "1 minute",
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
	});
});
