var should = require('should');
var wellsParser = require('../parsers/wellsParser.js');

describe('wellsParser', function() {
	var objects = {
		p: {
			type: "Plate",
			model: "m"
		},
		q: {
			type: "Source",
			wells: ["p(A12)", "p(B12)"]
		},
		m: {
			type: "PlateModel",
			rows: 8,
			columns: 12
		}
	}
	describe('parse()', function() {
		var test1 = function(text, wells) {
			should.deepEqual(wellsParser.parse(text, objects), wells);
		}
		var test2 = function(text, result, wells) {
			should.deepEqual(wellsParser.parse(text), result);
			should.deepEqual(wellsParser.parse(text, objects), wells);
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
				['p(A01)', 'p(A02)', 'p(A03)', 'p(A04)', 'p(A05)', 'p(A06)', 'p(A07)', 'p(A08)', 'p(A09)', 'p(A10)', 'p(A11)', 'p(A12)', 'p(B01)', 'p(B02)', 'p(B03)', 'p(B04)', 'p(B05)', 'p(B06)', 'p(B07)', 'p(B08)', 'p(B09)', 'p(B10)', 'p(B11)', 'p(B12)', 'p(C01)', 'p(C02)', 'p(C03)', 'p(C04)', 'p(C05)', 'p(C06)', 'p(C07)', 'p(C08)', 'p(C09)', 'p(C10)', 'p(C11)', 'p(C12)', 'p(D01)', 'p(D02)', 'p(D03)', 'p(D04)', 'p(D05)', 'p(D06)', 'p(D07)', 'p(D08)', 'p(D09)', 'p(D10)', 'p(D11)', 'p(D12)', 'p(E01)', 'p(E02)', 'p(E03)', 'p(E04)', 'p(E05)', 'p(E06)', 'p(E07)', 'p(E08)', 'p(E09)', 'p(E10)', 'p(E11)', 'p(E12)', 'p(F01)', 'p(F02)', 'p(F03)', 'p(F04)', 'p(F05)', 'p(F06)', 'p(F07)', 'p(F08)', 'p(F09)', 'p(F10)', 'p(F11)', 'p(F12)', 'p(G01)', 'p(G02)', 'p(G03)', 'p(G04)', 'p(G05)', 'p(G06)', 'p(G07)', 'p(G08)', 'p(G09)', 'p(G10)', 'p(G11)', 'p(G12)', 'p(H01)', 'p(H02)', 'p(H03)', 'p(H04)', 'p(H05)', 'p(H06)', 'p(H07)', 'p(H08)', 'p(H09)', 'p(H10)', 'p(H11)', 'p(H12)']
			)
		})
		it('should parse "p(A01)+q"', function() {
			var text = "p(A01,A02)+q";
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
			var text = "p(A01 down 1)";
			should.deepEqual(wellsParser.parse(text), [
				{labware: 'p', subject: 'A01', phrases: [['down', 1]]}
			]);
			should.deepEqual(wellsParser.parse(text, objects), [
				"p(A01)"
			]);
		})
		it('should parse "p(A01 right 1)"', function() {
			var text = "p(A01 right 1)";
			should.deepEqual(wellsParser.parse(text), [
				{labware: 'p', subject: 'A01', phrases: [['right', 1]]}
			]);
			should.deepEqual(wellsParser.parse(text, objects), [
				"p(A01)"
			]);
		})
		it('should parse "p(A01 down 4)"', function() {
			var text = "p(A01 down 4)";
			should.deepEqual(wellsParser.parse(text), [
				{labware: 'p', subject: 'A01', phrases: [['down', 4]]}
			]);
			should.deepEqual(wellsParser.parse(text, objects), [
				"p(A01)", "p(B01)", "p(C01)", "p(D01)"
			]);
		})
		it('should parse "p(A01 down take 4)"', function() {
			var text = "p(A01 down take 4)";
			should.deepEqual(wellsParser.parse(text), [
				{labware: 'p', subject: 'A01', phrases: [['down', 4]]}
			]);
			should.deepEqual(wellsParser.parse(text, objects), [
				"p(A01)", "p(B01)", "p(C01)", "p(D01)"
			]);
		})
		it('should parse "p(A01 right 4)"', function() {
			var text = "p(A01 right 4)";
			should.deepEqual(wellsParser.parse(text), [
				{labware: 'p', subject: 'A01', phrases: [['right', 4]]}
			]);
			should.deepEqual(wellsParser.parse(text, objects), [
				"p(A01)", "p(A02)", "p(A03)", "p(A04)"
			]);
		})
		it('should parse "p(A01 right take 4)"', function() {
			var text = "p(A01 right take 4)";
			should.deepEqual(wellsParser.parse(text), [
				{labware: 'p', subject: 'A01', phrases: [['right', 4]]}
			]);
			should.deepEqual(wellsParser.parse(text, objects), [
				"p(A01)", "p(A02)", "p(A03)", "p(A04)"
			]);
		})
		it('should parse "p(A01 down 9)"', function() {
			var text = "p(A01 down 9)";
			should.deepEqual(wellsParser.parse(text, objects), [
				"p(A01)", "p(B01)", "p(C01)", "p(D01)", "p(E01)", "p(F01)", "p(G01)", "p(H01)", "p(A02)"
			]);
		})
		it('should parse "p(A01 right 13)"', function() {
			var text = "p(A01 right 13)";
			should.deepEqual(wellsParser.parse(text, objects), [
				"p(A01)", "p(A02)", "p(A03)", "p(A04)", "p(A05)", "p(A06)", "p(A07)", "p(A08)", "p(A09)", "p(A10)", "p(A11)", "p(A12)", "p(B01)"
			]);
		})
		it('should parse "p(A01 down C01)"', function() {
			var text = "p(A01 down C01)";
			should.deepEqual(wellsParser.parse(text), [
				{labware: 'p', subject: 'A01', phrases: [['down-to', "C01"]]}
			]);
			should.deepEqual(wellsParser.parse(text, objects), [
				"p(A01)", "p(B01)", "p(C01)"
			]);
		})
		it('should parse "p(A01 down to C01)"', function() {
			var text = "p(A01 down to C01)";
			should.deepEqual(wellsParser.parse(text, objects), [
				"p(A01)", "p(B01)", "p(C01)"
			]);
		})
		it('should parse "p(A01 right A03)"', function() {
			var text = "p(A01 right A03)";
			should.deepEqual(wellsParser.parse(text), [
				{labware: 'p', subject: 'A01', phrases: [['right-to', "A03"]]}
			]);
			should.deepEqual(wellsParser.parse(text, objects), [
				"p(A01)", "p(A02)", "p(A03)"
			]);
		})
		it('should parse "p(A01 right to A03)"', function() {
			var text = "p(A01 right to A03)";
			should.deepEqual(wellsParser.parse(text), [
				{labware: 'p', subject: 'A01', phrases: [['right-to', "A03"]]}
			]);
			should.deepEqual(wellsParser.parse(text, objects), [
				"p(A01)", "p(A02)", "p(A03)"
			]);
		})
		it('should parse "p(A01 down A02)"', function() {
			var text = "p(A01 down A02)";
			should.deepEqual(wellsParser.parse(text, objects), [
				"p(A01)", "p(B01)", "p(C01)", "p(D01)", "p(E01)", "p(F01)", "p(G01)", "p(H01)", "p(A02)"
			]);
		})
		it('should parse "p(A01 right B01)"', function() {
			var text = "p(A01 right B01)";
			should.deepEqual(wellsParser.parse(text, objects), [
				"p(A01)", "p(A02)", "p(A03)", "p(A04)", "p(A05)", "p(A06)", "p(A07)", "p(A08)", "p(A09)", "p(A10)", "p(A11)", "p(A12)", "p(B01)"
			]);
		})
		it('should parse "p(A01 down block C02)"', function() {
			var text = "p(A01 down block C02)";
			should.deepEqual(wellsParser.parse(text, objects), [
				"p(A01)", "p(B01)", "p(C01)", "p(A02)", "p(B02)", "p(C02)"
			]);
		})
		it('should parse "p(A01 right block C02)"', function() {
			var text = "p(A01 right block C02)";
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
				['p(C01)', 'p(B01)', 'p(D01)', 'p(A01)']
			)
		})
	})
})
