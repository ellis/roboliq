var _ = require('lodash');
var should = require('should');
var roboliq = require('../src/roboliq.js')
var directiveHandlers = require('../src/config/roboliqDirectiveHandlers.js');
var misc = require('../src/misc.js');

var data = {
	directiveHandlers: directiveHandlers,
	objects: {
		plateModel1: {
			type: "PlateModel",
			rows: 8,
			columns: 12
		},
		plate1: {
			type: "Plate",
			model: "plateModel1"
		},
		plate2: {
			type: "Plate",
			model: "plateModel1"
		},
		list1: {
			type: "Variable",
			value: [1, 2, 3]
		},
		design2: {
			type: "Design",
			conditions: {
				age: 100
			}
		},
		DATA: [
			{name: "bob", gender: "male", age: 1},
			{name: "tom", gender: "male", age: 2},
			{name: "sue", gender: "female", age: 1}
		]
	}
};

describe('config/roboliqDirectiveHandlers', function() {

	require('mathjs').config({
		number: 'BigNumber', // Default type of number
		precision: 64        // Number of significant digits for BigNumbers
	});

	it('should handle data()', function() {
		should.deepEqual(misc.handleDirective(
			{"data()": {where: {age: 1}, value: "name"}}, data),
			["bob", "sue"]
		);
		should.deepEqual(misc.handleDirective(
			{"data()": {where: {age: 2}, head: true}}, data),
			{name: "tom", gender: "male", age: 2}
		);
		should.deepEqual(misc.handleDirective(
			{"data()": {value: "$`age*2`", join: ";"}}, data),
			"2;4;2"
		);
		should.deepEqual(misc.handleDirective(
			{"data()": {template: {age2: "$`age*2`"}}}, data),
			[{age2: 2}, {age2: 4}, {age2: 2}]
		);
		should.deepEqual(misc.handleDirective(
			{"data()": {source: "design2", template: {age2: "$`age*2`"}}}, data),
			[{age2: 200}]
		);

		const data2 = _.cloneDeep(data);
		data2.objects.DATA = [
			{well: "A01", volume: "10 ul", source: "liquid1"},
			{well: "B01", volume: "10 ul", source: "liquid2"},
			{well: "A02", volume: "20 ul", source: "liquid1"},
			{well: "B02", volume: "20 ul", source: "liquid2"}
		];

		should.deepEqual(misc.handleDirective(
			{"data()": {where: 'source == "liquid1"'}}, data2),
			[{well: "A01", volume: "10 ul", source: "liquid1"}, {well: "A02", volume: "20 ul", source: "liquid1"}]
		);
		should.deepEqual(misc.handleDirective(
			{"data()": {map: '$volume'}}, data2),
			["10 ul", "10 ul", "20 ul", "20 ul"]
		);
		should.deepEqual(misc.handleDirective(
			{"data()": {where: 'source == "liquid1"', map: '$(volume * 2)'}}, data2),
			["20 ul", "40 ul"]
		);
		should.deepEqual(misc.handleDirective(
			{"data()": {map: {well: "$well"}}}, data2),
			[{well: "A01"}, {well: "B01"}, {well: "A02"}, {well: "B02"}]
		);
		should.deepEqual(misc.handleDirective(
			{"data()": {map: "$well", join: ","}}, data2),
			"A01,B01,A02,B02"
		);
		should.deepEqual(misc.handleDirective(
			{"data()": {summarize: {totalVolume: '$(sum(volume))'}}}, data2),
			[{totalVolume: "60 ul"}]
		);
		should.deepEqual(misc.handleDirective(
			{"data()": {groupBy: "source", summarize: {source: '${source[0]}', totalVolume: '$(sum(volume))'}}}, data2),
			[{source: "liquid1", totalVolume: "30 ul"}, {source: "liquid2", totalVolume: "30 ul"}]
		);
	});

	it('should handle #destinationWells', function() {
		var spec = "#destinationWells#plate1(A01 down to D01)";
		should.deepEqual(misc.handleDirective(spec, data), [
			"plate1(A01)", "plate1(B01)", "plate1(C01)", "plate1(D01)"
		]);
	});

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

	it('should handle #factorialMixtures', function() {
		var spec1 = {"#factorialMixtures": {
			replicates: 2,
			items: [
				[{a: 1}, {a: 2}],
				{a: [3, 4]}
			]
		}};
		should.deepEqual(misc.handleDirective(spec1, data), [
			[{a: 1}, {a: 3}],
			[{a: 1}, {a: 4}],
			[{a: 2}, {a: 3}],
			[{a: 2}, {a: 4}],
			[{a: 1}, {a: 3}],
			[{a: 1}, {a: 4}],
			[{a: 2}, {a: 3}],
			[{a: 2}, {a: 4}]
		]);
		/*
		var spec1 = {"#factorialMixtures": {
			replicates: 1,
			items: [
				[
					[{a: 1}, {b: 1}]
					[{b: 2}, {a: 2}]
				],
				{c: [3, 4]}
			]
		}};
		should.deepEqual(misc.handleDirective(spec1, data), [
			[{a: 1}, {b: 1}, {c: 3}],
			[{a: 1}, {b: 1}, {c: 4}],
			[{b: 2}, {a: 2}, {c: 3}],
			[{b: 2}, {a: 2}, {c: 4}],
		]);
		*/
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

	it('should handle #length', function() {
		var spec = {"#length": [1,2,3]};
		should.deepEqual(misc.handleDirective(spec, data), 3);

		var spec = {"#length": "list1"};
		should.deepEqual(misc.handleDirective(spec, data), 3);
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

	it('it should handle #createPipetteMixtureList', function() {
		var spec = {
			"#createPipetteMixtureList": {
				replicates: 1,
				volume: '25ul',
				items: [
					{source: "buffer"},
					{source: "denaturant", volume: ['0ul', '10ul', '20ul']},
					{source: ['gfp', 'yfp'], volume: '5ul'}
				],
				transformations: [
					//{name: "shuffle", seed: 1234}
				],
				transformationsPerWell: [
					{name: "sortByVolumeMax", count: 2}
				]
			}
		};

		should.deepEqual(misc.handleDirective(spec, data), [
			[{source: "buffer", volume: "20 ul"}, {source: "denaturant", volume: '0ul'}, {source: "gfp", volume: "5ul"}],
			[{source: "buffer", volume: "20 ul"}, {source: "denaturant", volume: '0ul'}, {source: "yfp", volume: "5ul"}],
			[{source: "buffer", volume: "10 ul"}, {source: "denaturant", volume: '10ul'}, {source: "gfp", volume: "5ul"}],
			[{source: "buffer", volume: "10 ul"}, {source: "denaturant", volume: '10ul'}, {source: "yfp", volume: "5ul"}],
			[{source: "denaturant", volume: '20ul'}, {source: "buffer", volume: "0 l"}, {source: "gfp", volume: "5ul"}],
			[{source: "denaturant", volume: '20ul'}, {source: "buffer", volume: "0 l"}, {source: "yfp", volume: "5ul"}],
		]);
	});

	it('should handle #replaceLabware', function() {
		var spec = {
			"#replaceLabware": {
				list: ["plate1(A01)", "plate1(B01)", "plate1(C01)", "plate1(D01)"],
				new: "plate2"
			}
		};
		should.deepEqual(misc.handleDirective(spec, data), [
			"plate2(A01)", "plate2(B01)", "plate2(C01)", "plate2(D01)"
		]);

		var data2 = _.cloneDeep(data);
		data2.objects.wells = {
			type: "Variable",
			value: ["plate1(A01)", "plate1(B01)", "plate1(C01)", "plate1(D01)"]
		};
		var spec = {
			"#replaceLabware": {
				list: "wells",
				new: "plate2"
			}
		};
		should.deepEqual(misc.handleDirective(spec, data2), [
			"plate2(A01)", "plate2(B01)", "plate2(C01)", "plate2(D01)"
		]);
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

	it('should handle #take', function () {
		var spec = {"#take": {
			list: [1, 2, 3],
			count: 2
		}};
		should.deepEqual(misc.handleDirective(spec, data), [1, 2]);
	});

	it('should handle overrides', function () {
		const spec1 = {"#take": {
			list: [1, 2, 3],
			count: 2,
			override: "hello"
		}};
		should.deepEqual(misc.handleDirective(spec1, data), "hello");

		var spec2 = {"#tableCols": {
			x: ['a', 'b', 'c'],
			n: [1, 2, 3],
			q: "hello",
			override: [{}, {q: 'bye'}]
		}};
		should.deepEqual(misc.handleDirective(spec2, data), [
			{x: 'a', n: 1, q: 'hello'},
			{x: 'b', n: 2, q: 'bye'},
			{x: 'c', n: 3, q: 'hello'},
		]);
	});
});
