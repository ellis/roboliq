/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017 Ellis Whitehead
 * @license GPL-3.0
 */

const should = require('should');
const wellsParser = require('../src/parsers/wellsParser.js');

describe('wellsParser', function() {
	const objects = {
		p: {
			type: "Plate",
			model: "m96"
		},
		q: {
			type: "Source",
			wells: ["p(A12)", "p(B12)"]
		},
		r: {
			type: "Plate",
			model: "m384"
		},
		m96: {
			type: "PlateModel",
			rows: 8,
			columns: 12
		},
		m384: {
			type: "PlateModel",
			rows: 16,
			columns: 24
		},
		vp: {
			type: "Variable",
			value: "p"
		},
		vq: {
			type: "Variable",
			value: "q"
		},
		vw: {
			type: "Variable",
			value: "p(A01)"
		},
		vw2: {
			type: "Variable",
			value: "vw"
		}
	}
	describe('parse()', function() {
		const testA = function(text, result) {
			should.deepEqual(wellsParser.parse(text), result);
		}
		const test1 = function(text, wells) {
			should.deepEqual(wellsParser.parse(text, objects), wells);
		}
		const test2 = function(text, result, wells, config) {
			should.deepEqual(wellsParser.parse(text), result);
			should.deepEqual(wellsParser.parse(text, objects, config), wells);
		}
		it('should parse sources', function() {
			test2("q",
				[{source: 'q'}],
				[["p(A12)", "p(B12)"]]
			)
		})
		it('should parse individual wells', function() {
			test2("p(A1)",
				[{labware: 'p', subject: 'A01', phrases: []}],
				["p(A01)"]
			)
			test2("p( A1 )",
				[{labware: 'p', subject: 'A01', phrases: []}],
				["p(A01)"]
			)
			test2("p(A01)",
				[{labware: 'p', subject: 'A01', phrases: []}],
				["p(A01)"]
			)
			test2("p(A001)",
				[{labware: 'p', subject: 'A01', phrases: []}],
				["p(A01)"]
			)
			test2("p(A111)",
				[{labware: 'p', subject: 'A111', phrases: []}],
				["p(A111)"]
			)
			test2("p(A01,A02)",
				[
					{labware: 'p', subject: 'A01', phrases: []},
					{labware: 'p', subject: 'A02', phrases: []}
				],
				["p(A01)", "p(A02)"]
			)
			test2("p(A01, A02)",
				[
					{labware: 'p', subject: 'A01', phrases: []},
					{labware: 'p', subject: 'A02', phrases: []}
				],
				["p(A01)", "p(A02)"]
			)
		})
		it('should parse `all` subjects', function() {
			test2("p(all)",
				[{labware: 'p', subject: 'all', phrases: []}],
				["p(A01)","p(B01)","p(C01)","p(D01)","p(E01)","p(F01)","p(G01)","p(H01)","p(A02)","p(B02)","p(C02)","p(D02)","p(E02)","p(F02)","p(G02)","p(H02)","p(A03)","p(B03)","p(C03)","p(D03)","p(E03)","p(F03)","p(G03)","p(H03)","p(A04)","p(B04)","p(C04)","p(D04)","p(E04)","p(F04)","p(G04)","p(H04)","p(A05)","p(B05)","p(C05)","p(D05)","p(E05)","p(F05)","p(G05)","p(H05)","p(A06)","p(B06)","p(C06)","p(D06)","p(E06)","p(F06)","p(G06)","p(H06)","p(A07)","p(B07)","p(C07)","p(D07)","p(E07)","p(F07)","p(G07)","p(H07)","p(A08)","p(B08)","p(C08)","p(D08)","p(E08)","p(F08)","p(G08)","p(H08)","p(A09)","p(B09)","p(C09)","p(D09)","p(E09)","p(F09)","p(G09)","p(H09)","p(A10)","p(B10)","p(C10)","p(D10)","p(E10)","p(F10)","p(G10)","p(H10)","p(A11)","p(B11)","p(C11)","p(D11)","p(E11)","p(F11)","p(G11)","p(H11)","p(A12)","p(B12)","p(C12)","p(D12)","p(E12)","p(F12)","p(G12)","p(H12)"]
			)
		})
		it('should parse "p(A01)+q"', function() {
			const text = "p(A01,A02)+q";
			should.deepEqual(wellsParser.parse(text), [
				{labware: 'p', subject: 'A01', phrases: []},
				{labware: 'p', subject: 'A02', phrases: []},
				{source: 'q'}
			]);
			should.deepEqual(wellsParser.parse(text, objects), [
				"p(A01)", "p(A02)", ["p(A12)", "p(B12)"]
			]);
		})
		it('should parse "p(A01 down 1)"', function() {
			const text = "p(A01 down 1)";
			should.deepEqual(wellsParser.parse(text), [
				{labware: 'p', subject: 'A01', phrases: [['down', 1]]}
			]);
			should.deepEqual(wellsParser.parse(text, objects), [
				"p(A01)"
			]);
		})
		it('should parse "p(A01 right 1)"', function() {
			const text = "p(A01 right 1)";
			should.deepEqual(wellsParser.parse(text), [
				{labware: 'p', subject: 'A01', phrases: [['right', 1]]}
			]);
			should.deepEqual(wellsParser.parse(text, objects), [
				"p(A01)"
			]);
		})
		it('should parse "p(A01 down 4)"', function() {
			const text = "p(A01 down 4)";
			should.deepEqual(wellsParser.parse(text), [
				{labware: 'p', subject: 'A01', phrases: [['down', 4]]}
			]);
			should.deepEqual(wellsParser.parse(text, objects), [
				"p(A01)", "p(B01)", "p(C01)", "p(D01)"
			]);
		})
		it('should parse "p(A01 down take 4)"', function() {
			const text = "p(A01 down take 4)";
			should.deepEqual(wellsParser.parse(text), [
				{labware: 'p', subject: 'A01', phrases: [['down', 4]]}
			]);
			should.deepEqual(wellsParser.parse(text, objects), [
				"p(A01)", "p(B01)", "p(C01)", "p(D01)"
			]);
		})
		it('should parse "p(A01 right 4)"', function() {
			const text = "p(A01 right 4)";
			should.deepEqual(wellsParser.parse(text), [
				{labware: 'p', subject: 'A01', phrases: [['right', 4]]}
			]);
			should.deepEqual(wellsParser.parse(text, objects), [
				"p(A01)", "p(A02)", "p(A03)", "p(A04)"
			]);
		})
		it('should parse "p(A01 right take 4)"', function() {
			const text = "p(A01 right take 4)";
			should.deepEqual(wellsParser.parse(text), [
				{labware: 'p', subject: 'A01', phrases: [['right', 4]]}
			]);
			should.deepEqual(wellsParser.parse(text, objects), [
				"p(A01)", "p(A02)", "p(A03)", "p(A04)"
			]);
		})
		it('should parse "p(A01 down 9)"', function() {
			const text = "p(A01 down 9)";
			should.deepEqual(wellsParser.parse(text, objects), [
				"p(A01)", "p(B01)", "p(C01)", "p(D01)", "p(E01)", "p(F01)", "p(G01)", "p(H01)", "p(A02)"
			]);
		})
		it('should parse "p(A01 right 13)"', function() {
			const text = "p(A01 right 13)";
			should.deepEqual(wellsParser.parse(text, objects), [
				"p(A01)", "p(A02)", "p(A03)", "p(A04)", "p(A05)", "p(A06)", "p(A07)", "p(A08)", "p(A09)", "p(A10)", "p(A11)", "p(A12)", "p(B01)"
			]);
		})
		it('should parse "p(A01 down C01)"', function() {
			const text = "p(A01 down C01)";
			should.deepEqual(wellsParser.parse(text), [
				{labware: 'p', subject: 'A01', phrases: [['down-to', "C01"]]}
			]);
			should.deepEqual(wellsParser.parse(text, objects), [
				"p(A01)", "p(B01)", "p(C01)"
			]);
		})
		it('should parse "p(A01 down to C01)"', function() {
			const text = "p(A01 down to C01)";
			should.deepEqual(wellsParser.parse(text, objects), [
				"p(A01)", "p(B01)", "p(C01)"
			]);
		})
		it('should parse "p(A01 right A03)"', function() {
			const text = "p(A01 right A03)";
			should.deepEqual(wellsParser.parse(text), [
				{labware: 'p', subject: 'A01', phrases: [['right-to', "A03"]]}
			]);
			should.deepEqual(wellsParser.parse(text, objects), [
				"p(A01)", "p(A02)", "p(A03)"
			]);
		})
		it('should parse "p(A01 right to A03)"', function() {
			const text = "p(A01 right to A03)";
			should.deepEqual(wellsParser.parse(text), [
				{labware: 'p', subject: 'A01', phrases: [['right-to', "A03"]]}
			]);
			should.deepEqual(wellsParser.parse(text, objects), [
				"p(A01)", "p(A02)", "p(A03)"
			]);
		})
		it('should parse "p(A01 down A02)"', function() {
			const text = "p(A01 down A02)";
			should.deepEqual(wellsParser.parse(text, objects), [
				"p(A01)", "p(B01)", "p(C01)", "p(D01)", "p(E01)", "p(F01)", "p(G01)", "p(H01)", "p(A02)"
			]);
		})
		it('should parse "p(A01 right B01)"', function() {
			const text = "p(A01 right B01)";
			should.deepEqual(wellsParser.parse(text, objects), [
				"p(A01)", "p(A02)", "p(A03)", "p(A04)", "p(A05)", "p(A06)", "p(A07)", "p(A08)", "p(A09)", "p(A10)", "p(A11)", "p(A12)", "p(B01)"
			]);
		})
		it('should parse "p(A01 down block C02)"', function() {
			const text = "p(A01 down block C02)";
			should.deepEqual(wellsParser.parse(text, objects), [
				"p(A01)", "p(B01)", "p(C01)", "p(A02)", "p(B02)", "p(C02)"
			]);
		})
		it('should parse "p(A01 right block C02)"', function() {
			const text = "p(A01 right block C02)";
			should.deepEqual(wellsParser.parse(text, objects), [
				"p(A01)", "p(A02)", "p(B01)", "p(B02)", "p(C01)", "p(C02)"
			]);
		})
		it('should parse `random` phrases', function() {
			test2("p(A01 down 4 random(0))",
				[{labware: 'p', subject: 'A01', phrases: [['down', 4], ['random', 0]]}],
				['p(C01)', 'p(B01)', 'p(D01)', 'p(A01)']
			)
			test2("p(all random(0) take 4)",
				[{labware: 'p', subject: 'all', phrases: [['random', 0], ['take', 4]]}],
				['p(C09)', 'p(A06)', 'p(B04)', 'p(E10)']
			)
		})
		it('should parse `row-jump` phrases', function() {
			test2("p(A01 down 4 row-jump(1))",
				[{labware: 'p', subject: 'A01', phrases: [['down', 4], ['row-jump', 1]]}],
				['p(A01)', 'p(C01)', 'p(B01)', 'p(D01)']
			)
			test2("p(A01 down block E02 row-jump(1))",
				[{labware: 'p', subject: 'A01', phrases: [['down-block', 'E02'], ['row-jump', 1]]}],
				['p(A01)', 'p(C01)', 'p(E01)', 'p(B01)', 'p(D01)', 'p(A02)', 'p(C02)', 'p(E02)', 'p(B02)', 'p(D02)']
			)
			test1("r(all row-jump(1) take 2)",
				['r(A01)', 'r(C01)']
			)
		});
		it('should parse variable pointing to source', function() {
			test2("vq",
				[{source: 'vq'}],
				[["p(A12)", "p(B12)"]]
			);
		});
		it('should parse variable pointing to plate', function() {
			test2("vp(A1)",
				[{labware: 'vp', subject: 'A01', phrases: []}],
				["p(A01)"]
			);
		});
		it('should parse variable representing individual wells', function() {
			test2("vw",
				[{source: 'vw'}],
				["p(A01)"]
			);
		});
		it('should parse variable that indirectly represents individual wells', function() {
			test2("vw2",
				[{source: 'vw2'}],
				["p(A01)"]
			);
		});
		it('should catch error when missing source', function() {
			should.throws(() => wellsParser.parse("missing", objects), "Something");
		});
		it('should parse "A01"', function() {
			test2("A01",
				[{subject: "A01", phrases: []}],
				["A01"]
			);
		});
		it('should parse "A01+B01"', function() {
			test2("A01+B01",
				[{subject: "A01", phrases: []}, {subject: "B01", phrases: []}],
				["A01", "B01"]
			);
		});
		it('should parse "A01 right B01"', function() {
			test2("A01 right B01",
				[{subject: "A01", phrases: [["right-to", "B01"]]}],
				["A01", "A02", "A03", "A04", "A05", "A06", "A07", "A08", "A09", "A10", "A11", "A12", "B01"],
				{rows: 8, columns: 12}
			);
		});
	});
});
