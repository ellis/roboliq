'use strict';

const _ = require('lodash');
const fs = require('fs');
const math = require('mathjs');
const path = require('path');

math.config({
	number: 'BigNumber', // Default type of number
	precision: 64        // Number of significant digits for BigNumbers
});

/**
 * Convert quantities with units to plain numbers
 */
function handleOutputUnits(output, table) {
	if (output.units) {
		_.forEach(table, row => {
			_.forEach(output.units, (units, key) => {
				if (_.has(row, key)) {
					// console.log(row)
					// console.log({key, units, value: row[key]});
					try {
						const x = math.eval(row[key]);
						const n = x.toNumber(units);
						// console.log({x, n})
						row[key] = n;
						row[key+"_units"] = units;
						// console.log({new: row[key]})
					}
					catch (e) { console.log(e.toString()); }
				}
			});
		});
	}
}

/**
 * Save the JSON file
 */
function handleOutputWriteTo(output, table, runDir, prefix) {
	if (!_.isEmpty(output.writeTo)) {
		const filename = path.join(runDir, `${prefix}${output.writeTo}.json`);
		fs.writeFileSync(filename, JSON.stringify(table, null, '\t'));
	}
}

function handleOutputAppendTo(output, table, runDir) {
	const appendTo = _.get(output, "appendTo");
	if (!_.isEmpty(appendTo) && !_.isEmpty(table)) {
		const appendToFile = path.join(runDir, appendTo+".jsonl");
		const contents = table.map(s => JSON.stringify(s)).join("\n") + "\n";
		fs.appendFileSync(appendToFile, contents);

		// HACK to copy data to shared drive
		if (fs.existsSync("Y:\\Projects\\Roboliq")) {
			const hackDir = path.join("Y:\\Projects\\Roboliq\\labdata", scriptBase, runId);
			console.log({hackDir})
			mkdirp.sync(hackDir);
			const hackFile = path.join(hackDir, appendTo+".jsonl");
			console.log("writing duplicate file to: "+hackFile)
			const contents = table.map(s => JSON.stringify(s)).join("\n") + "\n";
			fs.appendFileSync(hackFile, contents);
		}
	}
}

module.exports = {
	handleOutputUnits: handleOutputUnits,
	handleOutputWriteTo: handleOutputWriteTo,
	handleOutputAppendTo: handleOutputAppendTo,
};
