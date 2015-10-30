var _ = require('lodash');
var should = require('should');
import math from 'mathjs';
var commandHelper = require('../src/commandHelper.js')

describe('commandHelper', function() {
	describe('_dereferenceVariable', function () {
		const objects = {
			number1: {type: "Variable", value: 1},
			number2: {type: "Variable", value: 'number1'},
			number3: {type: "Variable", value: 'number2'},
		};
		it('should handle 1-depth', () => {
			const data = {objects, accesses: []};
			should.deepEqual(
				commandHelper._dereferenceVariable(data, 'number1'),
				{objectName: 'number1', value: 1}
			);
		});
		it('should handle 2-depth', () => {
			const data = {objects, accesses: []};
			should.deepEqual(
				commandHelper._dereferenceVariable(data, 'number2'),
				{objectName: 'number1', value: 1}
			);
		});
		it('should handle 3-depth', () => {
			const data = {objects, accesses: []};
			should.deepEqual(
				commandHelper._dereferenceVariable(data, 'number3'),
				{objectName: 'number1', value: 1}
			);
		});
	});

	describe('_lookupValue with various levels of de-referencing', function () {
		const objects = {
			number1: {type: "Variable", value: 1},
			number2: {type: "Variable", value: 'number1'},
			number3: {type: "Variable", value: 'number2'},
		};
		const params = {
			n0: 1,
			n1: 'number1',
			n2: 'number2',
			n3: 'number3'
		};
		it('should handle 0-depth', () => {
			const data = {objects, accesses: []};
			should.deepEqual(
				commandHelper._lookupValue(params, data, 'n0'),
				{value: 1}
			);
		});
		it('should handle 1-depth', () => {
			const data = {objects, accesses: []};
			should.deepEqual(
				commandHelper._lookupValue(params, data, 'n1'),
				{objectName: 'number1', value: 1}
			);
		});
		it('should handle 2-depth', () => {
			const data = {objects, accesses: []};
			should.deepEqual(
				commandHelper._lookupValue(params, data, 'n2'),
				{objectName: 'number1', value: 1}
			);
		});
		it('should handle 3-depth', () => {
			const data = {objects, accesses: []};
			should.deepEqual(
				commandHelper._lookupValue(params, data, 'n3'),
				{objectName: 'number1', value: 1}
			);
		});
	});

	describe('_lookupValue on inline values', function() {
		const params = {
			name: "plate1",
			object1: {a: 1, b: 2},
			number: 42,
			string1: "hello",
			string2: '"hello"',
			duration1: 23,
			duration2: "23 minutes",
			volume1: 10,
			volume2: "10 ul",
		};
		it("should handle undefined values", () => {
			const data = {objects: {}, accesses: []};
			should.deepEqual(
				commandHelper._lookupValue(params, data, 'xxx'),
				{}
			);
		});
		it("should handle names", () => {
			const data = {objects: {}, accesses: []};
			should.deepEqual(
				commandHelper._lookupValue(params, data, 'name'),
				{value: 'plate1'}
			);
		});
		it("should handle objects", () => {
			const data = {objects: {}, accesses: []};
			should.deepEqual(
				commandHelper._lookupValue(params, data, 'object1'),
				{value: {a: 1, b: 2}}
			);
		});
		it("should handle numbers", () => {
			const data = {objects: {}, accesses: []};
			should.deepEqual(
				commandHelper._lookupValue(params, data, 'number'),
				{value: 42}
			);
		});
		it("should handle strings", () => {
			const data = {objects: {}, accesses: []};
			should.deepEqual(
				commandHelper._lookupValue(params, data, 'string2'),
				{value: '"hello"'}
			);
		});
		it("should handle duration numbers", () => {
			const data = {objects: {}, accesses: []};
			should.deepEqual(
				commandHelper._lookupValue(params, data, 'duration1'),
				{value: 23}
			);
		});
		it("should handle duration strings", () => {
			const data = {objects: {}, accesses: []};
			should.deepEqual(
				commandHelper._lookupValue(params, data, 'duration2'),
				{value: "23 minutes"}
			);
		});
	});

	describe('_lookupValue on variables', function() {
		const objects = {
			plate1: {type: "Plate", location: "P1"},
			number1: {type: "Variable", value: 1},
			//string1: {type: "Variable", value: "hello"},
			duration1: {type: "Variable", value: "23 minutes"},
		};
		const params = {
			name: "plate1",
			object: "plate1",
			number: "number1",
			//string: "${string1}",
			duration: "duration1",
		};
		it("should handle names", () => {
			const data = {objects, accesses: []};
			should.deepEqual(
				commandHelper._lookupValue(params, data, 'name'),
				{objectName: 'plate1', value: {type: "Plate", location: "P1"}}
			);
		});
		it("should handle objects", () => {
			const data = {objects, accesses: []};
			should.deepEqual(
				commandHelper._lookupValue(params, data, 'object'),
				{objectName: 'plate1', value: {type: "Plate", location: "P1"}}
			);
		});
		it("should handle numbers", () => {
			const data = {objects, accesses: []};
			should.deepEqual(
				commandHelper._lookupValue(params, data, 'number'),
				{objectName: 'number1', value: 1}
			);
		});
		/*it("should handle strings", () => {
			const data = {objects, accesses: []};
			should.deepEqual(
				commandHelper._lookupValue(params, data, 'string'),
				{objectName: 'string1', value: "hello"}
			);
		});*/
		it("should handle durations", () => {
			const data = {objects, accesses: []};
			should.deepEqual(
				commandHelper._lookupValue(params, data, 'duration'),
				{objectName: 'duration1', value: "23 minutes"}
			);
		});
	});

	describe('commandHelper.parseParams', function() {
		it("should work with values specified in-line", () => {
			var data = {
				objects: {
					p: {
						type: "Plate",
						model: "m96"
					},
					q: {
						type: "Source",
						wells: ["p(A01)", "p(A02)"]
					},
					m96: {
						type: "PlateModel",
						rows: 8,
						columns: 12
					},
				},
				accesses: []
			};
			var params = {
				name: "plate1",
				object1: {a: 1, b: 2},
				number: 42,
				string1: "hello",
				string2: '"hello"',
				time1: 23,
				time2: "23 minutes",
				volume1: 10,
				volume2: "10 ul",
				wells1: "p(A01)"
				//file
			};
			var specs = {
				name: 'name',
				object1: 'Object',
				number: 'Number',
				string1: 'String',
				string2: 'String',
				time1: 'Duration',
				time2: 'Duration',
				volume1: 'Volume',
				volume2: 'Volume',
				wells1: 'Wells',
			};
			var parsed = commandHelper.parseParams(params, data, specs);
			should.deepEqual(parsed, {
				name: {objectName: "plate1"},
				object1: {value: {a: 1, b: 2}},
				number: {value: 42},
				string1: {value: "hello"},
				string2: {value: '"hello"'},
				time1: {value: math.unit(23, 's')},
				time2: {value: math.unit(23, 'minutes')},
				volume1: {value: math.unit(10, 'l')},
				volume2: {value: math.unit(10, 'ul')},
				wells1: {value: ["p(A01)"]}
			});
			should.deepEqual(data.accesses, []);
		});

		//it("should work with values supplied via variables", () => {

		it('should work with error-free input', function() {
			const data = {
				objects: {
					plate1: {type: "Plate", location: "P1"},
					number1: {type: "Variable", value: 1},
					string1: {type: "Variable", value: "hello"}
				},
				accesses: []
			};
			var params = {
				objectName: "plate1",
				object: "plate1",
				count: "number1",
				text: "${string1}"
			};
			var specs = {
				objectName: "name",
				object: "Object",
				count: "Number",
				text: "String",
				any2: "Any?"
			};
			var parsed = commandHelper.parseParams(params, data, specs);
			should.deepEqual(parsed, {
				objectName: {objectName: "plate1"},
				object: {objectName: "plate1", value: {type: "Plate", location: "P1"}},
				count: {objectName: "number1", value: 1},
				any2: {},
				text: {value: "hello"}
			});
			should.deepEqual(data.accesses, ["plate1", "number1", "string1.type", "string1.value"]);
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
				object: {objectName: "plate1"},
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
				objectName: {objectName: "plate1"},
				object: {objectName: "plate1", value: {type: "Plate", location: "P1"}},
				count: {objectName: "number1", value: 1}
			};
			should.deepEqual(commandHelper.getParsedValue(parsed, data, "objectName", "location"), "P1");
			should.deepEqual(commandHelper.getParsedValue(parsed, data, "object", "location"), "P1");
		});
	});
});
