import _ from 'lodash';

/**
 * Create the Evoware token to execute an external command.
 * @param  {string} path - path to command to execute
 * @param  {array} args - array of arguments to pass
 * @param  {boolean} wait - true if evoware should wait for the command to complete execution
 * @return {string} a string representing an Evoware 'Execute' token.
 */
function createExecuteLine(path, args, wait) {
	const flag1 = (wait) ? 2 : 0;
	return `Execute("${path} ${args.join(" ")}",${flag1},"",2);`;
}

function createFactsLine(equipment, variableName, value = "") {
	const l = [
		`"${equipment}"`,
		`"${variableName}"`,
		`"${value}"`,
		`"0"`,
		`""`
	];
	return `FACTS(${l.join(",")});`;
}

function quote(s) {
	return `"${stripQuotes(s)}"`;
}

function stripQuotes(s) {
	return (_.startsWith(s, '"') && _.endsWith(s, '"'))
		? s.substring(1, s.length - 1) : s;
}

module.exports = {
	createExecuteLine,
	createFactsLine,
	quote,
	stripQuotes
};
