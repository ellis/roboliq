import _ from 'lodash';
import commandHelper from '../../commandHelper.js';
import evowareHelper from './evowareHelper.js';

export function _facts(params, parsed, data) {
	const line = evowareHelper.createFactsLine(parsed.orig.factsEquipment, parsed.orig.factsVariable, parsed.orig.factsValue);
	return [{ line }];
}
