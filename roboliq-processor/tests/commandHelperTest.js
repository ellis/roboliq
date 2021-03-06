/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017, ETH Zurich, Ellis Whitehead
 * @license GPL-3.0
 */

const _ = require('lodash');
const should = require('should');
const math = require('mathjs');
const commandHelper = require('../src/commandHelper.js')

describe('commandHelper', function() {
	describe('_dereferenceVariable', function () {
		const objects = {
			number1: {type: "Variable", value: 1},
			number2: {type: "Variable", value: 'number1'},
			number3: {type: "Variable", value: 'number2'},
			DATA: [
				{name: "bob", number: 1},
				{name: "bob", number: 2},
			],
			SCOPE: {
				name: "bob"
			}
		};
		it('should handle 1-depth', () => {
			const data = commandHelper.createData({}, objects, objects.SCOPE, objects.DATA);
			should.deepEqual(
				commandHelper._dereferenceVariable(data, 'number1'),
				{objectName: 'number1', value: 1}
			);
		});
		it('should handle 2-depth', () => {
			const data = commandHelper.createData({}, objects, objects.SCOPE, objects.DATA);
			should.deepEqual(
				commandHelper._dereferenceVariable(data, 'number2'),
				{objectName: 'number1', value: 1}
			);
		});
		it('should handle 3-depth', () => {
			const data = commandHelper.createData({}, objects, objects.SCOPE, objects.DATA);
			should.deepEqual(
				commandHelper._dereferenceVariable(data, 'number3'),
				{objectName: 'number1', value: 1}
			);
		});
		it("should handle SCOPE lookup of DATA column", () => {
			const data = commandHelper.createData({}, objects, objects.SCOPE, objects.DATA);
			// console.log("data:"); console.log(data)
			should.deepEqual(
				commandHelper._dereferenceVariable(data, '$number'),
				{value: [1, 2]}
			);
			should.deepEqual(data.accesses, new Set(["SCOPE.number"]));
		});
		it("should handle SCOPE lookup of DATA column _ONE value", () => {
			const data = commandHelper.createData({}, objects, objects.SCOPE, objects.DATA);
			should.deepEqual(
				commandHelper._dereferenceVariable(data, '$name_ONE'),
				{value: "bob"}
			);
			should.deepEqual(data.accesses, new Set(["SCOPE.name_ONE"]));
		});
	});

	describe("copyItemsWithDefaults", () => {
		it("should handle undefined items but with defined defaults", () => {
			const defaults = {
				a: [1,2,3],
				b: 4,
				c: undefined
			};
			should.deepEqual(commandHelper.copyItemsWithDefaults(undefined, defaults), [
				{a: 1, b: 4},
				{a: 2, b: 4},
				{a: 3, b: 4}
			]);
		});

	});

	describe('getStepKeys', function () {
		const steps = {
			description: "hi",
			1: {},
			2: {},
			5: {},
			3: {},
			"4a": {}
		};
		it('should get step-related keys of an object', () => {
			should.deepEqual(
				commandHelper.getStepKeys(steps),
				["1", "2", "3", "4a", "5"]
			);
		});
		it('should get step-related keys of an array', () => {
			should.deepEqual(
				commandHelper.getStepKeys([{}, {}, {}]),
				[0, 1, 2]
			);
		});
	});

	describe('lookupPath', function() {
		it("should lookup mixtures of parameter and object values", () => {
			const data = {
				objects: {
					model1: {evowareName: "evowareModel1"},
					plate1: {model: "model1"}
				},
				accesses: []
			};
			const params = {
				object: "plate1"
			};
			const path = [["@object", "model"], "evowareName"];
			should.deepEqual(
				commandHelper.lookupPath(path, params, data),
				"evowareModel1"
			);
		});
		it("should lookup an object value", () => {
			const data = {
				objects: {
					plate1: {model: "model1"}
				},
				accesses: []
			};
			const params = {};
			const path = ["plate1", "model"];
			should.deepEqual(
				commandHelper.lookupPath(path, params, data),
				"model1"
			);
		});
	});

	describe('parseParams', function() {

		require('mathjs').config({
			number: 'BigNumber', // Default type of number
			precision: 64        // Number of significant digits for BigNumbers
		});

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
				string3: ['"hello"', '"hello"', '"hello"'],
				time1: 23,
				time2: "23 minutes",
				time3: [23, 23, 23],
				length1: "40 m",
				length2: ["40 m", "40 m", "40 m"],
				plate1: "p",
				plate2: ["p"],
				plates1: "p",
				plates2: ["p", "p"],
				// volume1: 10,
				volume2: "10 ul",
				volume3: ["10 ul", "10 ul"],
				volumes1: "10 ul",
				volumes2: ["10 ul", "20 ul"],
				well1: "p(A01)",
				well2: ["p(A01)", "p(A01)", "p(A01)"],
				wells1: "p(A01)",
				source1: "p(A01)",
				source2: ["p(A01)", "p(A01)", "p(A01)"],
				sources1: "p(A01 down to B01)",
				sources2: "q",
				extra: "EXTRA"
				//file
			};
			Object.freeze(params);
			const schema = {
				properties: {
					name: {type: 'name'},
					object1: {type: 'object'},
					number: {type: 'number'},
					string1: {type: 'string'},
					string2: {type: 'string'},
					string3: {type: 'string'},
					time1: {type: 'Duration'},
					time2: {type: 'Duration'},
					time3: {type: 'Duration'},
					length1: {type: "Length"},
					length2: {type: "Length"},
					plate1: {type: "Plate"},
					plate2: {type: "Plate"},
					plates1: {type: "Plates"},
					plates2: {type: "Plates"},
					volume2: {type: 'Volume'},
					volume3: {type: 'Volume'},
					volumes1: {type: 'Volumes'},
					volumes2: {type: 'Volumes'},
					well1: {type: 'Well'},
					well2: {type: 'Well'},
					wells1: {type: 'Wells'},
					source1: {type: 'Source'},
					source2: {type: 'Source'},
					sources1: {type: 'Sources'},
					sources2: {type: 'Sources'},
				}
			};
			schema.required = _.keys(schema.properties);
			const parsed = commandHelper.parseParams(params, data, schema);
			// console.log("parsed.orig: "+JSON.stringify(parsed.orig, null, '\t'))
			should.deepEqual(parsed.orig, params);
			should.deepEqual(_.pick(parsed, "value"), {value: {
				name: "plate1",
				object1: {a: 1, b: 2},
				number: 42,
				string1: "hello",
				string2: '"hello"',
				string3: '"hello"',
				time1: math.unit(23, 's'),
				time2: math.unit(math.bignumber(23), 'minutes'),
				time3: math.unit(23, 's'),
				length1: math.unit(math.bignumber(40), 'm'),
				length2: math.unit(math.bignumber(40), 'm'),
				plate1: data.objects.p,
				plate2: data.objects.p,
				plates1: [data.objects.p],
				plates2: [data.objects.p, data.objects.p],
				volume2: math.unit(math.bignumber(10), 'ul'),
				volume3: math.unit(math.bignumber(10), 'ul'),
				volumes1: [math.unit(math.bignumber(10), 'ul')],
				volumes2: [math.unit(math.bignumber(10), 'ul'), math.unit(math.bignumber(20), 'ul')],
				well1: "p(A01)",
				well2: "p(A01)",
				wells1: ["p(A01)"],
				source1: "p(A01)",
				source2: "p(A01)",
				sources1: ["p(A01)", "p(B01)"],
				sources2: [["p(A01)", "p(A02)"]]
			}});
			should.deepEqual(_.pick(parsed, "objectName"), {
				objectName: {
					plate1: "p",
					plate2: "p",
					"plates1.0": "p",
					"plates2.0": "p",
					"plates2.1": "p",
					"sources2": "q"
				}
			});
			should.deepEqual(parsed.unknown, ["extra"]);
			should.deepEqual(data.accesses, new Set(["p", "q"]));
		});

		//it("should work with values supplied via variables", () => {

		it('should work with error-free input', function() {
			const data = {
				objects: {
					agent1: {type: "MyAgent"},
					equipment1: {type: "MyEquipment", config: "special"},
					plate1: {type: "Plate", location: "site"},
					lid1: {type: "Lid", location: "plate1"},
					site1: {type: "Site", extraData: 0},
					number1: {type: "Variable", value: 1},
					liquid1: {type: "Liquid", wells: ["plate1(A01)", "plate2(A02)"]},
					string1: {type: "Variable", value: "hello1"},
					PARAMS: {
						string2: "hello2",
					}
				},
				accesses: []
			};
			const params = {
				objectName: "plate1",
				agent: "agent1",
				equipment: "equipment1",
				plate: "plate1",
				lid: "lid1",
				// plates: "plate1",
				site: "site1",
				count: "number1",
				text1: "string1",
				text2: "${string2}"
			};
			const schema = {
				properties: {
					objectName: {type: "name"},
					agent: {type: "Agent"},
					equipment: {type: "Equipment"},
					plate: {type: "Plate"},
					lid: {type: "Labware"},
					// plates: {type: "Plates"},
					site: {type: "Site"},
					count: {type: "number"},
					text1: {type: "string"},
					text2: {type: "string"},
					any2: {}
				},
				required: ['objectName', 'agent', 'equipment', 'plate', 'site', 'count', 'text']
			};
			const parsed = commandHelper.parseParams(params, data, schema);
			// console.log(JSON.stringify(parsed, null, '\t'))
			should.deepEqual(parsed, {
				orig: _.merge({}, params, {text2: "hello2"}), // Since $-substitution was used, the original value is replaced
				value: {
					objectName: "plate1",
					agent: {type: "MyAgent"},
					equipment: {type: "MyEquipment", config: "special"},
					plate: {type: "Plate", location: "site"},
					lid: {type: "Lid", location: "plate1"},
					// plates: [{type: "Plate", location: "P1"}],
					site: {type: "Site", extraData: 0},
					count: 1,
					text1: "hello1",
					text2: "hello2"
				},
				objectName: {
					agent: "agent1",
					equipment: "equipment1",
					plate: "plate1",
					lid: "lid1",
					site: "site1",
					text1: "string1",
					count: "number1",
				}
			});
			should.deepEqual(data.accesses, new Set(['agent1', 'equipment1', 'plate1', 'lid1', 'site1', "number1", "string1"]));
		});

		it('should work with defaults', function() {
			const data = {
				objects: {},
				accesses: new Set()
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
				orig: params,
				value: {
					number1: 1,
					number2: 2
				},
				objectName: {}
			});
			should.deepEqual(data.accesses, new Set());
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
				orig: params,
				objectName: {
					"ns.0": "n1",
					"ns.1": "n2"
				},
				value: {
					ns: [1, 2]
				}
			});
			should.deepEqual(data.accesses, new Set(["n1", "n2"]));
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
				orig: params,
				objectName: {},
				value: {
					object: "plate1"
				}
			});
		});

		it("should work for a previous bug 2", () => {
			const data = {
				objects: {
					ourlab: {
						mario: {
							liha: {
								syringe: {
									1: {
										"type": "Syringe",
										"tipModel": "ourlab.mario.liha.tipModel.tipModel1000",
										"tipModelPermanent": "ourlab.mario.liha.tipModel.tipModel1000",
										contaminants: undefined,
										contents: undefined,
										cleaned: "thorough"
									}
								}
							}
						}
					}
				},
				schemas: {
					Syringe: {
						description: "Pipetting syringe.",
						properties: {
							type: {enum: ["Syringe"]},
							description: {type: "string"},
							label: {type: "string"},
							tipModel: {type: "string"},
							tipModelPermanent: {type: "string"}
						},
						required: ["type"]
					}
				},
				accesses: []
			};
			const params = {
				"syringe":"ourlab.mario.liha.syringe.1",
				"intensity":"thorough"
			};
			const schema = {
				"type":"object",
				"properties":{
					"syringe":{"description":"Syringe identifier","type":["number","nameOf Syringe"]},
					"intensity":{"description":"Intensity of the cleaning","type":"string"}
				},
				"required":["syringe"]
			};
			const parsed = commandHelper.parseParams(params, data, schema);
			should.deepEqual(parsed, {
				orig: params,
				objectName: {
					syringe: "ourlab.mario.liha.syringe.1",
				},
				value: {
					syringe: "ourlab.mario.liha.syringe.1",
					intensity: "thorough"
				}
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

		it("should handle Syringe objects", () => {
			const data = {
				objects: {
					ourlab: {
						mario: {
							liha: {
								syringe: {
									1: {
										"type": "Syringe",
										"tipModel": "ourlab.mario.liha.tipModel.tipModel1000",
										"tipModelPermanent": "ourlab.mario.liha.tipModel.tipModel1000",
										contaminants: undefined,
										contents: undefined,
										cleaned: "thorough"
									}
								}
							}
						}
					}
				},
				schemas: {
					Syringe: {
						description: "Pipetting syringe.",
						properties: {
							type: {enum: ["Syringe"]},
							description: {type: "string"},
							label: {type: "string"},
							tipModel: {type: "string"},
							tipModelPermanent: {type: "string"},
							contaminants: {type: "array"},
							contents: {type: "array"},
							cleaned: {type: "string"}
						},
						required: ["type"]
					}
				},
				accesses: []
			};
			const params = {
				"syringe": "ourlab.mario.liha.syringe.1",
				"syringeName": "ourlab.mario.liha.syringe.1"
			};
			const schema = {
				properties: {
					syringe: {type: "Syringe"},
					syringeName: {type: "nameOf Syringe"}
				},
				required: ['syringe', "syringeName"]
			};
			const parsed = commandHelper.parseParams(params, data, schema);
			should.deepEqual(parsed, {
				orig: params,
				objectName: {
					syringe: "ourlab.mario.liha.syringe.1",
					syringeName: "ourlab.mario.liha.syringe.1",
				},
				value: {
					syringe: {
						"type": "Syringe",
						"tipModel": "ourlab.mario.liha.tipModel.tipModel1000",
						"tipModelPermanent": "ourlab.mario.liha.tipModel.tipModel1000",
						contaminants: undefined,
						contents: undefined,
						cleaned: "thorough"
					},
					syringeName: "ourlab.mario.liha.syringe.1",
				}
			});
		});
	});

	describe('splitItemsAndDefaults', function () {
		it('should split a list of items into a list of items with common values moved to an object of defaults', () => {
			const items = [
				{a: 1, b: 1, c: 1, d: 1},
				{a: 1, b: 2, c: 1, d: 2},
				{a: 1, b: 3, c: 1, d: 3},
				{a: 1, b: 4, c: 1, d: 4}
			];
			should.deepEqual(commandHelper.splitItemsAndDefaults(items), {
				items: [
					{b: 1, d: 1},
					{b: 2, d: 2},
					{b: 3, d: 3},
					{b: 4, d: 4},
				],
				defaults: {a: 1, c: 1}
			});
		});

		it('should split a list of items into a list of items with common values moved to an object of defaults #2', () => {
			const items = [
				{"count":3,"destination":"plate1(A01)","volume":{"mathjs":"Unit","value":{"mathjs":"BigNumber","value":"70"},"unit":"ul","fixPrefix":false}},
				{"count":3,"destination":"plate1(B01)","volume":{"mathjs":"Unit","value":{"mathjs":"BigNumber","value":"70"},"unit":"ul","fixPrefix":false}}
			];
			should.deepEqual(commandHelper.splitItemsAndDefaults(items, ["syringe","destination"]), {
				items: [
					{"destination":"plate1(A01)"},
					{"destination":"plate1(B01)"}
				],
				defaults: {count: 3, volume: {"mathjs":"Unit","value":{"mathjs":"BigNumber","value":"70"},"unit":"ul","fixPrefix":false}}
			});
		});

	});

	describe('stepify', function () {
		it('should leave proper steps object unchanged', () => {
			const steps = {
				1: {n: 1},
				2: {n: 2},
				5: {n: 5},
				3: {n: 3},
				"4a": {n: 4}
			};
			should.deepEqual(
				commandHelper.stepify(steps),
				steps
			);
		});
		it('should wrap steps if the top object has its own properties', () => {
			const steps = {
				description: "hi",
				1: {n: 1},
				2: {n: 2},
				5: {n: 5},
				3: {n: 3},
				"4a": {n: 4}
			};
			should.deepEqual(
				commandHelper.stepify(steps),
				{"1": steps}
			);
		});
		it('should turn array into object', () => {
			should.deepEqual(
				commandHelper.stepify([{n: 1}, {n: 2}, {n: 3}, {n: 4}, {n: 5}]),
				{
					1: {n: 1},
					2: {n: 2},
					3: {n: 3},
					4: {n: 4},
					5: {n: 5}
				}
			);
		});
		it('should turn array of arrays into object', () => {
			should.deepEqual(
				commandHelper.stepify([[], [{n: 1}, {n: 2}], [], [[{n: 3}, {n: 4}, {n: 5}]]]),
				{
					1: {n: 1},
					2: {n: 2},
					3: {n: 3},
					4: {n: 4},
					5: {n: 5}
				}
			);
		});
	});

	describe("substituteDeep", () => {
		const SCOPE = {
			a: "A"
		};
		const DATA = [
			{n: 1}, {n: 2}
		];
		const data = commandHelper.createData({}, {}, SCOPE, DATA);
		// console.log("data:"); console.log(data);

		it("should handle SCOPE, DATA, and template substitutions", () => {
			const x = {
				x1: "$a",
				x2: "$n",
				x3: {
					x31: "$a",
					x32: "$n"
				},
				x4: [
					"$a",
					"$n"
				],
				x5: "`My {{a}}`"
			};
			should.deepEqual(
				commandHelper.substituteDeep(x, data, SCOPE, DATA),
				{
					x1: "A",
					x2: [1, 2],
					x3: {
						x31: "A",
						x32: [1, 2]
					},
					x4: [
						"A",
						[1, 2]
					],
					x5: "My A"
				}
			);
		});

		it("should skip directives and 'steps' properties", () => {
			const x = {
				x1: "$a",
				"#x2": "$a",
				"steps": {
					1: "$a"
				}
			};
			should.deepEqual(
				commandHelper.substituteDeep(x, data, SCOPE, DATA),
				{
					x1: "A",
					"#x2": "$a",
					"steps": {
						1: "$a"
					}
				}
			);
		});

		it("should handle embedded @DATA and @SCOPE properties", () => {
			const x = {
				x1: "$a",
				x2: "$n",
				x3: {
					"@DATA": [{y: "a"}, {y: "b"}],
					"@SCOPE": {b: "B"},
					x31: "$a",
					x32: "$b",
					x33: "$y"
				}
			};
			should.deepEqual(
				commandHelper.substituteDeep(x, data, SCOPE, DATA),
				{
					x1: "A",
					x2: [1, 2],
					x3: {
						"@DATA": [{y: "a"}, {y: "b"}],
						"@SCOPE": {b: "B"},
						x31: "A",
						x32: "B",
						x33: ["a", "b"]
					}
				}
			);
		});

		it("should handle 'data' properties", () => {
			const x = {
				data: {where: {n: {"gt": 1}}},
				x1: "$n",
				x2: {
					"@DATA": [{q: "Q", y: 1}, {q: "R", y: 2}],
					data: {where: {y: 2}},
					x21: "$q",
					x22: "$q_ONE",
					x23: "`Hello, {{q_ONE}}: {{q}}`"
				}
			};
			// console.log("data: "+JSON.stringify(data));
			should.deepEqual(
				commandHelper.substituteDeep(x, data, SCOPE, DATA),
				{
					data: {where: {n: {"gt": 1}}},
					x1: [2],
					x2: {
						"@DATA": [{q: "Q", y: 1}, {q: "R", y: 2}],
						data: {where: {y: 2}},
						x21: ["R"],
						x22: "R",
						x23: "Hello, R: R"
					}
				}
			);
		});

	});

	describe("_lookupInputPath", () => {
		const plate1 = {
			type: "Plate",
			location: "site1"
		};
		const site1 = {
			type: "Site"
		};
		const data = {
			objects: {
				plate1,
				site1
			}
		};
		const parsed = {
			orig: {
				thing: "plate1"
			},
			objectName: {
				thing: "plate1"
			}
		};

		it("should handle 'thing'", () => {
			should.deepEqual(
				commandHelper._lookupInputPath("thing", parsed, data),
				"plate1"
			);
		});

		it("should handle 'thing*'", () => {
			should.deepEqual(
				commandHelper._lookupInputPath("thing*", parsed, data),
				plate1
			);
		});

		it("should handle 'thing*location'", () => {
			should.deepEqual(
				commandHelper._lookupInputPath("thing*location", parsed, data),
				"site1"
			);
		});

		it("should handle 'thing*location*'", () => {
			should.deepEqual(
				commandHelper._lookupInputPath("thing*location*", parsed, data),
				site1
			);
		});

		it("should handle '?thingX'", () => {
			should.deepEqual(
				commandHelper._lookupInputPath("?thingX", parsed, data),
				undefined
			);
		});

		/*
	  * * "?object": optionally gets parameter value.
	  */
	});

	describe("parseInputSpec", () => {
		const plate1 = {
			type: "Plate",
			location: "site1"
		};
		const site1 = {
			type: "Site"
		};
		const data = {
			objects: {
				plate1,
				site1
			}
		};
		const parsed = {
			orig: {
				thing: "plate1"
			},
			objectName: {
				thing: "plate1"
			}
		};
		const inputSpec = {
			origin: "thing*location"
		};

		it("should handle valid specifications", () => {
			should.deepEqual(
				commandHelper.parseInputSpec(inputSpec, parsed, data),
				{origin: "site1"}
			);
		});
	});

});
