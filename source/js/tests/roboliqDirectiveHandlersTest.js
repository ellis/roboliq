var roboliq = require('../roboliq.js')
var should = require('should');
var directiveHandlers = require('../config/roboliqDirectiveHandlers.js');
var misc = require('../misc.js');

var data = {
	directiveHandlers: directiveHandlers,
	objects: {}
}
describe('config/roboliqDirectiveHandlers', function() {
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
		})
	})
})
