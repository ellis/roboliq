import _ from 'lodash';
import should from 'should';
import roboliq from '../src/roboliq.js';

describe('commands/shaker', function() {
	const protocol0 = {
		roboliq: "v1",
		objects: {
			plate1: {
				type: "Plate",
				model: "ourlab.model.plateModel_384_square",
				location: "ourlab.mario.site.P2"
			}
		}
	};

	describe('shaker.shakePlate', function () {
		it('should move plate to shaker, seal, then move plate back to original location', function () {
			const protocol = _.merge({}, protocol0, {
				steps: {
					1: {
						command: "shaker.shakePlate",
						object: "plate1",
						program: {
							duration: "1 minute"
						}
					}
				}
			});
			const result = roboliq.run(["-o", "", "-T"], protocol);
			//console.log("result:\n"+JSON.stringify(result.output.steps, null, '\t'))
			should.deepEqual(result.output.steps, {
				"1": {
					"command": "shaker.shakePlate",
					"object": "plate1",
					"program": {
						"duration": "1 minute"
					},
					"1": {
						"command": "transporter.movePlate",
						"object": "plate1",
						"destination": "ourlab.mario.site.P3",
						"1": {
							"command": "transporter._movePlate",
							"agent": "ourlab.mario.evoware",
							"equipment": "ourlab.mario.roma1",
							"program": "Narrow",
							"object": "plate1",
							"destination": "ourlab.mario.site.P3"
						},
					},
					"2": {
						"command": "shaker.run|ourlab.mario.evoware|ourlab.mario.shaker",
						"agent": "ourlab.mario.evoware",
						"equipment": "ourlab.mario.shaker",
						"program": {
							"duration": "1 minute"
						},
						"object": "plate1",
						"1": {
							"command": "evoware._facts",
							"agent": "ourlab.mario.evoware",
							"factsEquipment": "HPShaker",
							"factsVariable": "HPShaker_HP__Start",
							"factsValue": "*271|8*30*30*30*30|2|*30|1|*30|7,5*30|127*27"
						},
						"2": {
							"1": {
								"command": "timer._sleep",
								"duration": "1 minute",
								"agent": "ourlab.mario.evoware",
								"equipment": "ourlab.mario.timer1"
							},
							"command": "timer.sleep",
							"agent": "ourlab.mario.evoware",
							"duration": "1 minute"
						},
						"3": {
							"command": "evoware._facts",
							"agent": "ourlab.mario.evoware",
							"factsEquipment": "HPShaker",
							"factsVariable": "HPShaker_HP__Stop",
							"factsValue": "1"
						}
					},
					"3": {
						"command": "transporter.movePlate",
						"object": "plate1",
						"destination": "ourlab.mario.site.P2",
						"1": {
							"command": "transporter._movePlate",
							"agent": "ourlab.mario.evoware",
							"equipment": "ourlab.mario.roma1",
							"program": "Narrow",
							"object": "plate1",
							"destination": "ourlab.mario.site.P2"
						},
					},
				}
			});
		});
	});
});
