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
const rows1 = [{"center":"1","gradient":0,"step":0,"x1":0,"x2":0,"x3":0,"x4":0,"x5":0,"glucoseC":1.5,"nitrogen1C":1.5,"nitrogen2C":1.5,"trace1C":1.5,"trace2C":1.5},{"center":"1","gradient":0,"step":0,"x1":0,"x2":0,"x3":0,"x4":0,"x5":0,"glucoseC":1.5,"nitrogen1C":1.5,"nitrogen2C":1.5,"trace1C":1.5,"trace2C":1.5},{"center":"1","gradient":0,"step":0,"x1":0,"x2":0,"x3":0,"x4":0,"x5":0,"glucoseC":1.5,"nitrogen1C":1.5,"nitrogen2C":1.5,"trace1C":1.5,"trace2C":1.5},{"center":"1","gradient":1,"step":-2,"x1":-0.623,"x2":-0.194,"x3":0.021,"x4":0.099,"x5":-0.09,"glucoseC":1.1885,"nitrogen1C":1.403,"nitrogen2C":1.5315,"trace1C":1.5495,"trace2C":1.365},{"center":"1","gradient":1,"step":-1,"x1":-0.312,"x2":-0.097,"x3":0.011,"x4":0.05,"x5":-0.045,"glucoseC":1.344,"nitrogen1C":1.4515,"nitrogen2C":1.5165,"trace1C":1.525,"trace2C":1.4325},{"center":"1","gradient":1,"step":1,"x1":0.312,"x2":0.097,"x3":-0.011,"x4":-0.05,"x5":0.045,"glucoseC":1.656,"nitrogen1C":1.5485,"nitrogen2C":1.4835,"trace1C":1.475,"trace2C":1.5675},{"center":"1","gradient":1,"step":2,"x1":0.623,"x2":0.194,"x3":-0.021,"x4":-0.099,"x5":0.09,"glucoseC":1.8115,"nitrogen1C":1.597,"nitrogen2C":1.4685,"trace1C":1.4505,"trace2C":1.635},{"center":"1","gradient":1,"step":3,"x1":0.934,"x2":0.292,"x3":-0.032,"x4":-0.149,"x5":0.136,"glucoseC":1.967,"nitrogen1C":1.646,"nitrogen2C":1.452,"trace1C":1.4255,"trace2C":1.704},{"center":"1","gradient":1,"step":4,"x1":1.246,"x2":0.389,"x3":-0.043,"x4":-0.198,"x5":0.181,"glucoseC":2.123,"nitrogen1C":1.6945,"nitrogen2C":1.4355,"trace1C":1.401,"trace2C":1.7715},{"center":"1","gradient":1,"step":5,"x1":1.557,"x2":0.486,"x3":-0.054,"x4":-0.248,"x5":0.226,"glucoseC":2.2785,"nitrogen1C":1.743,"nitrogen2C":1.419,"trace1C":1.376,"trace2C":1.839},{"center":"1","gradient":1,"step":6,"x1":1.869,"x2":0.583,"x3":-0.064,"x4":-0.297,"x5":0.271,"glucoseC":2.4345,"nitrogen1C":1.7915,"nitrogen2C":1.404,"trace1C":1.3515,"trace2C":1.9065},{"center":"1","gradient":1,"step":7,"x1":2.181,"x2":0.681,"x3":-0.075,"x4":-0.347,"x5":0.316,"glucoseC":2.5905,"nitrogen1C":1.8405,"nitrogen2C":1.3875,"trace1C":1.3265,"trace2C":1.974},{"center":"1","gradient":1,"step":8,"x1":2.492,"x2":0.778,"x3":-0.086,"x4":-0.397,"x5":0.362,"glucoseC":2.746,"nitrogen1C":1.889,"nitrogen2C":1.371,"trace1C":1.3015,"trace2C":2.043},{"center":"1","gradient":1,"step":9,"x1":2.804,"x2":0.875,"x3":-0.096,"x4":-0.446,"x5":0.407,"glucoseC":2.902,"nitrogen1C":1.9375,"nitrogen2C":1.356,"trace1C":1.277,"trace2C":2.1105},{"center":"1","gradient":2,"step":-2,"x1":-0.573,"x2":-0.286,"x3":0.101,"x4":0.078,"x5":-0.134,"glucoseC":1.2135,"nitrogen1C":1.357,"nitrogen2C":1.6515,"trace1C":1.539,"trace2C":1.299},{"center":"1","gradient":2,"step":-1,"x1":-0.294,"x2":-0.134,"x3":0.039,"x4":0.036,"x5":-0.064,"glucoseC":1.353,"nitrogen1C":1.433,"nitrogen2C":1.5585,"trace1C":1.518,"trace2C":1.404},{"center":"1","gradient":2,"step":1,"x1":0.304,"x2":0.12,"x3":-0.017,"x4":-0.022,"x5":0.059,"glucoseC":1.652,"nitrogen1C":1.56,"nitrogen2C":1.4745,"trace1C":1.489,"trace2C":1.5885},{"center":"1","gradient":2,"step":2,"x1":0.614,"x2":0.233,"x3":-0.012,"x4":-0.023,"x5":0.111,"glucoseC":1.807,"nitrogen1C":1.6165,"nitrogen2C":1.482,"trace1C":1.4885,"trace2C":1.6665},{"center":"1","gradient":2,"step":3,"x1":0.925,"x2":0.347,"x3":0.025,"x4":0.004,"x5":0.154,"glucoseC":1.9625,"nitrogen1C":1.6735,"nitrogen2C":1.5375,"trace1C":1.502,"trace2C":1.731},{"center":"1","gradient":2,"step":4,"x1":1.224,"x2":0.482,"x3":0.11,"x4":0.065,"x5":0.175,"glucoseC":2.112,"nitrogen1C":1.741,"nitrogen2C":1.665,"trace1C":1.5325,"trace2C":1.7625},{"center":"1","gradient":2,"step":5,"x1":1.481,"x2":0.676,"x3":0.28,"x4":0.168,"x5":0.149,"glucoseC":2.2405,"nitrogen1C":1.838,"nitrogen2C":1.92,"trace1C":1.584,"trace2C":1.7235},{"center":"1","gradient":2,"step":6,"x1":1.648,"x2":0.946,"x3":0.546,"x4":0.298,"x5":0.055,"glucoseC":2.324,"nitrogen1C":1.973,"nitrogen2C":2.319,"trace1C":1.649,"trace2C":1.5825},{"center":"1","gradient":2,"step":7,"x1":1.732,"x2":1.243,"x3":0.846,"x4":0.425,"x5":-0.076,"glucoseC":2.366,"nitrogen1C":2.1215,"nitrogen2C":2.769,"trace1C":1.7125,"trace2C":1.386},{"center":"1","gradient":2,"step":8,"x1":1.773,"x2":1.53,"x3":1.136,"x4":0.539,"x5":-0.215,"glucoseC":2.3865,"nitrogen1C":2.265,"nitrogen2C":3.204,"trace1C":1.7695,"trace2C":1.1775},{"center":"1","gradient":2,"step":9,"x1":1.796,"x2":1.8,"x3":1.41,"x4":0.643,"x5":-0.35,"glucoseC":2.398,"nitrogen1C":2.4,"nitrogen2C":3.615,"trace1C":1.8215,"trace2C":0.975},{"center":"1","gradient":3,"step":-2,"x1":-0.598,"x2":-0.197,"x3":0.042,"x4":0.121,"x5":-0.176,"glucoseC":1.201,"nitrogen1C":1.4015,"nitrogen2C":1.563,"trace1C":1.5605,"trace2C":1.236},{"center":"1","gradient":3,"step":-1,"x1":-0.301,"x2":-0.11,"x3":0.023,"x4":0.05,"x5":-0.074,"glucoseC":1.3495,"nitrogen1C":1.445,"nitrogen2C":1.5345,"trace1C":1.525,"trace2C":1.389},{"center":"1","gradient":3,"step":1,"x1":0.291,"x2":0.153,"x3":-0.008,"x4":-0.027,"x5":0.049,"glucoseC":1.6455,"nitrogen1C":1.5765,"nitrogen2C":1.488,"trace1C":1.4865,"trace2C":1.5735},{"center":"1","gradient":3,"step":2,"x1":0.536,"x2":0.388,"x3":0.035,"x4":-0.03,"x5":0.065,"glucoseC":1.768,"nitrogen1C":1.694,"nitrogen2C":1.5525,"trace1C":1.485,"trace2C":1.5975},{"center":"1","gradient":3,"step":3,"x1":0.685,"x2":0.711,"x3":0.152,"x4":-0.015,"x5":0.043,"glucoseC":1.8425,"nitrogen1C":1.8555,"nitrogen2C":1.728,"trace1C":1.4925,"trace2C":1.5645},{"center":"1","gradient":3,"step":4,"x1":0.752,"x2":1.057,"x3":0.307,"x4":0.006,"x5":0.001,"glucoseC":1.876,"nitrogen1C":2.0285,"nitrogen2C":1.9605,"trace1C":1.503,"trace2C":1.5015},{"center":"1","gradient":3,"step":5,"x1":0.782,"x2":1.394,"x3":0.469,"x4":0.026,"x5":-0.046,"glucoseC":1.891,"nitrogen1C":2.197,"nitrogen2C":2.2035,"trace1C":1.513,"trace2C":1.431},{"center":"1","gradient":3,"step":6,"x1":0.795,"x2":1.72,"x3":0.631,"x4":0.046,"x5":-0.095,"glucoseC":1.8975,"nitrogen1C":2.36,"nitrogen2C":2.4465,"trace1C":1.523,"trace2C":1.3575},{"center":"1","gradient":3,"step":7,"x1":0.799,"x2":2.039,"x3":0.792,"x4":0.065,"x5":-0.144,"glucoseC":1.8995,"nitrogen1C":2.5195,"nitrogen2C":2.688,"trace1C":1.5325,"trace2C":1.284},{"center":"1","gradient":3,"step":8,"x1":0.799,"x2":2.351,"x3":0.951,"x4":0.084,"x5":-0.193,"glucoseC":1.8995,"nitrogen1C":2.6755,"nitrogen2C":2.9265,"trace1C":1.542,"trace2C":1.2105},{"center":"1","gradient":3,"step":9,"x1":0.795,"x2":2.656,"x3":1.108,"x4":0.102,"x5":-0.241,"glucoseC":1.8975,"nitrogen1C":2.828,"nitrogen2C":3.162,"trace1C":1.551,"trace2C":1.1385},{"center":"1","gradient":-1,"step":0,"x1":0,"x2":1.5,"x3":0,"x4":0,"x5":0,"glucoseC":1.5,"nitrogen1C":2.25,"nitrogen2C":1.5,"trace1C":1.5,"trace2C":1.5},{"center":"1","gradient":-1,"step":0,"x1":1,"x2":1,"x3":1,"x4":1,"x5":-1,"glucoseC":2,"nitrogen1C":2,"nitrogen2C":3,"trace1C":2,"trace2C":0},{"center":"1","gradient":-1,"step":0,"x1":1,"x2":-1,"x3":-1,"x4":-1,"x5":1,"glucoseC":2,"nitrogen1C":1,"nitrogen2C":0,"trace1C":1,"trace2C":3},{"center":"1","gradient":-1,"step":0,"x1":1.5,"x2":0,"x3":0,"x4":0,"x5":0,"glucoseC":2.25,"nitrogen1C":1.5,"nitrogen2C":1.5,"trace1C":1.5,"trace2C":1.5},{"center":"1","gradient":-1,"step":0,"x1":1,"x2":-1,"x3":1,"x4":-1,"x5":1,"glucoseC":2,"nitrogen1C":1,"nitrogen2C":3,"trace1C":1,"trace2C":3},{"center":"1","gradient":-1,"step":0,"x1":1,"x2":1,"x3":1,"x4":-1,"x5":1,"glucoseC":2,"nitrogen1C":2,"nitrogen2C":3,"trace1C":1,"trace2C":3},{"center":"1","gradient":-1,"step":0,"x1":0,"x2":0,"x3":0,"x4":0,"x5":0,"glucoseC":1.5,"nitrogen1C":1.5,"nitrogen2C":1.5,"trace1C":1.5,"trace2C":1.5},{"center":"1","gradient":-1,"step":0,"x1":0,"x2":0,"x3":0,"x4":-1.5,"x5":0,"glucoseC":1.5,"nitrogen1C":1.5,"nitrogen2C":1.5,"trace1C":0.75,"trace2C":1.5},{"center":"1","gradient":-1,"step":0,"x1":0,"x2":0,"x3":0,"x4":0,"x5":0,"glucoseC":1.5,"nitrogen1C":1.5,"nitrogen2C":1.5,"trace1C":1.5,"trace2C":0}];
// console.log(rows1);
//const rows2 = createFractional_5_1(2, alpha, 6)
//const rows3 = createFractional_5_1(1, alpha, 6)
const rows2 = [
	{"center":"2","gradient":0,"step":0,"x1":0,"x2":0,"x3":0,"x4":0,"x5":0,"glucoseC":2.25,"nitrogen1C":2.75,"nitrogen2C":2.5,"trace1C":2,"trace2C":2.5},{"center":"2","gradient":0,"step":0,"x1":0,"x2":0,"x3":0,"x4":0,"x5":0,"glucoseC":2.25,"nitrogen1C":2.75,"nitrogen2C":2.5,"trace1C":2,"trace2C":2.5},{"center":"2","gradient":0,"step":0,"x1":0,"x2":0,"x3":0,"x4":0,"x5":0,"glucoseC":2.25,"nitrogen1C":2.75,"nitrogen2C":2.5,"trace1C":2,"trace2C":2.5},{"center":"2","gradient":4,"step":-2,"x1":-0.625,"x2":-0.136,"x3":0.15,"x4":-0.111,"x5":-0.024,"glucoseC":1.7812,"nitrogen1C":2.58,"nitrogen2C":2.725,"trace1C":1.889,"trace2C":2.464},{"center":"2","gradient":4,"step":-1,"x1":-0.313,"x2":-0.068,"x3":0.075,"x4":-0.056,"x5":-0.012,"glucoseC":2.0152,"nitrogen1C":2.665,"nitrogen2C":2.6125,"trace1C":1.944,"trace2C":2.482},{"center":"2","gradient":4,"step":1,"x1":0.313,"x2":0.068,"x3":-0.075,"x4":0.056,"x5":0.012,"glucoseC":2.4848,"nitrogen1C":2.835,"nitrogen2C":2.3875,"trace1C":2.056,"trace2C":2.518},{"center":"2","gradient":4,"step":2,"x1":0.625,"x2":0.136,"x3":-0.15,"x4":0.111,"x5":0.024,"glucoseC":2.7188,"nitrogen1C":2.92,"nitrogen2C":2.275,"trace1C":2.111,"trace2C":2.536},{"center":"2","gradient":4,"step":3,"x1":0.938,"x2":0.204,"x3":-0.225,"x4":0.167,"x5":0.035,"glucoseC":2.9535,"nitrogen1C":3.005,"nitrogen2C":2.1625,"trace1C":2.167,"trace2C":2.5525},{"center":"2","gradient":4,"step":4,"x1":1.25,"x2":0.272,"x3":-0.299,"x4":0.222,"x5":0.047,"glucoseC":3.1875,"nitrogen1C":3.09,"nitrogen2C":2.0515,"trace1C":2.222,"trace2C":2.5705},{"center":"2","gradient":4,"step":5,"x1":1.562,"x2":0.34,"x3":-0.374,"x4":0.278,"x5":0.059,"glucoseC":3.4215,"nitrogen1C":3.175,"nitrogen2C":1.939,"trace1C":2.278,"trace2C":2.5885},{"center":"2","gradient":4,"step":6,"x1":1.875,"x2":0.407,"x3":-0.449,"x4":0.334,"x5":0.071,"glucoseC":3.6562,"nitrogen1C":3.2588,"nitrogen2C":1.8265,"trace1C":2.334,"trace2C":2.6065},{"center":"2","gradient":4,"step":7,"x1":2.188,"x2":0.475,"x3":-0.524,"x4":0.389,"x5":0.083,"glucoseC":3.891,"nitrogen1C":3.3438,"nitrogen2C":1.714,"trace1C":2.389,"trace2C":2.6245},{"center":"2","gradient":4,"step":8,"x1":2.5,"x2":0.543,"x3":-0.599,"x4":0.445,"x5":0.094,"glucoseC":4.125,"nitrogen1C":3.4288,"nitrogen2C":1.6015,"trace1C":2.445,"trace2C":2.641},{"center":"2","gradient":4,"step":9,"x1":2.813,"x2":0.611,"x3":-0.674,"x4":0.5,"x5":0.106,"glucoseC":4.3598,"nitrogen1C":3.5137,"nitrogen2C":1.489,"trace1C":2.5,"trace2C":2.659},{"center":"2","gradient":5,"step":-2,"x1":-0.507,"x2":0.221,"x3":-0.216,"x4":-0.297,"x5":-0.06,"glucoseC":1.8698,"nitrogen1C":3.0263,"nitrogen2C":2.176,"trace1C":1.703,"trace2C":2.41},{"center":"2","gradient":5,"step":-1,"x1":-0.275,"x2":0.096,"x3":-0.095,"x4":-0.132,"x5":-0.014,"glucoseC":2.0438,"nitrogen1C":2.87,"nitrogen2C":2.3575,"trace1C":1.868,"trace2C":2.479},{"center":"2","gradient":5,"step":1,"x1":0.307,"x2":-0.058,"x3":0.062,"x4":0.087,"x5":-0.047,"glucoseC":2.4802,"nitrogen1C":2.6775,"nitrogen2C":2.593,"trace1C":2.087,"trace2C":2.4295},{"center":"2","gradient":5,"step":2,"x1":0.621,"x2":-0.085,"x3":0.095,"x4":0.128,"x5":-0.162,"glucoseC":2.7157,"nitrogen1C":2.6437,"nitrogen2C":2.6425,"trace1C":2.128,"trace2C":2.257},{"center":"2","gradient":5,"step":3,"x1":0.924,"x2":-0.099,"x3":0.114,"x4":0.129,"x5":-0.326,"glucoseC":2.943,"nitrogen1C":2.6263,"nitrogen2C":2.671,"trace1C":2.129,"trace2C":2.011},{"center":"2","gradient":5,"step":4,"x1":1.212,"x2":-0.118,"x3":0.135,"x4":0.092,"x5":-0.519,"glucoseC":3.159,"nitrogen1C":2.6025,"nitrogen2C":2.7025,"trace1C":2.092,"trace2C":1.7215},{"center":"2","gradient":5,"step":5,"x1":1.482,"x2":-0.158,"x3":0.175,"x4":0.006,"x5":-0.725,"glucoseC":3.3615,"nitrogen1C":2.5525,"nitrogen2C":2.7625,"trace1C":2.006,"trace2C":1.4125},{"center":"2","gradient":5,"step":6,"x1":1.728,"x2":-0.237,"x3":0.256,"x4":-0.148,"x5":-0.933,"glucoseC":3.546,"nitrogen1C":2.4537,"nitrogen2C":2.884,"trace1C":1.852,"trace2C":1.1005},{"center":"2","gradient":5,"step":7,"x1":1.935,"x2":-0.368,"x3":0.391,"x4":-0.384,"x5":-1.125,"glucoseC":3.7012,"nitrogen1C":2.29,"nitrogen2C":3.0865,"trace1C":1.616,"trace2C":0.8125},{"center":"2","gradient":5,"step":8,"x1":2.093,"x2":-0.538,"x3":0.57,"x4":-0.679,"x5":-1.285,"glucoseC":3.8198,"nitrogen1C":2.0775,"nitrogen2C":3.355,"trace1C":1.321,"trace2C":0.5725},{"center":"2","gradient":5,"step":9,"x1":2.212,"x2":-0.725,"x3":0.768,"x4":-0.994,"x5":-1.416,"glucoseC":3.909,"nitrogen1C":1.8438,"nitrogen2C":3.652,"trace1C":1.006,"trace2C":0.376},{"center":"2","gradient":6,"step":-2,"x1":-0.438,"x2":-0.038,"x3":0.069,"x4":-0.287,"x5":-0.405,"glucoseC":1.9215,"nitrogen1C":2.7025,"nitrogen2C":2.6035,"trace1C":1.713,"trace2C":1.8925},{"center":"2","gradient":6,"step":-1,"x1":-0.26,"x2":-0.052,"x3":0.062,"x4":-0.097,"x5":-0.166,"glucoseC":2.055,"nitrogen1C":2.685,"nitrogen2C":2.593,"trace1C":1.903,"trace2C":2.251},{"center":"2","gradient":6,"step":1,"x1":0.277,"x2":0.1,"x3":-0.095,"x4":-0.006,"x5":0.125,"glucoseC":2.4577,"nitrogen1C":2.875,"nitrogen2C":2.3575,"trace1C":1.994,"trace2C":2.6875},{"center":"2","gradient":6,"step":2,"x1":0.526,"x2":0.229,"x3":-0.212,"x4":-0.073,"x5":0.255,"glucoseC":2.6445,"nitrogen1C":3.0362,"nitrogen2C":2.182,"trace1C":1.927,"trace2C":2.8825},{"center":"2","gradient":6,"step":3,"x1":0.734,"x2":0.382,"x3":-0.347,"x4":-0.177,"x5":0.404,"glucoseC":2.8005,"nitrogen1C":3.2275,"nitrogen2C":1.9795,"trace1C":1.823,"trace2C":3.106},{"center":"2","gradient":6,"step":4,"x1":0.902,"x2":0.549,"x3":-0.494,"x4":-0.304,"x5":0.572,"glucoseC":2.9265,"nitrogen1C":3.4363,"nitrogen2C":1.759,"trace1C":1.696,"trace2C":3.358},{"center":"2","gradient":6,"step":5,"x1":1.036,"x2":0.724,"x3":-0.648,"x4":-0.443,"x5":0.751,"glucoseC":3.027,"nitrogen1C":3.655,"nitrogen2C":1.528,"trace1C":1.557,"trace2C":3.6265},{"center":"2","gradient":6,"step":6,"x1":1.143,"x2":0.903,"x3":-0.806,"x4":-0.589,"x5":0.938,"glucoseC":3.1073,"nitrogen1C":3.8788,"nitrogen2C":1.291,"trace1C":1.411,"trace2C":3.907},{"center":"2","gradient":6,"step":7,"x1":1.232,"x2":1.084,"x3":-0.965,"x4":-0.739,"x5":1.129,"glucoseC":3.174,"nitrogen1C":4.105,"nitrogen2C":1.0525,"trace1C":1.261,"trace2C":4.1935},{"center":"2","gradient":6,"step":8,"x1":1.308,"x2":1.264,"x3":-1.124,"x4":-0.89,"x5":1.323,"glucoseC":3.231,"nitrogen1C":4.33,"nitrogen2C":0.814,"trace1C":1.11,"trace2C":4.4845},{"center":"2","gradient":6,"step":9,"x1":1.373,"x2":1.445,"x3":-1.282,"x4":-1.041,"x5":1.517,"glucoseC":3.2797,"nitrogen1C":4.5563,"nitrogen2C":0.577,"trace1C":0.959,"trace2C":4.7755},{"center":"2","gradient":7,"step":-1.25,"x1":-0.319,"x2":-0.927,"x3":-0.813,"x4":1.363,"x5":2.33,"glucoseC":2.0107,"nitrogen1C":1.5913,"nitrogen2C":1.2805,"trace1C":3.363,"trace2C":5.995},{"center":"2","gradient":7,"step":-1,"x1":-0.293,"x2":-1.039,"x3":-0.692,"x4":1.181,"x5":2.29,"glucoseC":2.0303,"nitrogen1C":1.4513,"nitrogen2C":1.462,"trace1C":3.181,"trace2C":5.935},{"center":"2","gradient":7,"step":-0.75,"x1":-0.267,"x2":-1.151,"x3":-0.572,"x4":0.999,"x5":2.25,"glucoseC":2.0497,"nitrogen1C":1.3113,"nitrogen2C":1.642,"trace1C":2.999,"trace2C":5.875},{"center":"2","gradient":7,"step":-0.5,"x1":-0.241,"x2":-1.263,"x3":-0.452,"x4":0.816,"x5":2.21,"glucoseC":2.0692,"nitrogen1C":1.1713,"nitrogen2C":1.822,"trace1C":2.816,"trace2C":5.815},{"center":"2","gradient":7,"step":-0.25,"x1":-0.215,"x2":-1.374,"x3":-0.332,"x4":0.634,"x5":2.17,"glucoseC":2.0888,"nitrogen1C":1.0325,"nitrogen2C":2.002,"trace1C":2.634,"trace2C":5.755},{"center":"2","gradient":7,"step":0,"x1":-0.189,"x2":-1.486,"x3":-0.211,"x4":0.452,"x5":2.131,"glucoseC":2.1082,"nitrogen1C":0.8925,"nitrogen2C":2.1835,"trace1C":2.452,"trace2C":5.6965},{"center":"2","gradient":7,"step":0,"x1":-0.189,"x2":-1.486,"x3":-0.211,"x4":0.452,"x5":2.131,"glucoseC":2.1082,"nitrogen1C":0.8925,"nitrogen2C":2.1835,"trace1C":2.452,"trace2C":5.6965},{"center":"2","gradient":7,"step":0,"x1":-0.189,"x2":-1.486,"x3":-0.211,"x4":0.452,"x5":2.131,"glucoseC":2.1082,"nitrogen1C":0.8925,"nitrogen2C":2.1835,"trace1C":2.452,"trace2C":5.6965},{"center":"2","gradient":7,"step":0,"x1":-0.189,"x2":-1.486,"x3":-0.211,"x4":0.452,"x5":2.131,"glucoseC":2.1082,"nitrogen1C":0.8925,"nitrogen2C":2.1835,"trace1C":2.452,"trace2C":5.6965},{"center":"2","gradient":7,"step":0.25,"x1":-0.162,"x2":-1.598,"x3":-0.091,"x4":0.269,"x5":2.091,"glucoseC":2.1285,"nitrogen1C":0.7525,"nitrogen2C":2.3635,"trace1C":2.269,"trace2C":5.6365},{"center":"2","gradient":7,"step":0.5,"x1":-0.136,"x2":-1.71,"x3":0.029,"x4":0.087,"x5":2.051,"glucoseC":2.148,"nitrogen1C":0.6125,"nitrogen2C":2.5435,"trace1C":2.087,"trace2C":5.5765},{"center":"2","gradient":7,"step":0.75,"x1":-0.11,"x2":-1.822,"x3":0.149,"x4":-0.096,"x5":2.011,"glucoseC":2.1675,"nitrogen1C":0.4725,"nitrogen2C":2.7235,"trace1C":1.904,"trace2C":5.5165},{"center":"2","gradient":7,"step":1,"x1":-0.084,"x2":-1.934,"x3":0.27,"x4":-0.278,"x5":1.971,"glucoseC":2.187,"nitrogen1C":0.3325,"nitrogen2C":2.905,"trace1C":1.722,"trace2C":5.4565},{"center":"2","gradient":7,"step":1.25,"x1":-0.058,"x2":-2.045,"x3":0.39,"x4":-0.46,"x5":1.931,"glucoseC":2.2065,"nitrogen1C":0.1938,"nitrogen2C":3.085,"trace1C":1.54,"trace2C":5.3965},{"center":"2","gradient":-1,"step":0,"x1":1,"x2":1,"x3":1,"x4":-1,"x5":-1,"glucoseC":3,"nitrogen1C":4,"nitrogen2C":4,"trace1C":1,"trace2C":1}
];

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
			fullVolume: "200 ul",
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
		}
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
