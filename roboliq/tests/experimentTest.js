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
		}
	},
	predicates: [
		{"timer.canAgentEquipment": {agent: "robot1", equipment: "timer1"}},
		{"timer.canAgentEquipment": {agent: "robot1", equipment: "timer2"}},
	]
};

describe('experiment', function() {
	describe("experiment.run", function() {
		it("should handle an experiment array", function() {
			const protocol = _.merge({}, protocol0, {
				roboliq: "v1",
				objects: {
					experiment1: {
						type: "Variable",
						value: [
							{
								index: 1,
								wait: "1 minute"
							}
						]
					}
				},
				steps: {
					1: {
						command: "experiment.run",
						experiment: "experiment1",
						steps: {
							1: {
								command: "timer.sleep",
								duration: "SCOPE.wait"
							}
						}
					}
				}
			});
			var result = roboliq.run(["-o", "", "-T", "--no-ourlab"], protocol);
			//console.log(JSON.stringify(result, null, '\t'))
			should.deepEqual(result.output.steps, {
				1: {
					command: "experiment.run",
					experiment: "experiment1",
					steps: {
						1: {
							command: "timer.sleep",
							duration: "SCOPE.wait",
						}
					},
					1: {
						_scope: {
							index: 1,
							wait: "1 minute"
						},
						1: {
							command: "timer.sleep",
							duration: "SCOPE.wait",
							1: {
								command: "timer._sleep",
								agent: "robot1",
								equipment: "timer1",
								duration: "1 minute"
							}
						}
					}
				}
			})
		});
	});
});
