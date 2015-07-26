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

	it('should handle #for', function() {
		// Equivalent to #factorialArrays
		var spec1 = {"#for": {
			factors: [
				{a: {source: 1}},
				{b: [{source: 2}, {source: 3}]},
				{c: [{source: 4}, {source: 5}]}
			],
			output: ["${a}", "${b}", "${c}"]
		}};
		should.deepEqual(misc.handleDirective(spec1, data), [
			[{source: 1}, {source: 2}, {source: 4}],
			[{source: 1}, {source: 2}, {source: 5}],
			[{source: 1}, {source: 3}, {source: 4}],
			[{source: 1}, {source: 3}, {source: 5}],
		]);
		// Equivalent to #factorialCols
		var spec2 = {"#for": {
			factors: [
				{x: ['a', 'b', 'c']},
				{n: [1, 2]},
				{q: "hello"}
			],
			output: {x2: "${x}", n2: "${n}", q2: "${q}"}
		}};
		should.deepEqual(misc.handleDirective(spec2, data), [
			{x2: 'a', n2: 1, q2: 'hello'},
			{x2: 'a', n2: 2, q2: 'hello'},
			{x2: 'b', n2: 1, q2: 'hello'},
			{x2: 'b', n2: 2, q2: 'hello'},
			{x2: 'c', n2: 1, q2: 'hello'},
			{x2: 'c', n2: 2, q2: 'hello'},
		]);
		// Use object syntax for 'variables' instead of an array
		var spec3 = {"#for": {
			factors: {
				x: ['a', 'b', 'c'],
				n: [1, 2],
				q: "hello"
			},
			output: {x2: "${x}", n2: "${n}", q2: "${q}"}
		}};
		should.deepEqual(misc.handleDirective(spec3, data), [
			{x2: 'a', n2: 1, q2: 'hello'},
			{x2: 'a', n2: 2, q2: 'hello'},
			{x2: 'b', n2: 1, q2: 'hello'},
			{x2: 'b', n2: 2, q2: 'hello'},
			{x2: 'c', n2: 1, q2: 'hello'},
			{x2: 'c', n2: 2, q2: 'hello'},
		]);
		// Equivalent to #factorialMerge
		var spec4 = {"#for": {
			factors: {
				a: [{x: 'a'}, {x: 'b'}, {x: 'c'}],
				b: [{n: 1}, {n: 2}],
				c: {q: "hello"}
			},
			output: {"#merge": ["${a}", "${b}", "${c}"]}
		}};
		should.deepEqual(misc.handleDirective(spec4, data), [
			{x: 'a', n: 1, q: 'hello'},
			{x: 'a', n: 2, q: 'hello'},
			{x: 'b', n: 1, q: 'hello'},
			{x: 'b', n: 2, q: 'hello'},
			{x: 'c', n: 1, q: 'hello'},
			{x: 'c', n: 2, q: 'hello'},
		]);
	});

	it('should handle #merge', function() {
		var spec = {"#merge": [
			{a: 'a', b: 1, c: 'hello'},
			{x: 'b', b: 2, q: 'hello'},
			{x: 'c', n: 3},
		]};
		should.deepEqual(misc.handleDirective(spec, data),
			{a: 'a', b: 2, c: 'hello', x: 'c', q: 'hello', n: 3}
		);
	});

	it('should handle #replicate', function() {
		var spec1 = {
		    "#replicate": {
		        count: 2,
		        value: [
		            {a: 1},
		            {a: 2}
		        ]
		    }
		};
		should.deepEqual(misc.handleDirective(spec1, data),
			[{a: 1}, {a: 2}, {a: 1}, {a: 2}]
		);
		var spec2 = {
		    "#replicate": {
		        count: 2,
		        depth: 1,
		        value: [
		            {a: 1},
		            {a: 2}
		        ]
		    }
		};
		should.deepEqual(misc.handleDirective(spec2, data),
			[{a: 1}, {a: 1}, {a: 2}, {a: 2}]
		);
	});

	it('should handle #tableCols', function () {
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

	it('should handle #tableRows', function () {
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
