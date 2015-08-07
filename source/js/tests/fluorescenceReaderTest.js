var _ = require('lodash');
var should = require('should');
var roboliq = require('../roboliq.js')

describe('commands/fluorescenceReader', function() {
	var protocol0 = {
		objects: {
			plate1: {
				type: "Plate",
				model: "ourlab.model.plateModel_384_square",
				location: "ourlab.mario.site.P2"
			}
		}
	};

	describe('fluorescenceReader.measurePlate', function () {
		it('should move plate to reader, measure, then move plate back to original location', function () {
			var protocol = _.merge({}, protocol0, {
				steps: {
					1: {
						command: "fluorescenceReader.measurePlate",
						object: "plate1",
						program: {
							programFile: "tania13_ph-temp.mdfx"
						},
						outputFile: "C:\\Users\\localadmin\\Desktop\\Ellis\\tania13_ph--<YYYYMMDD_HHmmss>.xml"
					}
				}
			});
			var result = roboliq.run(["-o", "", "-T"], protocol);
			//console.log("result:\n"+JSON.stringify(result, null, '\t'))
			should.deepEqual(result.protocol.errors, {});
			should.deepEqual(result.protocol.warnings, {});
			//console.log("result:\n"+JSON.stringify(result.output.steps, null, '\t'))
			should.deepEqual(result.output.steps[1],
				{
					"1": {
						"1": {
							"command": "transporter._movePlate",
							"agent": "ourlab.mario.evoware",
							"equipment": "ourlab.mario.roma1",
							"program": "Narrow",
							"object": "plate1",
							"destination": "ourlab.mario.site.REGRIP"
						},
						"2": {
							"command": "equipment._openSite",
							"agent": "ourlab.mario.evoware",
							"equipment": "ourlab.mario.reader",
							"site": "ourlab.mario.site.READER"
						},
						"3": {
							"command": "transporter._movePlate",
							"agent": "ourlab.mario.evoware",
							"equipment": "ourlab.mario.roma2",
							"program": "Wide",
							"object": "plate1",
							"destination": "ourlab.mario.site.READER"
						},
						"4": {
							"command": "equipment._close",
							"agent": "ourlab.mario.evoware",
							"equipment": "ourlab.mario.reader"
						},
						"command": "transporter.movePlate",
						"object": "plate1",
						"destination": "ourlab.mario.site.READER"
					},
					"2": {
						"command": "fluorescenceReader._run",
						"agent": "ourlab.mario.evoware",
						"equipment": "ourlab.mario.reader",
						"program": {
							"programFile": "tania13_ph-temp.mdfx"
						},
						"object": "plate1"
					},
					"3": {
						"1": {
							"command": "equipment._openSite",
							"agent": "ourlab.mario.evoware",
							"equipment": "ourlab.mario.reader",
							"site": "ourlab.mario.site.READER"
						},
						"2": {
							"command": "transporter._movePlate",
							"agent": "ourlab.mario.evoware",
							"equipment": "ourlab.mario.roma2",
							"program": "Wide",
							"object": "plate1",
							"destination": "ourlab.mario.site.REGRIP"
						},
						"3": {
							"command": "equipment._close",
							"agent": "ourlab.mario.evoware",
							"equipment": "ourlab.mario.reader"
						},
						"4": {
							"command": "transporter._movePlate",
							"agent": "ourlab.mario.evoware",
							"equipment": "ourlab.mario.roma1",
							"program": "Narrow",
							"object": "plate1",
							"destination": "ourlab.mario.site.P2"
						},
						"command": "transporter.movePlate",
						"object": "plate1",
						"destination": "ourlab.mario.site.P2"
					},
					"command": "fluorescenceReader.measurePlate",
					"object": "plate1",
					"program": {
						"programFile": "tania13_ph-temp.mdfx"
					},
					"outputFile": "C:\\Users\\localadmin\\Desktop\\Ellis\\tania13_ph--<YYYYMMDD_HHmmss>.xml"
				}
			);
		});
	});
});
