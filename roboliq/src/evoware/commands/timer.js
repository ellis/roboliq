import _ from 'lodash';
import math from 'mathjs';
import commandHelper from '../../commandHelper.js';
//import evowareHelper from './evowareHelper.js';

export function _start(params, parsed, data) {
	const id = commandHelper.lookupPath(["@equipment", "evowareId"], params, data);
	const line = `StartTimer("${id}");`;
	return [{ line }];
}

export function _wait(params, parsed, data) {
	console.log(JSON.stringify(parsed, null, '\t'))
	const id = commandHelper.lookupPath(["@equipment", "evowareId"], params, data);
	const seconds = math.round(parsed.value.till.toNumber('s'));
	const result = [{line: `WaitTimer("${id}","${seconds}");`}];
	if (parsed.value.stop === true)
		result.push({line: `StopTimer("${id}");`});
	return result;
}
