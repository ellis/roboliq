var _ = require('lodash');
var should = require('should');
var roboliq = require('../src/roboliq.js')

describe('pipetter', function() {
	describe('pipetter.pipetteMixtures', function () {
		it("should pipette mixtures to destination wells", function() {
			var protocol = {
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
			var result = roboliq.run(["-o", ""], protocol);
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

			protocol.steps["1"].order = ["destination"];
			result = roboliq.run(["-o", ""], protocol);
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
	});
});
