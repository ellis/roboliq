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
const rows1 = createFractional_5_1(1, alpha, 6)
// console.log(rows1);
const rows2 = createFractional_5_1(2, alpha, 6)
const rows3 = createFractional_5_1(1, alpha, 6)

const sources = {
	buffer: 50,
	strain: 20,
	glucose: 10,
	nitrogen1: 50,
	nitrogen2: 50,
	trace1: 50,
	trace2: 50
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
	trace1: "(4-1.5)/2",
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
			fullVolume: "200 ul",
			group,
			"bufferV=calculate": {expression: `to(fullVolume * (${centers.buffer}) / (${sources.buffer}), "ul")`},
			"strainV=calculate": {expression: `to(fullVolume * (${centers.strain}) / (${sources.strain}), "ul")`},
			"glucoseV=calculate": {expression: `to(fullVolume * ((${centers.glucose}) + x1 * (${deltas.glucose})) / (${sources.glucose}), "ul")`, decimals: 0},
			"glucoseC=calculate": {expression: `(${sources.glucose}) * glucoseV / fullVolume`, decimals: 2},
			"nitrogen1V=calculate": {expression: `max(0 ul, to(fullVolume * ((${centers.nitrogen1}) + x2 * (${deltas.nitrogen1})) / (${sources.nitrogen1}), "ul"))`, decimals: 0},
			"nitrogen1C=calculate": {expression: `(${sources.nitrogen1}) * nitrogen1V / fullVolume`, decimals: 2},
			"nitrogen2V=calculate": {expression: `max(0 ul, to(fullVolume * ((${centers.nitrogen2}) + x3 * (${deltas.nitrogen2})) / (${sources.nitrogen2}), "ul"))`, decimals: 0},
			"nitrogen2C=calculate": {expression: `(${sources.nitrogen2}) * nitrogen2V / fullVolume`, decimals: 2},
			"trace1V=calculate": {expression: `max(0 ul, to(fullVolume * ((${centers.trace1}) + x4 * (${deltas.trace1})) / (${sources.trace1}), "ul"))`, decimals: 0},
			"trace1C=calculate": {expression: `(${sources.trace1}) * trace1V / fullVolume`, decimals: 2},
			// Trace 2 has to be pipetted with large tips, so make sure it has at least 3ul
			"trace2V=calculate": {expression: `max(0 ul, to(fullVolume * ((${centers.trace2}) + x5 * (${deltas.trace2})) / (${sources.trace2}), "ul"))`, decimals: 0},
			"trace2C=calculate": {expression: `(${sources.trace2}) * trace2V / fullVolume`, decimals: 2},
			"waterV=calculate": 'fullVolume - bufferV - glucoseV - nitrogen1V - nitrogen1V - trace1V - trace2V - strainV'
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
		}
	};
};
const design1 = makeDesign(1, rows1, centers1, deltas1);
const table1 = Design.flattenDesign(design1);
// Design.printRows(table1)

const design2 = makeDesign(2, rows2, centers1, deltas1);
const table2 = Design.flattenDesign(design2);

const design3 = makeDesign(3, rows3, centers3, deltas3);
const table3 = Design.flattenDesign(design3);
// Design.printRows(table3)

const table = table1.concat(table2).concat(table3);
Design.printRows(table)
// table.forEach(row => console.log("- "+JSON.stringify(row)))

const protocol = {roboliq: "v1", objects: {design: {initialRows: table}}};
// console.log(JSON.stringify(protocol))
module.exports = protocol;
