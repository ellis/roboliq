/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017, ETH Zurich, Ellis Whitehead
 * @license GPL-3.0
 */

var _ = require('lodash');
var should = require('should');
var misc = require('../src/misc.js')

describe('misc', function() {
	describe('misc.mutateDeep', function () {
		it('should mutate an object', function () {
			var x1 = {
				a: 1,
				b: 'hello'
			};
			var x2 = {
				a: {
					b: {n: 1},
					n: 1
				},
				b: 'hello'
			};
			var x3 = {
				a: {"#": {
					b: 'hello',
					c: {'#': {
						d: 3,
						e: 4
					}}
				}}
			};
			var fn1 = function(x) {
				if (_.isNumber(x)) return x * 2;
				return x;
			}
			var fn2 = function(x) {
				//console.log("fn2:", x)
				if (_.isPlainObject(x)) {
					var l = _.toPairs(x);
					//console.log('l:', l)
					if (l.length === 1 && _.startsWith(l[0][0], "#")) {
						return _.toPairs(l[0][1]);
					}
				}
				//console.log("fn2 ret:", x)
				return x;
			}

			misc.mutateDeep(x1, fn1);
			should.deepEqual(x1, {a: 2, b: 'hello'});

			misc.mutateDeep(x2, fn1);
			should.deepEqual(x2, {a: {b: {n: 2}, n: 2}, b: 'hello'});

			// Handle directives for objects first
			misc.mutateDeep(x3, fn2);
			should.deepEqual(x3, {a:
				[['b', 'hello'], ['c', [['d', 3], ['e', 4]]]]
			});
		});
	});
});
