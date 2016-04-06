import _ from 'lodash';
import should from 'should';
import roboliq from '../src/roboliq.js';

describe('pipetter', function() {
	describe('pipetter.pipetteDilutionSeries', function() {
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
					command: "pipetter.pipetteDilutionSeries",
					clean: 'none',
					diluent: "ourlab.mario.systemLiquid",
					destinationLabware: "plate1",
					items: [
						{destinations: ["A01", "A02", "A03", "A04", "A05"], syringe: "ourlab.mario.liha.syringe.1"},
						{destinations: ["B01", "B02", "B03", "B04", "B05"], syringe: "ourlab.mario.liha.syringe.2"}
					]
				}
			}
		};
		it("should pipette 2D mixture array to destination wells", function() {
			const protocol = protocolA;
			const result = roboliq.run(["-o", ""], protocol);
			// console.log(JSON.stringify(result.output.steps[1], null, '\t'))
			should.deepEqual(_.pick(result.output.steps[1], [1, 2, 3]), {
				"1": {
					"1": {
						"1": {
							"1": {
								"command": "pipetter._washTips",
								"agent": "ourlab.mario.evoware",
								"equipment": "ourlab.mario.liha",
								"program": "ourlab.mario.washProgram.thorough_1000",
								"intensity": "thorough",
								"syringes": [
									"ourlab.mario.liha.syringe.1",
									"ourlab.mario.liha.syringe.2",
									"ourlab.mario.liha.syringe.3",
									"ourlab.mario.liha.syringe.4"
								]
							},
							"command": "pipetter.cleanTips|ourlab.mario.evoware|ourlab.mario.liha",
							"agent": "ourlab.mario.evoware",
							"equipment": "ourlab.mario.liha",
							"items": [
								{
									"syringe": "ourlab.mario.liha.syringe.1",
									"intensity": "thorough"
								},
								{
									"syringe": "ourlab.mario.liha.syringe.2",
									"intensity": "thorough"
								}
							]
						},
						"command": "pipetter.cleanTips",
						"agent": "ourlab.mario.evoware",
						"equipment": "ourlab.mario.liha",
						"items": [
							{
								"syringe": "ourlab.mario.liha.syringe.1",
								"intensity": "thorough"
							},
							{
								"syringe": "ourlab.mario.liha.syringe.2",
								"intensity": "thorough"
							}
						]
					},
					"2": {
						"command": "pipetter._pipette",
						"agent": "ourlab.mario.evoware",
						"equipment": "ourlab.mario.liha",
						"program": "\"Roboliq_Water_Dry_1000\"",
						"items": [
							{
								"syringe": "ourlab.mario.liha.syringe.1",
								"source": "ourlab.mario.systemLiquidLabware(A01)",
								"destination": "plate1(A02)",
								"volume": "50 ul"
							},
							{
								"syringe": "ourlab.mario.liha.syringe.2",
								"source": "ourlab.mario.systemLiquidLabware(B01)",
								"destination": "plate1(B02)",
								"volume": "50 ul"
							}
						]
					},
					"3": {
						"command": "pipetter._pipette",
						"agent": "ourlab.mario.evoware",
						"equipment": "ourlab.mario.liha",
						"program": "\"Roboliq_Water_Dry_1000\"",
						"items": [
							{
								"syringe": "ourlab.mario.liha.syringe.1",
								"source": "ourlab.mario.systemLiquidLabware(A01)",
								"destination": "plate1(A03)",
								"volume": "50 ul"
							},
							{
								"syringe": "ourlab.mario.liha.syringe.2",
								"source": "ourlab.mario.systemLiquidLabware(B01)",
								"destination": "plate1(B03)",
								"volume": "50 ul"
							}
						]
					},
					"4": {
						"command": "pipetter._pipette",
						"agent": "ourlab.mario.evoware",
						"equipment": "ourlab.mario.liha",
						"program": "\"Roboliq_Water_Dry_1000\"",
						"items": [
							{
								"syringe": "ourlab.mario.liha.syringe.1",
								"source": "ourlab.mario.systemLiquidLabware(A01)",
								"destination": "plate1(A04)",
								"volume": "50 ul"
							},
							{
								"syringe": "ourlab.mario.liha.syringe.2",
								"source": "ourlab.mario.systemLiquidLabware(B01)",
								"destination": "plate1(B04)",
								"volume": "50 ul"
							}
						]
					},
					"5": {
						"command": "pipetter._pipette",
						"agent": "ourlab.mario.evoware",
						"equipment": "ourlab.mario.liha",
						"program": "\"Roboliq_Water_Dry_1000\"",
						"items": [
							{
								"syringe": "ourlab.mario.liha.syringe.1",
								"source": "ourlab.mario.systemLiquidLabware(A01)",
								"destination": "plate1(A05)",
								"volume": "50 ul"
							},
							{
								"syringe": "ourlab.mario.liha.syringe.2",
								"source": "ourlab.mario.systemLiquidLabware(B01)",
								"destination": "plate1(B05)",
								"volume": "50 ul"
							}
						]
					},
					"destinationLabware": "plate1",
					"command": "pipetter.pipette",
					"items": [
						{
							"layer": 1,
							"source": "ourlab.mario.systemLiquid",
							"destination": "plate1(A02)",
							"volume": "50 ul",
							"syringe": "ourlab.mario.liha.syringe.1"
						},
						{
							"layer": 2,
							"source": "ourlab.mario.systemLiquid",
							"destination": "plate1(A03)",
							"volume": "50 ul",
							"syringe": "ourlab.mario.liha.syringe.1"
						},
						{
							"layer": 3,
							"source": "ourlab.mario.systemLiquid",
							"destination": "plate1(A04)",
							"volume": "50 ul",
							"syringe": "ourlab.mario.liha.syringe.1"
						},
						{
							"layer": 4,
							"source": "ourlab.mario.systemLiquid",
							"destination": "plate1(A05)",
							"volume": "50 ul",
							"syringe": "ourlab.mario.liha.syringe.1"
						},
						{
							"layer": 1,
							"source": "ourlab.mario.systemLiquid",
							"destination": "plate1(B02)",
							"volume": "50 ul",
							"syringe": "ourlab.mario.liha.syringe.2"
						},
						{
							"layer": 2,
							"source": "ourlab.mario.systemLiquid",
							"destination": "plate1(B03)",
							"volume": "50 ul",
							"syringe": "ourlab.mario.liha.syringe.2"
						},
						{
							"layer": 3,
							"source": "ourlab.mario.systemLiquid",
							"destination": "plate1(B04)",
							"volume": "50 ul",
							"syringe": "ourlab.mario.liha.syringe.2"
						},
						{
							"layer": 4,
							"source": "ourlab.mario.systemLiquid",
							"destination": "plate1(B05)",
							"volume": "50 ul",
							"syringe": "ourlab.mario.liha.syringe.2"
						}
					],
					"cleanBetweenSameSource": "none",
					"cleanEnd": "none"
				},
				"2": {
					"1": {
						"command": "pipetter._pipette",
						"agent": "ourlab.mario.evoware",
						"equipment": "ourlab.mario.liha",
						"program": "\"Roboliq_Water_Wet_1000\"",
						"items": [
							{
								"syringe": "ourlab.mario.liha.syringe.1",
								"source": "plate1(A01)",
								"destination": "plate1(A02)",
								"volume": "50 ul",
								"sourceMixing": {
									"count": 3,
									"amount": 0.7
								},
								"destinationMixing": {
									"count": 3,
									"amount": 0.7
								}
							},
							{
								"syringe": "ourlab.mario.liha.syringe.2",
								"source": "plate1(B01)",
								"destination": "plate1(B02)",
								"volume": "50 ul",
								"sourceMixing": {
									"count": 3,
									"amount": 0.7
								},
								"destinationMixing": {
									"count": 3,
									"amount": 0.7
								}
							}
						]
					},
					"2": {
						"command": "pipetter._pipette",
						"agent": "ourlab.mario.evoware",
						"equipment": "ourlab.mario.liha",
						"program": "\"Roboliq_Water_Wet_1000\"",
						"items": [
							{
								"syringe": "ourlab.mario.liha.syringe.1",
								"source": "plate1(A02)",
								"destination": "plate1(A03)",
								"volume": "50 ul",
								"destinationMixing": {
									"count": 3,
									"amount": 0.7
								}
							},
							{
								"syringe": "ourlab.mario.liha.syringe.2",
								"source": "plate1(B02)",
								"destination": "plate1(B03)",
								"volume": "50 ul",
								"destinationMixing": {
									"count": 3,
									"amount": 0.7
								}
							}
						]
					},
					"3": {
						"command": "pipetter._pipette",
						"agent": "ourlab.mario.evoware",
						"equipment": "ourlab.mario.liha",
						"program": "\"Roboliq_Water_Wet_1000\"",
						"items": [
							{
								"syringe": "ourlab.mario.liha.syringe.1",
								"source": "plate1(A03)",
								"destination": "plate1(A04)",
								"volume": "50 ul",
								"destinationMixing": {
									"count": 3,
									"amount": 0.7
								}
							},
							{
								"syringe": "ourlab.mario.liha.syringe.2",
								"source": "plate1(B03)",
								"destination": "plate1(B04)",
								"volume": "50 ul",
								"destinationMixing": {
									"count": 3,
									"amount": 0.7
								}
							}
						]
					},
					"4": {
						"command": "pipetter._pipette",
						"agent": "ourlab.mario.evoware",
						"equipment": "ourlab.mario.liha",
						"program": "\"Roboliq_Water_Wet_1000\"",
						"items": [
							{
								"syringe": "ourlab.mario.liha.syringe.1",
								"source": "plate1(A04)",
								"destination": "plate1(A05)",
								"volume": "50 ul",
								"destinationMixing": {
									"count": 3,
									"amount": 0.7
								}
							},
							{
								"syringe": "ourlab.mario.liha.syringe.2",
								"source": "plate1(B04)",
								"destination": "plate1(B05)",
								"volume": "50 ul",
								"destinationMixing": {
									"count": 3,
									"amount": 0.7
								}
							}
						]
					},
					"5": {
						"command": "pipetter._pipette",
						"agent": "ourlab.mario.evoware",
						"equipment": "ourlab.mario.liha",
						"program": "\"Roboliq_Water_Wet_1000\"",
						"items": [
							{
								"syringe": "ourlab.mario.liha.syringe.1",
								"source": "plate1(A05)",
								"volume": "50 ul"
							},
							{
								"syringe": "ourlab.mario.liha.syringe.2",
								"source": "plate1(B05)",
								"volume": "50 ul"
							}
						]
					},
					"6": {
						"1": {
							"1": {
								"command": "pipetter._washTips",
								"agent": "ourlab.mario.evoware",
								"equipment": "ourlab.mario.liha",
								"program": "ourlab.mario.washProgram.thorough_1000",
								"intensity": "thorough",
								"syringes": [
									"ourlab.mario.liha.syringe.1",
									"ourlab.mario.liha.syringe.2",
									"ourlab.mario.liha.syringe.3",
									"ourlab.mario.liha.syringe.4"
								]
							},
							"command": "pipetter.cleanTips|ourlab.mario.evoware|ourlab.mario.liha",
							"agent": "ourlab.mario.evoware",
							"equipment": "ourlab.mario.liha",
							"items": [
								{
									"syringe": "ourlab.mario.liha.syringe.1",
									"intensity": "thorough"
								},
								{
									"syringe": "ourlab.mario.liha.syringe.2",
									"intensity": "thorough"
								}
							]
						},
						"command": "pipetter.cleanTips",
						"agent": "ourlab.mario.evoware",
						"equipment": "ourlab.mario.liha",
						"items": [
							{
								"syringe": "ourlab.mario.liha.syringe.1",
								"intensity": "thorough"
							},
							{
								"syringe": "ourlab.mario.liha.syringe.2",
								"intensity": "thorough"
							}
						]
					},
					"destinationLabware": "plate1",
					"command": "pipetter.pipette",
					"items": [
						{
							"layer": 1,
							"source": "plate1(A01)",
							"destination": "plate1(A02)",
							"volume": "50 ul",
							"syringe": "ourlab.mario.liha.syringe.1",
							"sourceMixing": true
						},
						{
							"layer": 2,
							"source": "plate1(A02)",
							"destination": "plate1(A03)",
							"volume": "50 ul",
							"syringe": "ourlab.mario.liha.syringe.1"
						},
						{
							"layer": 3,
							"source": "plate1(A03)",
							"destination": "plate1(A04)",
							"volume": "50 ul",
							"syringe": "ourlab.mario.liha.syringe.1"
						},
						{
							"layer": 4,
							"source": "plate1(A04)",
							"destination": "plate1(A05)",
							"volume": "50 ul",
							"syringe": "ourlab.mario.liha.syringe.1"
						},
						{
							"layer": 5,
							"source": "plate1(A05)",
							"volume": "50 ul",
							"syringe": "ourlab.mario.liha.syringe.1"
						},
						{
							"layer": 1,
							"source": "plate1(B01)",
							"destination": "plate1(B02)",
							"volume": "50 ul",
							"syringe": "ourlab.mario.liha.syringe.2",
							"sourceMixing": true
						},
						{
							"layer": 2,
							"source": "plate1(B02)",
							"destination": "plate1(B03)",
							"volume": "50 ul",
							"syringe": "ourlab.mario.liha.syringe.2"
						},
						{
							"layer": 3,
							"source": "plate1(B03)",
							"destination": "plate1(B04)",
							"volume": "50 ul",
							"syringe": "ourlab.mario.liha.syringe.2"
						},
						{
							"layer": 4,
							"source": "plate1(B04)",
							"destination": "plate1(B05)",
							"volume": "50 ul",
							"syringe": "ourlab.mario.liha.syringe.2"
						},
						{
							"layer": 5,
							"source": "plate1(B05)",
							"volume": "50 ul",
							"syringe": "ourlab.mario.liha.syringe.2"
						}
					],
					"cleanBegin": "none",
					"cleanBetweenSameSource": "none",
					"cleanBetween": "none",
					"destinationMixing": true
				}
			});
		});
	});
});
