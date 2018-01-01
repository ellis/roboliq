/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017, ETH Zurich, Ellis Whitehead
 * @license GPL-3.0
 */

/**
 * Helper functions for evoware command handlers.
 * @module
 */
const _ = require('lodash');
const assert = require('assert');

/**
 * Create the Evoware token to execute an external command.
 * @param  {string} path - path to command to execute
 * @param  {array} args - array of arguments to pass
 * @param  {boolean} wait - true if evoware should wait for the command to complete execution
 * @param  {string} exitCodeVariableName - optional name of
 * @return {string} a string representing an Evoware 'Execute' token.
 */
function createExecuteLine(path, args, wait, exitCodeVariableName = "") {
	const flag1 = ((wait) ? 2 : 0) + (_.isEmpty(exitCodeVariableName) ? 0 : 4);
	return `Execute("${path} ${args.join(" ")}",${flag1},"${exitCodeVariableName}",2);`;
}

/**
 * Create an Evoware FACTS token.
 * @param  {string} equipment - equipment ID for the FACTS command
 * @param  {string} variableName - variable name for the FACTS command
 * @param  {string} value - optional value of the variable
 * @return {string} a string representing an Evoware 'FACTS' token.
 */
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

/**
 * Create an 'If' token.
 * @param  {string} variable - name of the variable to test
 * @param  {string} test - one of "==", "!=", "<", ">"
 * @param  {string|number} value - value to compare to
 * @param  {string} target - target to jump to: an Evoware "Comment" token
 * @return {string} - line for an "If" token
 */
function createIfLine(variable, test, value, target) {
	const cmps = ["==", "!-", ">", "<"];
	const cmp = cmps.indexOf(test);
	assert(cmp >= 0, `Unknown test: ${test}`);
	return `If("${variable}",${cmp},"${value}","${target}");`
}

/**
 * Create the Evoware 'StartScript' token.
 * @param  {string} path - path to the script to start
 * @return {string} - line for a "StartScript" token
 */
function createStartScriptLine(path) {
	return `StartScript("${path}");`;
}

/**
 * Create the Evoware token to prompt the user.
 * @param  {string} text - text to show the user
 * @param  {numeric} beep - 0: none, 1: beep once, 2: beep three times, 3: beep every 3 seconds
 * @param  {numeric} autoclose - number of second to leave the prompt open before autoclosing it and continuing operation (-1 means no autoclose)
 */
function createUserPromptLine(text, beep = 0, autoclose = -1) {
	return `UserPrompt("${text}",${beep},${autoclose});`;
}

/**
 * Create the Evoware token to prompt the user.
 * @param  {string} text - text to show the user
 * @param  {numeric} beep - 0: none, 1: beep once, 2: beep three times, 3: beep every 3 seconds
 * @param  {numeric} autoclose - number of second to leave the prompt open before autoclosing it and continuing operation (-1 means no autoclose)
 */
function createVariableLine(name, value) {
	const min = "1.000000";
	const max = "10.000000";
	return `Variable(${name},"${value}",0,"",0,${min},${max},${(_.isNumber(value) ? 0 : 1)},2,0,0);`;
}

/**
 * Put double-quotes around a string, if it doesn't already have them.
 * @param  {string} s - any input string
 * @return {string} string with outer double-quotes.
 */
function quote(s) {
	return `"${stripQuotes(s)}"`;
}

/**
 * If a string is surrounded by double-quotes, remove them.
 * @param  {string} s - a string, possible with outer double-quotes.
 * @return {string} string with outer double-quotes removed.
 */
function stripQuotes(s) {
	return (_.startsWith(s, '"') && _.endsWith(s, '"'))
		? s.substring(1, s.length - 1) : s;
}

module.exports = {
  createExecuteLine,
  createFactsLine,
  createIfLine,
  createStartScriptLine,
  createUserPromptLine,
  createVariableLine,
  quote,
  stripQuotes,
};
