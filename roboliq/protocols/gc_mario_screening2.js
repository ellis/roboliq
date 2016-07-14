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

const alpha = 2 // 1.5 // undefined;
const rows1 = createFractional_5_1(1, alpha, 6)
// console.log(rows1);
const rows2 = createFractional_5_1(2, alpha, 6)
const rows3 = createFractional_5_1(1, alpha, 6)

const ranges = {
	buffer: {min: "0 mg/L", max: "2 mg/L"},
	glucose: {min: "0 mg/L", max: "2 mg/L"},
	nitrogen: {min: 0, max: 1},
	mix: {min: "0 mg/L", max: "2 mg/L"},
	vitamin: {min: "0 mg/L", max: "2 mg/L"},
};

const sources = {
	buffer: "8 mg/L",
	glucose: "8 mg/L",
	nitrogen1: "8 mg/L",
	nitrogen2: "8 mg/L",
	mix: "8 mg/L",
	vitamin: "8 mg/L",
};

const centers1 = {
	buffer: "1 mg/L",
	glucose: "1 mg/L",
	nitrogenV: "20 ul",
	mix: "1 mg/L",
	vitamin: "1 mg/L",
};

const centers3 = {
	buffer: "0.5 mg/L",
	glucose: "0.5 mg/L",
	nitrogenV: "20 ul",
	mix: "0.5 mg/L",
	vitamin: "0.5 mg/L",
};

const calcDelta = (s) => math.eval(`((${ranges[s].max}) - (${ranges[s].min})) / 10`).format()
const deltas1 = {
	buffer: calcDelta("buffer"),
	glucose: calcDelta("glucose"),
	nitrogen: "5 ul",
	mix: calcDelta("mix"),
	vitamin: calcDelta("vitamin"),
};

/*
* $x1$: Buffer concentration
* $x2$: Glucose concentration
* $x3$: Nitrogen concentration or type
* $x4$: Phosphate/Sulfur/Trace elements mix concentration
* $x5$: Vitamin mix concentation
*/
const design1 = {
	initialRows: rows1,
	conditions: {
		fullVolume: "200 ul",
		group: 1,
		"bufferC=calculate": `(${centers1.buffer}) + x1 * (${deltas1.buffer})`,
		"bufferV=calculate": `to(fullVolume * bufferC / (${sources.buffer}), "ul")`,
		"glucoseC=calculate": `(${centers1.glucose}) + x2 * (${deltas1.glucose})`,
		"glucoseV=calculate": `to(fullVolume * glucoseC / (${sources.glucose}), "ul")`,
		"nitrogen1V=calculate": `to((${centers1.nitrogenV}) + x3 * (${deltas1.nitrogen}), "ul")`,
		"nitrogen1C=calculate": `to((${sources.nitrogen1}) * nitrogen1V / fullVolume, "mg / L")`,
		"nitrogen2V=calculate": `to((${centers1.nitrogenV}) + (1 - x3) * (${deltas1.nitrogen}), "ul")`,
		"nitrogen2C=calculate": `to((${sources.nitrogen2}) * nitrogen2V / fullVolume, "mg / L")`,
		"mixC=calculate": `(${centers1.mix}) + x4 * (${deltas1.mix})`,
		"mixV=calculate": `to(fullVolume * mixC / (${sources.mix}), "ul")`,
		"vitaminC=calculate": `(${centers1.vitamin}) + x5 * (${deltas1.vitamin})`,
		"vitaminV=calculate": `to(fullVolume * vitaminC / (${sources.vitamin}), "ul")`,
		"waterV=calculate": 'fullVolume - bufferV - glucoseV - nitrogen1V - nitrogen1V - mixV - vitaminV'
	}
};
const table1 = Design.flattenDesign(design1);
// Design.printRows(table1)

const design2 = {
	initialRows: rows2,
	conditions: {
		fullVolume: "200 ul",
		group: 2,
		"bufferC=calculate": `(${centers1.buffer}) + x1 * (${deltas1.buffer})`,
		"bufferV=calculate": `to(fullVolume * bufferC / (${sources.buffer}), "ul")`,
		"glucoseC=calculate": `(${centers1.glucose}) + x2 * (${deltas1.glucose})`,
		"glucoseV=calculate": `to(fullVolume * glucoseC / (${sources.glucose}), "ul")`,
		"nitrogen1V=calculate": `to((${centers1.nitrogenV}) + x3 * (${deltas1.nitrogen}), "ul")`,
		"nitrogen1C=calculate": `to((${sources.nitrogen1}) * nitrogen1V / fullVolume, "mg / L")`,
		"nitrogen2V=calculate": `to((${centers1.nitrogenV}) + (1 - x3) * (${deltas1.nitrogen}), "ul")`,
		"nitrogen2C=calculate": `to((${sources.nitrogen2}) * nitrogen2V / fullVolume, "mg / L")`,
		"mixC=calculate": `(${centers1.mix}) + x4 * (${deltas1.mix})`,
		"mixV=calculate": `to(fullVolume * mixC / (${sources.mix}), "ul")`,
		"vitaminC=calculate": `(${centers1.vitamin}) + x5 * (${deltas1.vitamin})`,
		"vitaminV=calculate": `to(fullVolume * vitaminC / (${sources.vitamin}), "ul")`,
		"waterV=calculate": 'fullVolume - bufferV - glucoseV - nitrogen1V - nitrogen1V - mixV - vitaminV'
	}
};
const table2 = Design.flattenDesign(design2);
// Design.printRows(table1)

const design3 = {
	initialRows: rows3,
	conditions: {
		fullVolume: "200 ul",
		group: 3,
		"bufferC=calculate": `(${centers3.buffer}) + x1 * (${deltas1.buffer})`,
		"bufferV=calculate": `to(fullVolume * bufferC / (${sources.buffer}), "ul")`,
		"glucoseC=calculate": `(${centers3.glucose}) + x2 * (${deltas1.glucose})`,
		"glucoseV=calculate": `to(fullVolume * glucoseC / (${sources.glucose}), "ul")`,
		"nitrogen1V=calculate": `to((${centers3.nitrogenV}) + x3 * (${deltas1.nitrogen}), "ul")`,
		"nitrogen1C=calculate": `to((${sources.nitrogen1}) * nitrogen1V / fullVolume, "mg / L")`,
		"nitrogen2V=calculate": `to((${centers3.nitrogenV}) + (1 - x3) * (${deltas1.nitrogen}), "ul")`,
		"nitrogen2C=calculate": `to((${sources.nitrogen2}) * nitrogen2V / fullVolume, "mg / L")`,
		"mixC=calculate": `(${centers3.mix}) + x4 * (${deltas1.mix})`,
		"mixV=calculate": `to(fullVolume * mixC / (${sources.mix}), "ul")`,
		"vitaminC=calculate": `(${centers3.vitamin}) + x5 * (${deltas1.vitamin})`,
		"vitaminV=calculate": `to(fullVolume * vitaminC / (${sources.vitamin}), "ul")`,
		"waterV=calculate": 'fullVolume - bufferV - glucoseV - nitrogen1V - nitrogen1V - mixV - vitaminV'
	}
};
const table3 = Design.flattenDesign(design3);
// Design.printRows(table3)

const table = table1.concat(table2).concat(table3);
// Design.printRows(table)
table.forEach(row => console.log("- "+JSON.stringify(row)))
