var _ = require('lodash');
var assert = require('assert');

function getContextPrefix(context) {
	if (_.isEmpty(context)) return "";
	else if (_.isString(context.paramName)) return "parameter `"+context.paramName+"`: ";
	else if (_.isArray(context.paramName)) return "parameters `"+context.paramName.join('`, `')+"`: ";
	else if (_.isString(context.valueName)) return "value `"+value+"`: ";
	else return "";
}

function handleError(context, e) {
	var prefix = getContextPrefix(context);
	if (!e.trace) {
		console.log(e.stack);
		e.trace = e.stack;
	}
	if (e.errors) {
		e.errors = _.map(e.errors, function(message) {
			return prefix+message;
		});
		throw e;
	}
	else {
		e.name = "ProcessingError";
		e.errors = prefix+e.toString();
		throw e;
	}
}

function truthy(context, result, message) {
	assert(message, "you must provide a `message` value");
	if (!result) {
		var o = _.merge({}, context, {
			name: "ProcessingError",
			errors: [message]
		});
		throw o;
	}
}

function _try(context, fn) {
	try {
		return fn();
	} catch (e) {
		handleError(context, e);
	}
}

module.exports = {
	truthy: truthy,
	try: _try
}
