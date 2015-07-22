var roboliq = require('../roboliq.js')
var should = require('should');
var directiveHandlers = require('../config/roboliqDirectiveHandlers.js');
var misc = require('../misc.js');

var data = {
	directiveHandlers: directiveHandlers,
	objects: {}
};

describe('config/roboliqDirectiveHandlers', function() {

	describe('factorialArrays', function() {
		it('should generate object lists', function() {
			var spec = {"#factorialArrays": [
				{source: 1},
				[{source: 2}, {source: 3}],
				[{source: 4}, {source: 5}]
			]};
			should.deepEqual(misc.handleDirective(spec, data), [
				[{source: 1}, {source: 2}, {source: 4}],
				[{source: 1}, {source: 2}, {source: 5}],
				[{source: 1}, {source: 3}, {source: 4}],
				[{source: 1}, {source: 3}, {source: 5}],
			]);
		});
	});

	describe('factorialCols', function() {
		it('should generate object lists', function() {
			var spec = {"#factorialCols": {
				x: ['a', 'b', 'c'],
				n: [1, 2],
				q: "hello"
			}};
			should.deepEqual(misc.handleDirective(spec, data), [
				{x: 'a', n: 1, q: 'hello'},
				{x: 'a', n: 2, q: 'hello'},
				{x: 'b', n: 1, q: 'hello'},
				{x: 'b', n: 2, q: 'hello'},
				{x: 'c', n: 1, q: 'hello'},
				{x: 'c', n: 2, q: 'hello'},
			]);
		});
	});

	describe('tableCols', function () {
		it('should generate object lists', function () {
			var spec = {"#tableCols": {
				x: ['a', 'b', 'c'],
				n: [1, 2, 3],
				q: "hello"
			}};
			should.deepEqual(misc.handleDirective(spec, data), [
				{x: 'a', n: 1, q: 'hello'},
				{x: 'b', n: 2, q: 'hello'},
				{x: 'c', n: 3, q: 'hello'},
			]);
		});
	});

	describe('tableRows', function () {
		it('should generate object lists', function () {
			var spec = {"#tableRows": [
				{x: 'X', y: 'Y'},
				['A', 'B', 'C'],
				['a', 'b', 1],
				{y: 'Z'},
				['c', 'd', 2],
			]};
			should.deepEqual(misc.handleDirective(spec, data), [
				{x: 'X', y: 'Y', A: 'a', B: 'b', C: 1},
				{x: 'X', y: 'Z', A: 'c', B: 'd', C: 2},
			]);
		});
	});
});
