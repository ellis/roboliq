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
			/*
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
					command: "timer.instruction.wait",
					agent: "robot1",
					equipment: "timer1",
					till: 10,
					stop: true
				},
			});
			*/
		});
	});
});
