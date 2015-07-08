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
	describe('parse(text)', function() {
		it('should parse "p"', function() {
			var result = wellsParser.parse("p");
			should.deepEqual(result, [{source: 'p'}]);
		})
		it('should parse "p(A1)"', function() {
			var text = "p(A1)";
			should.deepEqual(wellsParser.parse(text), [{labware: 'p', subject: 'A01', phrases: []}]);
			should.deepEqual(wellsParser.parse(text, objects), ["p(A01)"]);
		})
		it('should parse "p(A01)"', function() {
			var result = wellsParser.parse("p(A01)");
			should.deepEqual(result, [{labware: 'p', subject: 'A01', phrases: []}]);
		})
		it('should parse "p(A001)"', function() {
			var result = wellsParser.parse("p(A001)");
			should.deepEqual(result, [{labware: 'p', subject: 'A01', phrases: []}]);
		})
		it('should parse "p(A111)"', function() {
			var result = wellsParser.parse("p(A111)");
			should.deepEqual(result, [{labware: 'p', subject: 'A111', phrases: []}]);
		})
		it('should parse "p(A01,A02)"', function() {
			var text = "p(A01,A02)";
			var result = wellsParser.parse(text);
			should.deepEqual(result, [
				{labware: 'p', subject: 'A01', phrases: []},
				{labware: 'p', subject: 'A02', phrases: []}
			]);
			should.deepEqual(wellsParser.parse(text, objects), ["p(A01)", "p(A02)"]);
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
		it('should parse "p(A01 down 0)"', function() {
			var text = "p(A01 down 0)";
			should.deepEqual(wellsParser.parse(text), [
				{labware: 'p', subject: 'A01', phrases: [['down', 0]]}
			]);
			should.deepEqual(wellsParser.parse(text, objects), [
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
		it('should parse "p(A01 down 4 right 2)"', function() {
			var text = "p(A01 down 4)";
			should.deepEqual(wellsParser.parse(text), [
				{labware: 'p', subject: 'A01', phrases: [['down', 4]]}
			]);
			should.deepEqual(wellsParser.parse(text, objects), [
				"p(A01)", "p(B01)", "p(C01)", "p(D01)"
			]);
		})
	})
})
