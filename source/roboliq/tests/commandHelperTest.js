var _ = require('lodash');
var should = require('should');
var commandHelper = require('../commandHelper.js')

describe('commandHelper', function() {
	describe('commandHelper.parseParams', function() {
		it('should work with error-free input', function() {
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
				objectName: {valueName: "plate1"},
				object: {valueName: "plate1", value: {type: "Plate", location: "P1"}},
				count: {valueName: "number1", value: 1},
				any2: {}
			});
			should.deepEqual(data.accesses, ["plate1", "number1.type", "number1.value"]);
		});

		it('should work with defaults', function() {
			var data = {
				objects: {},
				accesses: []
			};
			var params = {
				number1: 1
			};
			var specs = {
				number1: {type: "Number", default: -1},
				number2: {type: "Number", default: 2}
			};
			var parsed = commandHelper.parseParams(params, data, specs);
			should.deepEqual(parsed, {
				number1: {value: 1},
				number2: {value: 2}
			});
			should.deepEqual(data.accesses, []);
		});

		it('should work for a previous bug', function() {
			var data = {
				objects: {
					plate1: {type: "Plate", location: "P1"},
					number1: {type: "Variable", value: 1}
				},
				accesses: []
			};
			var params = {
				"command": "sealer.sealPlate",
				"object": "plate1"
			};
			var specs = {
				agent: "name?",
				equipment: "name?",
				program: "name?",
				object: "name",
				site: "name?",
				destinationAfter: "name?"
			};
			var parsed = commandHelper.parseParams(params, data, specs);
			should.deepEqual(parsed, {
				agent: {},
				equipment: {},
				program: {},
				object: {valueName: "plate1"},
				site: {},
				destinationAfter: {}
			});
		});
	});

	describe('commandHelper.getParsedValue', function() {
		it('should work with error-free input', function() {
			var data = {
				objects: {
					plate1: {type: "Plate", location: "P1"},
					number1: {type: "Variable", value: 1}
				},
				accesses: []
			};
			var parsed = {
				objectName: {valueName: "plate1"},
				object: {valueName: "plate1", value: {type: "Plate", location: "P1"}},
				count: {valueName: "number1", value: 1}
			};
			should.deepEqual(commandHelper.getParsedValue(parsed, data, "objectName", "location"), "P1");
			should.deepEqual(commandHelper.getParsedValue(parsed, data, "object", "location"), "P1");
		});
	});
});
