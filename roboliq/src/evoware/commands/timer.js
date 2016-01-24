import _ from 'lodash';
import commandHelper from '../../commandHelper.js';
import evowareHelper from './evowareHelper.js';

export function _start(step, objects, protocol, path) {
	const data = {objects, effects: {}, accesses: []};
	const id = commandHelper.lookupPath(["@equipment", "evowareId"], step, data);
	const line = `StartTimer("${id}");`;
	return [{ line }];
}

export function _wait(step, objects, protocol, path) {
	const data = {objects, effects: {}, accesses: []};
	const id = commandHelper.lookupPath(["@equipment", "evowareId"], step, data);
	const line = `WaitTimer("${id}","${step.till}");`;
	return [{ line }];
}
