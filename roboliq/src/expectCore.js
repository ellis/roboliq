import _ from 'lodash';
import assert from 'assert';

class RoboliqError extends Error {
	constructor(context = {}, errors, fnIgnore, stack) {
		super(getErrors(context, errors).join("; "));
		this.name = this.constructor.name;
		this.context = _.cloneDeep(context);
		this.errors = _.isArray(errors) ? errors : [errors];
		if (_.isUndefined(stack)) {
			Error.captureStackTrace(this, fnIgnore || this.constructor.name);
		}
		else {
			this.stack = stack;
		}
	}

	addContext(context) {
		_.mergeWith(this.context, context || {}, (a, b) => {
			if (_.isArray(a) && _.isArray(b)) {
				return a.concat(b);
			}
		});
	}

	getPrefix() { return getPrefix(this.context); }

	toString() { return getErrors(context, errors).join("; "); }
}

function getPrefix(context) {
	const prefixes = [];
	if (_.isPlainObject(context)) {
		if (_.isString(context.stepName)) prefixes.push(`steps.${context.stepName}`);
		if (_.isString(context.paramName)) prefixes.push(`parameter "${context.paramName}"`);
		else if (_.isArray(context.paramName)) prefixes.push(`parameters "${context.paramName.join('`, `')}"`);
		if (_.isString(context.objectName)) prefixes.push(`objects.${context.objectName}`);
	}
	return (prefixes.length > 0) ? prefixes.join(", ")+": " : "";
}

function getErrors(context, errors) {
	const prefix = getPrefix(context);
	return _.map(errors || [], s => prefix+s);
}

function _context(context, fn) {
	try {
		fn();
	}
	catch (e) {
		rethrow(e, context, _context);
	}
}

function rethrow(e, context, fnIgnore = rethrow) {
	if (typeof e === "RoboliqError") {
		e.addContext(context);
		throw e;
	}
	else if (typeof e === "Error") {
		const error = new RoboliqError(context, [e.message], undefined, e.stack);
		throw error;
	}
	else if (typeof e === "string") {
		throw new RoboliqError({errors: [e]}, fnIgnore);
	}
	else {
		throw new RoboliqError(context, [e.toString()], fnIgnore);
	}
}

/*function getContextPrefix(context) {
	const prefixes = [];
	if (!_.isEmpty(context)) {
		if (_.isString(context.paramName)) prefixes.push(`parameter "${context.paramName}"`);
		else if (_.isArray(context.paramName)) prefixes.push(`parameters "${context.paramName.join('`, `')}"`);
		if (_.isString(context.objectName)) prefixes.push(`object "${context.objectName}"`);
	}
	return (prefixes.length > 0) ? prefixes.join(", ")+": " : "";
}*/

/*function handleError(context, e) {
	var prefix = getContextPrefix(context);

	if (!e.trace) {
		if (e.stack)
			console.log(e.stack);
		e.trace = e.stack;
	}

	if (e.errors) {
		e.errors = _.map(e.errors, message => prefix+message);
	}
	else {
		e.name = "ProcessingError";
		e.errors = _.compact([prefix+e.message]);
	}

	//console.log({epath: e.path, cpath: context.path})
	if (!e.path && context.path)
		e.path = context.path;

	throw e;
}*/

function truthy(context, result, message) {
	assert(message, "you must provide a `message` value");
	if (!result) {
		if (_.isFunction(message))
			message = message();
		throw new RoboliqError(context, [message], truthy);
	}
}

function _try(context, fn) {
	try {
		return fn();
	} catch (e) {
		rethrow(e, context, _try);
	}
}

function _throw(context, errors) {
	throw new RoboliqError(context, errors);
}

// TODO: get rid of this function after refactoring parameter processing in roboliqDirectives
function paramsRequired(params, names) {
	assert(_.isPlainObject(params));
	assert(_.isArray(names));
	_.forEach(names, function(name) {
		truthy({paramName: name}, params.hasOwnProperty(name), "missing required value");
	});
}

module.exports = {
	RoboliqError,
	context: _context,
	paramsRequired: paramsRequired,
	rethrow,
	throw: _throw,
	truthy: truthy,
	try: _try
}
