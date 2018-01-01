/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017, ETH Zurich, Ellis Whitehead
 * @license GPL-3.0
 */

const _ = require('lodash');
const math = require('mathjs');
const commandHelper = require('roboliq-processor/src/commandHelper.js');

function _sleep(params, parsed, data) {
	//console.log("timer._sleep: "+JSON.stringify(parsed, null, '\t'))
	const id = commandHelper.lookupPath(["@equipment", "evowareId"], params, data);
	const seconds = math.round(parsed.value.duration.toNumber('s'));
	return [
		{line: `StartTimer("${id}");`},
		{line: `WaitTimer("${id}","${seconds}");`}
	];
}

function _start(params, parsed, data) {
	const id = commandHelper.lookupPath(["@equipment", "evowareId"], params, data);
	const line = `StartTimer("${id}");`;
	return [{ line }];
}

function _wait(params, parsed, data) {
	//console.log(JSON.stringify(parsed, null, '\t'))
	const id = commandHelper.lookupPath(["@equipment", "evowareId"], params, data);
	const seconds = math.round(parsed.value.till.toNumber('s'));
	const result = [{line: `WaitTimer("${id}","${seconds}");`}];
	// There is no Evoware command for stopping the timer, so we can't emit a StopTimer command.
	//if (parsed.value.stop === true)
	//	result.push({line: `StopTimer("${id}");`});
	return result;
}

module.exports = {
  _sleep,
  _start,
  _wait,
};
