import _ from 'lodash';
import assert from 'assert';

/**
 * Lookup values referenced by the paths array.
 * @param  {object} paths - a map whose keys are name to use in the return object, with values of the form `{path: [...], optional: true|false}`.
 * @param  {object} params - original parameter map
 * @param  {object} data - data object with properties `objects`, `protocol`, and `path`
 * @return {object} a object of lookups
 */
export function lookupSpecs(specs, params, data) {
	const result = {};
	_.forEach(specs, (spec, name) => {

		result[name] =
	});
}

/**
 * Lookup nested paths 	// plateModelName: "((@object).model).evowareName: string"
 	// plateModelName: [["@object", "model"], "evowareName"]

 * @param  {[type]} path   [description]
 * @param  {[type]} params [description]
 * @param  {[type]} data   [description]
 * @return {[type]}        [description]
 */
export function lookupPath(path, params, data) {
	let prev;
	_.forEach(path, elem => {
		let current = elem;
		if (_.isArray(elem))
			current = lookupPath(elem, params, data);
		else {
			assert(_.isString(current));
			if (_.startsWith(current, "@")) {
				current = _.tail(current);
				assert(_.has(params, current))
				current = _.get(params, current);
			}
			assert(!_.isUndefined(prev))
		}

		if (_.isUndefined(prev)) {
			prev = current;
		}
		else {
			assert(_.isString(current));
			assert(_.has(prev, current));
			prev = _.get(prev, current);
		}
	});
	return prev;
}
