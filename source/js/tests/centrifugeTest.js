var _ = require('lodash');
var should = require('should');
var roboliq = require('../roboliq.js')

describe('centrifgue', function() {
	var protocol0 = {
		objects: {
			plate1: {
				type: "Plate",
				model: "ourlab.model.plateModel_384_square",
				location: "ourlab.mario.site.P2"
			},
			plate2: {
				type: "Plate",
				model: "ourlab.model.plateModel_384_square",
				location: "ourlab.mario.site.P3"
			}
		}
	};

	describe('centrifuge.centrifuge2', function () {
		it('should put two plates in the centrifuge, run, then move the plates back', function () {
			var protocol = _.merge({}, protocol0, {
				steps: {
					1: {
						command: "centrifuge.centrifuge2",
						object1: "plate1",
						object2: "plate2",
						program: {
							rpm: 3000,
							duration: 120,
							temperature: 25
						}
					}
				}
			});
			var result = roboliq.run(["-o", "", "-T"], protocol);
			console.log("result:\n"+JSON.stringify(result.output.steps, null, '\t'))
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
					command: "timer.instruction.wait",
					agent: "robot1",
					equipment: "timer1",
					till: 10,
					stop: true
				},
			});
		});
	});
});
