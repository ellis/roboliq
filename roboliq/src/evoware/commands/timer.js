import _ from 'lodash';
import commandHelper from '../../commandHelper.js';
//import evowareHelper from './evowareHelper.js';

export function _start(params, parsed, data) {
	const id = commandHelper.lookupPath(["@equipment", "evowareId"], params, data);
	const line = `StartTimer("${id}");`;
	return [{ line }];
}

export function _wait(params, parsed, data) {
	const id = commandHelper.lookupPath(["@equipment", "evowareId"], params, data);
	const line = `WaitTimer("${id}","${step.till}");`;
	return [{ line }];
}
