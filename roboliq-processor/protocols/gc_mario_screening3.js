import _ from 'lodash';
import math from 'mathjs';
// import Design from '../src/design.js'
const Design = require('../src/design.js');

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
//const rows1 = createFractional_5_1(1, alpha, 6)
const rows1 = [{"center":"1","gradient":1,"x1":-0.022,"x2":0.04,"x3":0.225,"x4":-0.092,"x5":0.036},{"center":"1","gradient":1,"x1":-0.044,"x2":0.08,"x3":0.45,"x4":-0.184,"x5":0.073},{"center":"1","gradient":1,"x1":-0.066,"x2":0.12,"x3":0.675,"x4":-0.276,"x5":0.109},{"center":"1","gradient":1,"x1":-0.088,"x2":0.161,"x3":0.9,"x4":-0.369,"x5":0.145},{"center":"1","gradient":1,"x1":-0.11,"x2":0.201,"x3":1.125,"x4":-0.461,"x5":0.182},{"center":"1","gradient":1,"x1":-0.132,"x2":0.241,"x3":1.35,"x4":-0.553,"x5":0.218},{"center":"1","gradient":1,"x1":-0.154,"x2":0.281,"x3":1.575,"x4":-0.645,"x5":0.254},{"center":"1","gradient":1,"x1":-0.176,"x2":0.321,"x3":1.8,"x4":-0.737,"x5":0.29},{"center":"1","gradient":1,"x1":-0.198,"x2":0.361,"x3":2.025,"x4":-0.83,"x5":0.327},{"center":"1","gradient":2,"x1":-0.024,"x2":0,"x3":0.228,"x4":-0.094,"x5":0.033},{"center":"1","gradient":2,"x1":-0.048,"x2":0,"x3":0.456,"x4":-0.189,"x5":0.065},{"center":"1","gradient":2,"x1":-0.072,"x2":0,"x3":0.684,"x4":-0.283,"x5":0.098},{"center":"1","gradient":2,"x1":-0.096,"x2":0,"x3":0.912,"x4":-0.377,"x5":0.13},{"center":"1","gradient":2,"x1":-0.12,"x2":0,"x3":1.14,"x4":-0.472,"x5":0.163},{"center":"1","gradient":2,"x1":-0.144,"x2":0,"x3":1.368,"x4":-0.566,"x5":0.195},{"center":"1","gradient":2,"x1":-0.168,"x2":0,"x3":1.596,"x4":-0.661,"x5":0.228},{"center":"1","gradient":2,"x1":-0.192,"x2":0,"x3":1.824,"x4":-0.755,"x5":0.26},{"center":"1","gradient":2,"x1":-0.216,"x2":0,"x3":2.052,"x4":-0.849,"x5":0.293},{"center":"1","gradient":3,"x1":-0.012,"x2":0.079,"x3":0.208,"x4":-0.094,"x5":0.063},{"center":"1","gradient":3,"x1":-0.027,"x2":0.191,"x3":0.404,"x4":-0.187,"x5":0.122},{"center":"1","gradient":3,"x1":-0.044,"x2":0.339,"x3":0.585,"x4":-0.27,"x5":0.174},{"center":"1","gradient":3,"x1":-0.062,"x2":0.523,"x3":0.752,"x4":-0.334,"x5":0.213},{"center":"1","gradient":3,"x1":-0.079,"x2":0.738,"x3":0.903,"x4":-0.375,"x5":0.238},{"center":"1","gradient":3,"x1":-0.095,"x2":0.974,"x3":1.038,"x4":-0.392,"x5":0.25},{"center":"1","gradient":3,"x1":-0.108,"x2":1.223,"x3":1.159,"x4":-0.388,"x5":0.251},{"center":"1","gradient":3,"x1":-0.12,"x2":1.477,"x3":1.269,"x4":-0.37,"x5":0.244},{"center":"1","gradient":3,"x1":-0.13,"x2":1.731,"x3":1.369,"x4":-0.34,"x5":0.231},{"center":"1","gradient":3,"x1":0.02,"x2":-0.075,"x3":-0.452,"x4":0.165,"x5":-0.109},{"center":"1","gradient":3,"x1":0.01,"x2":-0.05,"x3":-0.22,"x4":0.088,"x5":-0.06},{"center":"1","gradient":4,"x1":-0.026,"x2":0,"x3":0.22,"x4":-0.107,"x5":0.043},{"center":"1","gradient":4,"x1":-0.054,"x2":0,"x3":0.43,"x4":-0.232,"x5":0.09},{"center":"1","gradient":4,"x1":-0.083,"x2":0,"x3":0.628,"x4":-0.376,"x5":0.139},{"center":"1","gradient":4,"x1":-0.115,"x2":0,"x3":0.813,"x4":-0.537,"x5":0.192},{"center":"1","gradient":4,"x1":-0.148,"x2":0,"x3":0.985,"x4":-0.714,"x5":0.248},{"center":"1","gradient":4,"x1":-0.182,"x2":0,"x3":1.144,"x4":-0.903,"x5":0.306},{"center":"1","gradient":4,"x1":-0.217,"x2":0,"x3":1.291,"x4":-1.103,"x5":0.366},{"center":"1","gradient":4,"x1":-0.253,"x2":0,"x3":1.426,"x4":-1.31,"x5":0.427},{"center":"1","gradient":4,"x1":-0.29,"x2":0,"x3":1.554,"x4":-1.526,"x5":0.49},{"center":"1","gradient":4,"x1":0.05,"x2":0,"x3":-0.465,"x4":0.158,"x5":-0.078},{"center":"1","gradient":4,"x1":0.025,"x2":0,"x3":-0.229,"x4":0.088,"x5":-0.04},{"center":"1","gradient":0,"x1":0,"x2":0,"x3":0,"x4":0,"x5":0},{"center":"1","gradient":0,"x1":0,"x2":0,"x3":0,"x4":0,"x5":0},{"center":"1","gradient":0,"x1":0,"x2":0,"x3":0,"x4":0,"x5":0},{"center":"1","gradient":0,"x1":0,"x2":0,"x3":0,"x4":0,"x5":0},{"center":"1","gradient":0,"x1":0,"x2":0,"x3":0,"x4":0,"x5":0},{"center":"1","gradient":0,"x1":0,"x2":0,"x3":0,"x4":0,"x5":0},{"center":"1","gradient":-1,"x1":0,"x2":1.5,"x3":0,"x4":0,"x5":0},{"center":"1","gradient":-1,"x1":0,"x2":0,"x3":0,"x4":-1.5,"x5":0},{"center":"1","gradient":-1,"x1":1,"x2":1,"x3":1,"x4":1,"x5":-1},{"center":"1","gradient":-1,"x1":0,"x2":0,"x3":0,"x4":0,"x5":0},{"center":"1","gradient":-1,"x1":0,"x2":0,"x3":1.5,"x4":0,"x5":0},{"center":"1","gradient":-1,"x1":1,"x2":-1,"x3":1,"x4":-1,"x5":1},{"center":"1","gradient":-1,"x1":0,"x2":0,"x3":1.5,"x4":0,"x5":0},{"center":"1","gradient":-1,"x1":1,"x2":1,"x3":1,"x4":-1,"x5":1}];
// console.log(rows1);
//const rows2 = createFractional_5_1(2, alpha, 6)
//const rows3 = createFractional_5_1(1, alpha, 6)
const rows2 = [{"center":"2","gradient":1,"x1":-0.163,"x2":0.067,"x3":0.127,"x4":-0.118,"x5":0.036},{"center":"2","gradient":1,"x1":-0.327,"x2":0.133,"x3":0.253,"x4":-0.236,"x5":0.073},{"center":"2","gradient":1,"x1":-0.49,"x2":0.2,"x3":0.38,"x4":-0.355,"x5":0.109},{"center":"2","gradient":1,"x1":-0.654,"x2":0.267,"x3":0.507,"x4":-0.473,"x5":0.145},{"center":"2","gradient":1,"x1":-0.817,"x2":0.334,"x3":0.633,"x4":-0.591,"x5":0.181},{"center":"2","gradient":1,"x1":-0.981,"x2":0.4,"x3":0.76,"x4":-0.709,"x5":0.218},{"center":"2","gradient":1,"x1":-1.144,"x2":0.467,"x3":0.887,"x4":-0.827,"x5":0.254},{"center":"2","gradient":1,"x1":-1.307,"x2":0.534,"x3":1.013,"x4":-0.945,"x5":0.29},{"center":"2","gradient":1,"x1":-1.472,"x2":0.601,"x3":1.141,"x4":-1.065,"x5":0.327},{"center":"2","gradient":2,"x1":-0.166,"x2":0.069,"x3":0.129,"x4":-0.117,"x5":0},{"center":"2","gradient":2,"x1":-0.332,"x2":0.137,"x3":0.258,"x4":-0.233,"x5":0},{"center":"2","gradient":2,"x1":-0.498,"x2":0.206,"x3":0.387,"x4":-0.35,"x5":0},{"center":"2","gradient":2,"x1":-0.664,"x2":0.275,"x3":0.516,"x4":-0.466,"x5":0},{"center":"2","gradient":2,"x1":-0.83,"x2":0.343,"x3":0.644,"x4":-0.583,"x5":0},{"center":"2","gradient":2,"x1":-0.996,"x2":0.412,"x3":0.774,"x4":-0.7,"x5":0},{"center":"2","gradient":2,"x1":-1.162,"x2":0.481,"x3":0.903,"x4":-0.817,"x5":0},{"center":"2","gradient":2,"x1":-1.328,"x2":0.549,"x3":1.031,"x4":-0.933,"x5":0},{"center":"2","gradient":2,"x1":-1.495,"x2":0.618,"x3":1.162,"x4":-1.051,"x5":0},{"center":"2","gradient":0,"x1":0,"x2":0,"x3":0,"x4":0,"x5":0},{"center":"2","gradient":0,"x1":0,"x2":0,"x3":0,"x4":0,"x5":0},{"center":"2","gradient":0,"x1":0,"x2":0,"x3":0,"x4":0,"x5":0},{"center":"2","gradient":0,"x1":0,"x2":0,"x3":0,"x4":0,"x5":0},{"center":"2","gradient":0,"x1":0,"x2":0,"x3":0,"x4":0,"x5":0},{"center":"2","gradient":0,"x1":0,"x2":0,"x3":0,"x4":0,"x5":0},{"center":"2","gradient":-1,"x1":-1,"x2":1,"x3":1,"x4":-1,"x5":1},{"center":"2","gradient":-1,"x1":-1,"x2":-1,"x3":1,"x4":-1,"x5":-1}];

const sources = {
	buffer: 50,
	strain: 20,
	glucose: 10,
	nitrogen1: 50,
	nitrogen2: 50,
	trace1: 10,
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

const design2 = makeDesign(2, rows2, centers1, deltas1); // ERROR FIXME: should be `centers3` and `deltas3`
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
