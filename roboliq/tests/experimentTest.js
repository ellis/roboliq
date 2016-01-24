import _ from 'lodash';
import should from 'should';
import experiment from '../src/commands/experiment.js';
import roboliq from '../src/roboliq.js';

describe('experiment', function() {
	describe("experiment.run", function() {
		it("should handle an experiment array", function() {
			var protocol = {
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
			};
			var result = roboliq.run(["-o", "", "-T"], protocol);
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
						1: {
							command: "timer.sleep",
							duration: "1 minute",
							1: {
								command: "timer._sleep",
								agent: "robot1",
								equipment: "timer1",
								duration: 60
							}
						}
					}
				}
			})
		});
	});
});
