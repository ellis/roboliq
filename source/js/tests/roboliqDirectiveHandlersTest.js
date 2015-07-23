var roboliq = require('../roboliq.js')
var should = require('should');
var directiveHandlers = require('../config/roboliqDirectiveHandlers.js');
var misc = require('../misc.js');

var data = {
	directiveHandlers: directiveHandlers,
	objects: {}
};

describe('config/roboliqDirectiveHandlers', function() {

	it('should handle #factorialArrays', function() {
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

	it('should handle #factorialCols', function() {
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

	it('should handle #factorialMerge', function() {
		var spec = {"#factorialMerge": [
			[{x: 'a'}, {x: 'b'}, {x: 'c'}],
			[{n: 1}, {n: 2}],
			{q: "hello"}
		]};
		should.deepEqual(misc.handleDirective(spec, data), [
			{x: 'a', n: 1, q: 'hello'},
			{x: 'a', n: 2, q: 'hello'},
			{x: 'b', n: 1, q: 'hello'},
			{x: 'b', n: 2, q: 'hello'},
			{x: 'c', n: 1, q: 'hello'},
			{x: 'c', n: 2, q: 'hello'},
		]);
	});

	describe('#merge', function() {
		it('should generate object lists', function() {
			var spec = {"#merge": [
				{a: 'a', b: 1, c: 'hello'},
				{x: 'b', b: 2, q: 'hello'},
				{x: 'c', n: 3},
			]};
			should.deepEqual(misc.handleDirective(spec, data),
				{a: 'a', b: 2, c: 'hello', x: 'c', q: 'hello', n: 3}
			);
		});
	});

	describe('#tableCols', function () {
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

	describe('#tableRows', function () {
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
