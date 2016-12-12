import _ from 'lodash';
import should from 'should';
import roboliq from '../src/roboliq.js';

describe('commands/sealer', function() {
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

	describe('sealer.sealPlate', function () {
		it('should move plate to sealer, seal, then move plate back to original location', function () {
			const protocol = _.merge({}, protocol0, {
				steps: {
					1: {
						command: "sealer.sealPlate",
						object: "plate1",
					}
				}
			});
			const result = roboliq.run([__dirname+"/ourlab.js", "-o", "", "-T"], protocol, false);
			//console.log("result:\n"+JSON.stringify(result.output.steps, null, '\t'))
			should.deepEqual(result.output.steps,
				{
					"1": {
						"1": {
							"1": {
								"command": "transporter._movePlate",
								"agent": "ourlab.mario.evoware",
								"equipment": "ourlab.mario.roma2",
								"program": "Narrow",
								"object": "plate1",
								"destination": "ourlab.mario.site.ROBOSEAL"
							},
							"command": "transporter.movePlate",
							"object": "plate1",
							"destination": "ourlab.mario.site.ROBOSEAL"
						},
						"2": {
							"1": {
								"agent": "ourlab.mario.evoware",
								"command": "evoware._facts",
								"factsEquipment": "RoboSeal",
								"factsValue": "C:\\HJBioanalytikGmbH\\RoboSeal3\\RoboSeal_PlateParameters\\Greiner_384_schwarz.bcf",
								"factsVariable": "RoboSeal_Seal"
							},
							"command": "equipment.run|ourlab.mario.evoware|ourlab.mario.sealer",
							"agent": "ourlab.mario.evoware",
							"equipment": "ourlab.mario.sealer",
							"program": "C:\\HJBioanalytikGmbH\\RoboSeal3\\RoboSeal_PlateParameters\\Greiner_384_schwarz.bcf",
							"object": "plate1"
						},
						"3": {
							"1": {
								"command": "transporter._movePlate",
								"agent": "ourlab.mario.evoware",
								"equipment": "ourlab.mario.roma2",
								"program": "Narrow",
								"object": "plate1",
								"destination": "ourlab.mario.site.P2"
							},
							"command": "transporter.movePlate",
							"object": "plate1",
							"destination": "ourlab.mario.site.P2"
						},
						"command": "sealer.sealPlate",
						"object": "plate1"
					}
				}
			);
		});
	});
});
