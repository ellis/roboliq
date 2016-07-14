import _ from 'lodash';

const fractional_5_1a = {
	x1: [-1,  1, -1,  1, -1,  1, -1,  1, -1,  1, -1,  1, -1,  1, -1,  1],
	x2: [-1, -1,  1,  1, -1, -1,  1,  1, -1, -1,  1,  1, -1, -1,  1,  1],
	x3: [-1, -1, -1, -1,  1,  1,  1,  1, -1, -1, -1, -1,  1,  1,  1,  1],
	x4: [-1, -1, -1, -1, -1, -1, -1, -1,  1,  1,  1,  1,  1,  1,  1,  1],
	x5: [ 1, -1, -1,  1, -1,  1,  1, -1, -1,  1,  1, -1,  1, -1, -1,  1]
};
const fractional_5_1b = _.set(_.cloneDeep(fractional_5_1a), "x5", fractional_5_1a.x5.map(x => -1));

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

const rows1 = createFractional_5_1(1, undefined, 6)
console.log(rows1);
const rows2 = createFractional_5_1(2, undefined, 6)
const rows3 = createFractional_5_1(1, undefined, 6)
