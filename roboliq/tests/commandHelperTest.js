const _ = require('lodash');
const should = require('should');
import math from 'mathjs';
const commandHelper = require('../src/commandHelper.js')

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
			const data = {
				objects: {
					p: {
						type: "Plate",
						model: "m96"
					},
					q: {
						type: "Liquid",
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
			const params = {
				name: "plate1",
				object1: {a: 1, b: 2},
				number: 42,
				string1: "hello",
				string2: '"hello"',
				time1: 23,
				time2: "23 minutes",
				volume1: 10,
				volume2: "10 ul",
				well1: "p(A01)",
				wells1: "p(A01)",
				source1: "p(A01)",
				sources1: "p(A01 down to B01)",
				sources2: "q"
				//file
			};
			const schema = {
				properties: {
					name: {type: 'name'},
					object1: {type: 'object'},
					number: {type: 'number'},
					string1: {type: 'string'},
					string2: {type: 'string'},
					time1: {type: 'Duration'},
					time2: {type: 'Duration'},
					volume1: {type: 'Volume'},
					volume2: {type: 'Volume'},
					well1: {type: 'Well'},
					wells1: {type: 'Wells'},
					source1: {type: 'Source'},
					sources1: {type: 'Sources'},
					sources2: {type: 'Sources'},
				},
				required: ['name', 'object1', 'number', 'string1', 'string2', 'time1', 'time2', 'volume1', 'volume2', 'well1', 'wells1', 'source1', 'sources1']
			};
			const parsed = commandHelper.parseParams(params, data, schema);
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
				well1: {value: "p(A01)"},
				wells1: {value: ["p(A01)"]},
				source1: {value: "p(A01)"},
				sources1: {value: ["p(A01)", "p(B01)"]},
				sources2: {objectName: "q", value: [["p(A01)", "p(A02)"]]},
			});
			should.deepEqual(data.accesses, ["q"]);
		});

		//it("should work with values supplied via variables", () => {

		it('should work with error-free input', function() {
			const data = {
				objects: {
					agent1: {type: "MyAgent"},
					equipment1: {type: "MyEquipment", config: "special"},
					plate1: {type: "Plate", location: "P1"},
					site1: {type: "Site", extraData: 0},
					number1: {type: "Variable", value: 1},
					string1: {type: "Variable", value: "hello"},
					liquid1: {type: "Liquid", wells: ["plate1(A01)", "plate2(A02)"]},
				},
				accesses: []
			};
			const params = {
				objectName: "plate1",
				agent: "agent1",
				equipment: "equipment1",
				plate: "plate1",
				site: "site1",
				count: "number1",
				text: "${string1}"
			};
			const schema = {
				properties: {
					objectName: {type: "name"},
					agent: {type: "Agent"},
					equipment: {type: "Equipment"},
					plate: {type: "Plate"},
					site: {type: "Site"},
					count: {type: "number"},
					text: {type: "string"},
					any2: {}
				},
				required: ['objectName', 'agent', 'equipment', 'plate', 'site', 'count', 'text']
			};
			const parsed = commandHelper.parseParams(params, data, schema);
			should.deepEqual(parsed, {
				objectName: {objectName: "plate1"},
				agent: {objectName: "agent1", value: {type: "MyAgent"}},
				equipment: {objectName: "equipment1", value: {type: "MyEquipment", config: "special"}},
				plate: {objectName: "plate1", value: {type: "Plate", location: "P1"}},
				site: {objectName: "site1", value: {type: "Site", extraData: 0}},
				count: {objectName: "number1", value: 1},
				any2: {},
				text: {value: "hello"}
			});
			should.deepEqual(data.accesses, ['agent1', 'equipment1', 'plate1', 'site1', "number1", "string1.type", "string1.value"]);
		});

		it('should work with defaults', function() {
			const data = {
				objects: {},
				accesses: []
			};
			const params = {
				number1: 1
			};
			const schema = {
				properties: {
					number1: {type: "number", default: -1},
					number2: {type: "number", default: 2}
				},
				required: ['number1', 'number2']
			};
			const parsed = commandHelper.parseParams(params, data, schema);
			should.deepEqual(parsed, {
				number1: {value: 1},
				number2: {value: 2}
			});
			should.deepEqual(data.accesses, []);
		});

		it('should work with arrays of variables', () => {
			const data = {
				objects: {
					n1: {type: "Variable", value: 1},
					n2: {type: "Variable", value: 2},
				},
				accesses: []
			};
			const params = {
				ns: ["n1", "n2"]
			};
			const schema = {
				properties: {
					ns: {type: "array", items: {type: "integer"}},
				},
				required: ["ns"]
			};
			const parsed = commandHelper.parseParams(params, data, schema);
			should.deepEqual(parsed, {
				ns: {value: [1, 2]}
			});
			should.deepEqual(data.accesses, ["n1", "n2"]);
		});

		it('should work for a previous bug', function() {
			const data = {
				objects: {
					plate1: {type: "Plate", location: "P1"},
					number1: {type: "Variable", value: 1}
				},
				accesses: []
			};
			const params = {
				"command": "sealer.sealPlate",
				"object": "plate1"
			};
			const schema = {
				properties: {
					agent: {type: "name"},
					equipment: {type: "name"},
					program: {type: "name"},
					object: {type: "name"},
					site: {type: "name"},
					destinationAfter: {type: "name"}
				},
				required: ['object']
			};
			const parsed = commandHelper.parseParams(params, data, schema);
			should.deepEqual(parsed, {
				agent: {},
				equipment: {},
				program: {},
				object: {objectName: "plate1"},
				site: {},
				destinationAfter: {}
			});
		});

		it('should catch missing sources', () => {
			const data = {
				objects: {},
				accesses: []
			};
			const params = {
				sources: ["missing"]
			};
			const schema = {
				properties: {
					sources: {type: "Sources"},
				},
				required: ["sources"]
			};
			should.throws(() => commandHelper.parseParams(params, data, schema), "something");
		});
	});

	describe('commandHelper.getParsedValue', function() {
		it('should work with error-free input', function() {
			const data = {
				objects: {
					plate1: {type: "Plate", location: "P1"},
					number1: {type: "Variable", value: 1}
				},
				accesses: []
			};
			const parsed = {
				objectName: {objectName: "plate1"},
				object: {objectName: "plate1", value: {type: "Plate", location: "P1"}},
				count: {objectName: "number1", value: 1}
			};
			should.deepEqual(commandHelper.getParsedValue(parsed, data, "objectName", "location"), "P1");
			should.deepEqual(commandHelper.getParsedValue(parsed, data, "object", "location"), "P1");
		});
	});
});
