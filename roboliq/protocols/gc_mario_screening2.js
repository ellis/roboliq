const fractional_5_1 = {
	x1: [-1,  1, -1,  1, -1,  1, -1,  1, -1,  1, -1,  1, -1,  1, -1,  1],
	x2: [-1, -1,  1,  1, -1, -1,  1,  1, -1, -1,  1,  1, -1, -1,  1,  1],
	x3: [-1, -1, -1, -1,  1,  1,  1,  1, -1, -1, -1, -1,  1,  1,  1,  1],
	x4: [-1, -1, -1, -1, -1, -1, -1, -1,  1,  1,  1,  1,  1,  1,  1,  1],
	x5: [ 1, -1, -1,  1, -1,  1,  1, -1, -1,  1,  1, -1,  1, -1, -1,  1]
};

function addAxialRuns(cols, alpha) {
	const keys = _.keys(cols);
	const n = keys.length;
	const a = (alpha) ? alpha : sqrt(sqrt(n));
	for (let i = 0; i < n; i++) {
		const key = keys[i];
		const axialValues = Array(n * 2);
		_.fill(axialValues, 0);
		axialValues[i*n] = -a;
		axialValues[i*n + 1] = a;
		cols[key].push(...axialValues);
	}
}

function addCenterRuns(cols, n) {
	const l0 = _.fill(Array(n), 0);
	_.forEach(cols, (l) => {
		l.push(...l0);
	});
}
