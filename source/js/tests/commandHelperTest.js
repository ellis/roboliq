var _ = require('lodash');
var should = require('should');
var commandHelper = require('../commandHelper.js')

describe('commandHelper', function() {
	describe('commandHelper.parseParams', function () {
		it('should work with error-free input', function () {
			var data = {
				objects: {
					plate1: {type: "Plate", location: "P1"},
					number1: {type: "Variable", value: 1}
				},
				accesses: []
			};
			var params = {
				objectName: "plate1",
				object: "plate1",
				count: "${number1}"
			};
			var specs = {
				objectName: "name",
				object: "Object",
				count: "Number",
				any2: "Any?"
			};
			var parsed = commandHelper.parseParams(params, data, specs);
			should.deepEqual(parsed, {
				objectName: {value: "plate1"},
				object: {valueName: "plate1", value: {type: "Plate", location: "P1"}},
				count: {valueName: "number1", value: 1}
			});
		});
	});
});
