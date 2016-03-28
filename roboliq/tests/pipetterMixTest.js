import _ from 'lodash';
var should = require('should');
var roboliq = require('../src/roboliq.js')

describe('pipetter', function() {
	describe('pipetter.mix', function () {
		const protocolA = {
			roboliq: "v1",
			objects: {
				plate1: {
					type: "Plate",
					model: "ourlab.model.plateModel_96_square_transparent_nunc",
					location: "ourlab.mario.site.P2",
					contents: {
						A01: ['100ul', 'source1'],
						B01: ['100ul', 'source2']
					}
				},
				source1: {
					type: 'Liquid',
					wells: 'plate1(A01)'
				},
				source2: {
					type: 'Liquid',
					wells: 'plate1(B01)'
				},
			},
			steps: {
				"1": {
					command: "pipetter.mix",
					clean: 'none',
					wells: "plate1(A01 down B01)",
				}
			}
		};
		/*const protocolB = _.cloneDeep(protocolA);
		protocolB.steps["1"] = {
			command: "pipetter.pipetteMixtures",
			clean: 'none',
			mixtures: [
				{destination: "plate1(A02)", sources: [{source: 'source1', volume: '10ul'}, {source: 'source2', volume: '10ul'}]},
				{destination: "plate1(B02)", sources: [{source: 'source1', volume: '10ul'}, {source: 'source2', volume: '20ul'}]},
			]
		};
		const protocolC = _.cloneDeep(protocolA);
		protocolC.steps["1"] = {
			command: "pipetter.pipetteMixtures",
			clean: 'none',
			destinationLabware: "plate1",
			mixtures: [
				{destination: "A02", sources: [{source: 'source1', volume: '10ul'}, {source: 'source2', volume: '10ul'}]},
				{destination: "B02", sources: [{source: 'source1', volume: '10ul'}, {source: 'source2', volume: '20ul'}]},
			]
		};*/
		it("should mix some wells", function() {
			const protocol = protocolA;
			const result = roboliq.run(["-o", ""], protocol);
			// console.log(JSON.stringify(result.output.steps[1], null, '\t'))
			should.deepEqual(result.output.steps[1], {
				command: "pipetter.mix",
				clean: 'none',
				wells: "plate1(A01 down B01)",
				"1": {
					"command": "pipetter._mix",
					"agent": "ourlab.mario.evoware",
					"equipment": "ourlab.mario.liha",
					"program": "\"Roboliq_Water_Wet_1000\"",
					"count": 3,
					"volume": "70 ul",
					"items": [
						{
							"syringe": "ourlab.mario.liha.syringe.1",
							"destination": "plate1(A01)"
						},
						{
							"syringe": "ourlab.mario.liha.syringe.2",
							"destination": "plate1(B01)"
						}
					]
				},
			});
		});
	});
});
