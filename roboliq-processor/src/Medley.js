import _ from 'lodash';
import assert from 'assert';

export function delMut(data, path) {
	assert(_.isPlainObject(data));
	assert(_.isArray(path) || _.isString(path) || _.isNumber(path), `expected an array or string: ${JSON.stringify(path)}`);

	if (path.length === 0) {
		// do nothing
	}
	else if (path.length === 1) {
		delete data[path[0]];
	}
	else {
		const o = _.get(data, _.initial(path));
		delete o[_.last(path)];
	}
	return data;
}
export function setMut(data, path, value) {
	assert(_.isPlainObject(data));
	assert(_.isArray(path) || _.isString(path) || _.isNumber(path), `expected an array or string: ${JSON.stringify(path)}`);

	path
		= (_.isArray(path)) ? path
		: (_.isString(path)) ? path.split(".")
		: [path];

	// If the path should be deleted
	if (_.isNull(value) || _.isUndefined(value)) {
		delMut(data, path);
	}
	// If path is empty, set the value itself
	else if (_.isEmpty(path)) {
		assert(_.isPlainObject(value));
		_.merge(data, value);
	}
	else {
		const pathPresent = [];
		let dataPresent = data;
		for (let i = 0; i < path.length - 1; i++) {
			const name = path[i];
			//console.log({i, name, dataPresent})
			if (!dataPresent.hasOwnProperty(name)) {
				dataPresent[name] = {};
			}
			dataPresent = dataPresent[name];
		}
		//console.log({dataPresent})
		dataPresent[_.last(path)] = value;
	}

	return data;
}

export function get(data, path, dflt) {
	return _.get(data, path, dflt);
}

module.exports = {
	setMut,
	get
}
