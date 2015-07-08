var should = require('should');
var wellsParser = require('../parsers/wellsParser.js');

describe('wellsParser', function() {
	describe('parse(text)', function() {
		it('should parse "p"', function() {
			var result = wellsParser.parse("p");
			should.deepEqual(result, [{source: 'p'}]);
		})
		it('should parse "p(A1)"', function() {
			var result = wellsParser.parse("p(A1)");
			should.deepEqual(result, [{labware: 'p', subject: 'A01', phrases: []}]);
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
			var result = wellsParser.parse("p(A01,A02)");
			should.deepEqual(result, [
				{labware: 'p', subject: 'A01', phrases: []},
				{labware: 'p', subject: 'A02', phrases: []}
			]);
		})
		it('should parse "p(A01)+q"', function() {
			var result = wellsParser.parse("p(A01,A02)+q");
			should.deepEqual(result, [
				{labware: 'p', subject: 'A01', phrases: []},
				{labware: 'p', subject: 'A02', phrases: []},
				{source: 'q'}
			]);
		})
		it('should parse "p(A01 down 0)"', function() {
			var result = wellsParser.parse("p(A01 down 0)");
			should.deepEqual(result, [
				{labware: 'p', subject: 'A01', phrases: [['down', 0]]}
			]);
		})
	})
})
