import _ from 'lodash';
import math from 'mathjs';
// import Design from '../src/design2.js'
const Design = require('../src/design2.js');

// Configure mathjs to use bignumbers
require('mathjs').config({
	number: 'BigNumber', // Default type of number
	precision: 64        // Number of significant digits for BigNumbers
});

const fractional_5_1a = {
	x1: [-1,  1, -1,  1, -1,  1, -1,  1, -1,  1, -1,  1, -1,  1, -1,  1],
	x2: [-1, -1,  1,  1, -1, -1,  1,  1, -1, -1,  1,  1, -1, -1,  1,  1],
	x3: [-1, -1, -1, -1,  1,  1,  1,  1, -1, -1, -1, -1,  1,  1,  1,  1],
	x4: [-1, -1, -1, -1, -1, -1, -1, -1,  1,  1,  1,  1,  1,  1,  1,  1],
	x5: [ 1, -1, -1,  1, -1,  1,  1, -1, -1,  1,  1, -1,  1, -1, -1,  1]
};
const fractional_5_1b = _.set(_.cloneDeep(fractional_5_1a), "x5", fractional_5_1a.x5.map(x => -x));

function addAxialRuns(cols, alpha) {
	const keys = _.keys(cols);
	const n = keys.length;
	const a = (alpha) ? alpha : Math.sqrt(Math.sqrt(n));
	for (let i = 0; i < n; i++) {
		const key = keys[i];
		const axialValues = _.fill(Array(n * 2), 0);
		axialValues[i*2] = -a;
		axialValues[i*2 + 1] = a;
		cols[key].push(...axialValues);
	}
}

function addCenterRuns(cols, n) {
	const l0 = _.fill(Array(n), 0);
	_.forEach(cols, (l) => {
		l.push(...l0);
	});
}

function createFractional_5_1(fractionIndex, axialAlpha, centerCount) {
	const fractional = (fractionIndex === 1) ? fractional_5_1a : fractional_5_1b;
	const l = _.cloneDeep(fractional);
	addAxialRuns(l, axialAlpha);
	addCenterRuns(l, centerCount);
	const rows = _.values(_.merge({}, ... _.map(l, (values, key) => values.map(x => ({[key]: x})))));
	return rows;
}

const alpha = 1.5 // undefined;

const rowsFromExp1 = [
	{"center":"1","x1":0,"x2":1.5,"x3":0,"x4":0,"x5":0,"glucoseC":1.5,"nitrogen1C":2.25,"nitrogen2C":1.5,"trace1C":1.5,"trace2C":1.5},
	{"center":"1","x1":0,"x2":-1.5,"x3":0,"x4":0,"x5":0,"glucoseC":1.5,"nitrogen1C":0.75,"nitrogen2C":1.5,"trace1C":1.5,"trace2C":1.5},
	{"center":"2","x1":-1,"x2":1,"x3":-1,"x4":1,"x5":1,"glucoseC":1.5,"nitrogen1C":4,"nitrogen2C":1,"trace1C":3.75,"trace2C":4},
	{"center":"2","x1":-1,"x2":1,"x3":1,"x4":1,"x5":-1,"glucoseC":1.5,"nitrogen1C":4,"nitrogen2C":4,"trace1C":3.75,"trace2C":1},
	{"center":"1","x1":1,"x2":-1,"x3":1,"x4":-1,"x5":1,"glucoseC":2,"nitrogen1C":1,"nitrogen2C":3,"trace1C":1,"trace2C":3},
	{"center":"1","x1":0,"x2":0,"x3":0,"x4":0,"x5":0,"glucoseC":1.5,"nitrogen1C":1.5,"nitrogen2C":1.5,"trace1C":1.5,"trace2C":1.5},
	{"center":"1","x1":1,"x2":1,"x3":1,"x4":1,"x5":-1,"glucoseC":2,"nitrogen1C":2,"nitrogen2C":3,"trace1C":2,"trace2C":0},
	{"center":"2","x1":1,"x2":-1,"x3":1,"x4":-1,"x5":1,"glucoseC":3,"nitrogen1C":1.5,"nitrogen2C":4,"trace1C":1.75,"trace2C":4}
];
const rowsFromExp2 = [
	{"center":"2","x1":-1,"x2":-1,"x3":1,"x4":-1,"x5":-1,"glucoseC":1,"nitrogen1C":1,"nitrogen2C":3,"trace1C":1,"trace2C":0},
	{"center":"1","x1":-0.253,"x2":0,"x3":1.426,"x4":-1.31,"x5":0.427,"glucoseC":1.35,"nitrogen1C":1.5,"nitrogen2C":3.75,"trace1C":0.85,"trace2C":2.25},
	{"center":"2","x1":-1.495,"x2":0.618,"x3":1.162,"x4":-1.051,"x5":0,"glucoseC":0.75,"nitrogen1C":1.75,"nitrogen2C":3.25,"trace1C":0.95,"trace2C":1.5},
	{"center":"2","x1":-0.981,"x2":0.4,"x3":0.76,"x4":-0.709,"x5":0.218,"glucoseC":1,"nitrogen1C":1.75,"nitrogen2C":2.75,"trace1C":1.15,"trace2C":1.75},
	{"center":"1","x1":-0.044,"x2":0.08,"x3":0.45,"x4":-0.184,"x5":0.073,"glucoseC":1.5,"nitrogen1C":1.5,"nitrogen2C":2.25,"trace1C":1.4,"trace2C":1.5},
	{"center":"1","x1":-0.012,"x2":0.079,"x3":0.208,"x4":-0.094,"x5":0.063,"glucoseC":1.5,"nitrogen1C":1.5,"nitrogen2C":1.75,"trace1C":1.45,"trace2C":1.5},
	{"center":"1","x1":-0.115,"x2":0,"x3":0.813,"x4":-0.537,"x5":0.192,"glucoseC":1.45,"nitrogen1C":1.5,"nitrogen2C":2.75,"trace1C":1.25,"trace2C":1.75},
	{"center":"1","x1":-0.088,"x2":0.161,"x3":0.9,"x4":-0.369,"x5":0.145,"glucoseC":1.45,"nitrogen1C":1.5,"nitrogen2C":2.75,"trace1C":1.3,"trace2C":1.75}
];
const rowsFromExp3 = [
	{"center":"2","x1":-0.189,"x2":-1.486,"x3":-0.211,"x4":0.452,"x5":2.131,"glucoseC":2.11,"nitrogen1C":0.9,"nitrogen2C":2.18,"trace1C":3.2,"trace2C":5.7},
	{"center":"2","x1":-0.241,"x2":-1.263,"x3":-0.452,"x4":0.816,"x5":2.21,"glucoseC":2.07,"nitrogen1C":1.18,"nitrogen2C":1.82,"trace1C":3.57,"trace2C":5.82},
	{"center":"2","x1":-0.267,"x2":-1.151,"x3":-0.572,"x4":0.999,"x5":2.25,"glucoseC":2.05,"nitrogen1C":1.32,"nitrogen2C":1.64,"trace1C":3.75,"trace2C":5.88},
	{"center":"2","x1":0,"x2":0,"x3":0,"x4":0,"x5":0,"glucoseC":2.25,"nitrogen1C":2.76,"nitrogen2C":2.5,"trace1C":2.75,"trace2C":2.5},
	{"center":"2","x1":-0.162,"x2":-1.598,"x3":-0.091,"x4":0.269,"x5":2.091,"glucoseC":2.13,"nitrogen1C":0.8,"nitrogen2C":2.36,"trace1C":3.02,"trace2C":5.64},
	{"center":"1","x1":1,"x2":-1,"x3":1,"x4":-1,"x5":1,"glucoseC":2,"nitrogen1C":1,"nitrogen2C":3,"trace1C":1,"trace2C":3},
	{"center":"1","x1":2.181,"x2":0.681,"x3":-0.075,"x4":-0.347,"x5":0.316,"glucoseC":2.59,"nitrogen1C":1.84,"nitrogen2C":1.38,"trace1C":1.33,"trace2C":1.98},
	{"center":"2","x1":1.935,"x2":-0.368,"x3":0.391,"x4":-0.384,"x5":-1.125,"glucoseC":3.7,"nitrogen1C":2.3,"nitrogen2C":3.08,"trace1C":2.37,"trace2C":0.82}
];

const rowsFromExps = rowsFromExp1.concat(rowsFromExp2).concat(rowsFromExp3);

const rows1 = rowsFromExps.filter(x => x.center === "1");
const rows2 = rowsFromExps.filter(x => x.center === "2");

const sources = {
	buffer: 40,
	strain: 20,
	glucose: 20,
	nitrogen1: 40,
	nitrogen2: 40,
	trace1: 10,
	trace2: 40
};

const ranges = {
	glucose: {min: 0, max: 2},
	nitrogen1: {min: 0, max: 5},
	nitrogen2: {min: 0, max: 5},
	trace1: {min: 0, max: 5},
	trace2: {min: 0, max: 5},
};

/*
glucose: 2%-4% (1x - 2x)
nitrogen1: 1x-2x
nitrogen2: 0-3x
trace1: 1x-2x
trace2: 0-3x

higher box:
glucose: 1.5x - 3x (3% - 6%)
nitrogen1: 1.5x-4x
nitrogen2: 1-4x
trace1: 1.5x-4x
trace2: 1-4x
*/

const centers1 = {
	buffer: 1,
	strain: 1,
	glucose: 1.5,
	nitrogen1: 1.5,
	nitrogen2: 1.5,
	trace1: 1.5,
	trace2: 1.5
};
// const calcDelta = (s) => math.eval(`((${ranges[s].max}) - (${ranges[s].min})) / 10`).format()
const deltas1 = {
	glucose: 0.5,
	nitrogen1: 0.5,
	nitrogen2: 1.5,
	trace1: 0.5,
	trace2: 1.5,
};

const centers3 = {
	buffer: 1,
	strain: 1,
	glucose: "(1.5+3)/2",
	nitrogen1: "(1.5+4)/2",
	nitrogen2: "(1+4)/2",
	trace1: "(1.5+4)/2",
	trace2: "(1+4)/2"
};
const deltas3 = {
	glucose: "(3-1.5)/2",
	nitrogen1: "(4-1.5)/2",
	nitrogen2: "(4-1)/2",
	trace1: "(3-1)/2",
	trace2: "(4-1)/2",
};

/*
* $x1$: Buffer concentration
* $x2$: Glucose concentration
* $x3$: Nitrogen concentration or type
* $x4$: Phosphate/Sulfur/Trace elements mix concentration
* $x5$: Vitamin mix concentation
*/
function makeDesign(group, initialRows, centers, deltas) {
	return {
		initialRows,
		conditions: {
			fullVolume: "1000 ul",
			//group,
			"bufferV=calculate": {expression: `to(fullVolume * (${centers.buffer}) / (${sources.buffer}), "ul")`, decimals: 1},
			"strainV=calculate": {expression: `to(fullVolume * (${centers.strain}) / (${sources.strain}), "ul")`, decimals: 1},
			"glucoseV=calculate": {expression: `max(4 ul, to(fullVolume * ((${centers.glucose}) + x1 * (${deltas.glucose})) / (${sources.glucose}), "ul"))`, decimals: 1},
			"nitrogen1V=calculate": {expression: `max(4 ul, to(fullVolume * ((${centers.nitrogen1}) + x2 * (${deltas.nitrogen1})) / (${sources.nitrogen1}), "ul"))`, decimals: 1},
			"nitrogen2V=calculate": {expression: `to(fullVolume * ((${centers.nitrogen2}) + x3 * (${deltas.nitrogen2})) / (${sources.nitrogen2}), "ul")`, decimals: 1},
			"trace1V=calculate": {expression: `max(4 ul, to(fullVolume * ((${centers.trace1}) + x4 * (${deltas.trace1})) / (${sources.trace1}), "ul"))`, decimals: 1},
			// Trace 2 has to be pipetted with large tips, so make sure it has at least 3ul
			"trace2V=calculate": {expression: `to(fullVolume * ((${centers.trace2}) + x5 * (${deltas.trace2})) / (${sources.trace2}), "ul")`, decimals: 1},
			".tooSmall=case": {
				cases: [
					//{where: 'glucoseV < (4ul)', conditions: {glucoseV: "4 ul"}},
					//{where: 'nitrogen1V < (4ul)', conditions: {nitrogen1V: "0 ul"}},
					{where: 'nitrogen2V < (2ul)', conditions: {nitrogen2V: "0 ul"}},
					{where: 'nitrogen2V < (4ul)', conditions: {nitrogen2V: "4 ul"}},
					//{where: 'trace1V < (4ul)', conditions: {trace1V: "0 ul"}},
					{where: 'trace2V < (2ul)', conditions: {trace2V: "0 ul"}},
					{where: 'trace2V < (4ul)', conditions: {trace2V: "4 ul"}},
				]
			},
			"glucoseC=calculate": {expression: `(${sources.glucose}) * glucoseV / fullVolume`, decimals: 2},
			"nitrogen1C=calculate": {expression: `(${sources.nitrogen1}) * nitrogen1V / fullVolume`, decimals: 2},
			"nitrogen2C=calculate": {expression: `(${sources.nitrogen2}) * nitrogen2V / fullVolume`, decimals: 2},
			"trace1C=calculate": {expression: `(${sources.trace1}) * trace1V / fullVolume`, decimals: 2},
			"trace2C=calculate": {expression: `(${sources.trace2}) * trace2V / fullVolume`, decimals: 2},
			"waterV=calculate": {expression: 'fullVolume - bufferV - glucoseV - nitrogen1V - nitrogen1V - trace1V - trace2V - strainV', decimals: 1}
			// "bufferV": `to(fullVolume * bufferC / (${sources.buffer}), "ul")`,
			// "glucoseC=calculate": `(${centers.glucose}) + x2 * (${deltas.glucose})`,
			// "glucoseV=calculate": `to(fullVolume * glucoseC / (${sources.glucose}), "ul")`,
			// "nitrogen1V=calculate": `to((${centers.nitrogenV}) + x3 * (${deltas.nitrogen}), "ul")`,
			// "nitrogen1C=calculate": `to((${sources.nitrogen1}) * nitrogen1V / fullVolume, "mg / L")`,
			// "nitrogen2V=calculate": `to((${centers.nitrogenV}) + (1 - x3) * (${deltas.nitrogen}), "ul")`,
			// "nitrogen2C=calculate": `to((${sources.nitrogen2}) * nitrogen2V / fullVolume, "mg / L")`,
			// "mixC=calculate": `(${centers.mix}) + x4 * (${deltas.mix})`,
			// "mixV=calculate": `to(fullVolume * mixC / (${sources.mix}), "ul")`,
			// "vitaminC=calculate": `(${centers.vitamin}) + x5 * (${deltas.vitamin})`,
			// "vitaminV=calculate": `to(fullVolume * vitaminC / (${sources.vitamin}), "ul")`,
			// "waterV=calculate": 'fullVolume - bufferV - glucoseV - nitrogen1V - nitrogen1V - mixV - vitaminV'
		},
		orderBy: ["center", "x1", "x2", "x3", "x4", "x5"]
	};
};
const design1 = makeDesign(1, rows1, centers1, deltas1);
const table1 = Design.flattenDesign(design1);
// Design.printRows(table1)

const design2 = makeDesign(2, rows2, centers3, deltas3);
const table2 = Design.flattenDesign(design2);

// const design3 = makeDesign(3, rows3, centers3, deltas3);
// const table3 = Design.flattenDesign(design3);
// // Design.printRows(table3)
//
// const table = table1.concat(table2).concat(table3);
const table = table1.concat(table2);
Design.printRows(table)
// table.forEach(row => console.log("- "+JSON.stringify(row)))

const protocol = {roboliq: "v1", objects: {design: {initialRows: table}}};
// console.log(JSON.stringify(protocol))
module.exports = protocol;
