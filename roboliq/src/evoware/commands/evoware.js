import _ from 'lodash';
import commandHelper from '../../commandHelper.js';
import evowareHelper from './evowareHelper.js';

export function _facts(step, objects, protocol, path) {
	const line = evowareHelper.createFactsLine(step.factsEquipment, step.factsVariable, step.factsValue);
	return [{
		line
	}];
}
