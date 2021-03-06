/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017, ETH Zurich, Ellis Whitehead
 * @license GPL-3.0
 */

const _ = require('lodash');
const should = require('should');
const roboliq = require('../src/roboliq.js')

describe('pipetter', function() {
	describe('pipetter.pipetteMixtures', function () {
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
					command: "pipetter.pipetteMixtures",
					clean: 'none',
					mixtures: [
						[{source: 'source1', volume: '10ul'}, {source: 'source2', volume: '10ul'}],
						[{source: 'source1', volume: '10ul'}, {source: 'source2', volume: '20ul'}],
					],
					destinations: "plate1(A02 down to D02)"
				}
			}
		};
		const protocolB = _.cloneDeep(protocolA);
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
		};
		it("should pipette 2D mixture array to destination wells", function() {
			const protocol = protocolA;
			const result = roboliq.run([__dirname+"/ourlab.js", "-o", ""], protocol, false);
			// console.log(JSON.stringify(result.output.steps, null, '\t'))
			should.deepEqual(_.omit(result.output.steps[1][1], "1"), {
				command: "pipetter.pipette",
				clean: 'none',
				items: [
					{source: 'source1', volume: '10ul', destination: 'plate1(A02)'},
					{source: 'source1', volume: '10ul', destination: 'plate1(B02)'},
					{source: 'source2', volume: '10ul', destination: 'plate1(A02)'},
					{source: 'source2', volume: '20ul', destination: 'plate1(B02)'},
				]
			});
			// TODO: need to test for dry dispense with source1 and wet dispense with source2
		});
		it("should pipette mixtures to destination wells", function() {
			const protocol = protocolB;
			const result = roboliq.run([__dirname+"/ourlab.js", "-o", ""], protocol, false);
			should.deepEqual(_.omit(result.output.steps[1][1], "1"), {
				command: "pipetter.pipette",
				clean: 'none',
				items: [
					{source: 'source1', volume: '10ul', destination: 'plate1(A02)'},
					{source: 'source1', volume: '10ul', destination: 'plate1(B02)'},
					{source: 'source2', volume: '10ul', destination: 'plate1(A02)'},
					{source: 'source2', volume: '20ul', destination: 'plate1(B02)'},
				]
			});
		});
		it("should pipette mixtures to destination wells, with destinationLabware specified separately", function() {
			const protocol = protocolC;
			const result = roboliq.run([__dirname+"/ourlab.js", "-o", ""], protocol, false);
			should.deepEqual(_.omit(result.output.steps[1][1], "1"), {
				command: "pipetter.pipette",
				clean: 'none',
				destinationLabware: "plate1",
				items: [
					{source: 'source1', volume: '10ul', destination: 'A02'},
					{source: 'source1', volume: '10ul', destination: 'B02'},
					{source: 'source2', volume: '10ul', destination: 'A02'},
					{source: 'source2', volume: '20ul', destination: 'B02'},
				]
			});
		});
		it("should handle order=destination", function() {
			const protocol = _.merge({}, protocolA, {
				steps: {"1": {order: ["destination"]}}
			});
			const result = roboliq.run([__dirname+"/ourlab.js", "-o", ""], protocol, false);
			should.deepEqual(_.omit(result.output.steps[1][1], "1"), {
				command: "pipetter.pipette",
				clean: 'none',
				items: [
					{source: 'source1', volume: '10ul', destination: 'plate1(A02)'},
					{source: 'source2', volume: '10ul', destination: 'plate1(A02)'},
					{source: 'source1', volume: '10ul', destination: 'plate1(B02)'},
					{source: 'source2', volume: '20ul', destination: 'plate1(B02)'},
				]
			});
			//console.log(JSON.stringify(result.output.tables.wellContentsFinal, null, '\t'));
			should.deepEqual(_.get(result, "output.tables.wellContentsFinal"),
				[
					{well: 'ourlab.mario.systemLiquidLabware', systemLiquid: 'Infinity l'},
					{"well": "plate1(A01)", "source1": "80 ul"},
					{"well": "plate1(B01)", "source2": "70 ul"},
					{"well": "plate1(A02)", "source1": "10 ul", "source2": "10 ul"},
					{"well": "plate1(B02)", "source1": "10 ul", "source2": "20 ul"}
				]
			)
		});

		/*it.only("should translate calibration data to volumes", () => {
			const protocol = _.cloneDeep(protocolA);
			protocol.steps["1"] = {
				command: "pipetter.pipetteMixtures",
				clean: 'none',
				calibration: {
					absorbance: {
						calibrationVariable: "absorbance",
						calibrationData: [
							{absorbance: 0.5, volume: "50ul"},
							{absorbance: 1.5, volume: "150ul"},
						]
					}
				},
				mixtures: [
					{destination: "plate1(A02)", sources: [{source: 'source1', absorbance: 1.0}, {source: 'source2'}], volume: "175ul"},
				],
			};
			const result = roboliq.run([__dirname+"/ourlab.js", "-o", ""], protocol, false);
			console.log(JSON.stringify(result.output.steps[1][1], null, '\t'));
			should.deepEqual(_.omit(result.output.steps[1][1], "1"), {
				command: "pipetter.pipette",
				clean: 'none',
				items: [
					{source: 'source1', volume: '100ul', destination: 'plate1(A02)'},
					{source: 'source2', volume: '75ul', destination: 'plate1(A02)'},
				]
			});
		});*/

	});
});
