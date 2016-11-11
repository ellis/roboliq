// import mergeWith from 'lodash/mergeWith';

/*
* Recursively merge properties of multiple objects (shallow copies)
*/
function mergeR() {
	let result = arguments[0],
			i = 0,
			n = arguments.length,
			key;
	for (let i = 1; i < n; i++) {
		const o = arguments[i];
		for (key in o) {
			if (o.hasOwnProperty(key)) {
				const x = o[key];
				const has = result.hasOwnProperty(key);
				// Remove 'null' values from result
				if (x === null) {
					// Remove from result
					if (has)
						delete result[key];
				}
				// Ignore undefined value in sources
				else if (x === undefined) {
					// Ignore
				}
				// Otherwise, see how to update result's value:
				else {
					// If result doesn't have the value yet, then set it.
					if (!has) {
						result[key] = x;
					}
					else {
						const x0 = result[key];
						if (isPlainObject(x)) {
							mergeR(x0, x);
						}
						// Replace any result values that aren't objects
						else {
							result[key] = x;
						}
					}
				}
			}
		}
	}
	return result;
}

module.exports = {
	mergeR
}
