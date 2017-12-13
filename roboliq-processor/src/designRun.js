/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017, ETH Zurich, Ellis Whitehead
 * @license GPL-3.0
 */

import _ from 'lodash';
import jsonfile from 'jsonfile';
import path from 'path';
import yaml from 'yamljs';
import commandHelper from './commandHelper.js';
import * as Design from './design.js';
import misc from './misc.js';

const commander = require('commander')
	.version("1.0")
	.option("-d, --debug", "enable debugging output")
	.option("-t, --type [type]", "specify input type (yaml, json)")
	.option("-p, --path [path]", "path to design within an YAML or JSON object")
	.option("-T, --outputType [type]", "specify the output type (text, tab, jsonl, markdown)")
	.arguments("<input>")
	.description(
		"Arguments:\n"+
		"    file   path input file, or - to read from stdin\n"
	);

function handleDesign(design, opts) {
	let protocol;
	if (opts.path) {
		protocol = design;
		design = _.get(design, opts.path);
	}
	if (protocol) {
		const parameters = _.get(protocol, "parameters", {});
		const data = {protocol, objects: {PARAMS: _.mapValues(parameters, value => value.value)}};
		const SCOPE = {};
		const DATA = [];
		design = misc.handleDirectiveDeep(design, data);
		design = commandHelper.substituteDeep(design, data, SCOPE, DATA);
		// console.log(JSON.stringify(design, null, '\t'))
	}
	const table = Design.flattenDesign(design);
	if (opts.outputType === "tab") {
		Design.printTAB(table);
	}
	else if (opts.outputType === "jsonl") {
		_.forEach(table, row => console.log(JSON.stringify(row)));
	}
	else if (opts.outputType === "markdown") {
		Design.printMarkdown(table);
	}
	else {
		Design.printRows(table);
	}
}

function run(argv) {
	require('mathjs').config({
		number: 'BigNumber', // Default type of number
		precision: 64		// Number of significant digits for BigNumbers
	});

	var opts = commander.parse(argv);
	if (opts.args.length === 0 || opts.rawArgs.indexOf("--help") >= 0 || opts.rawArgs.indexOf("-h") >= 0) {
		opts.outputHelp();
		return;
	}

	if (opts.debug) {
		console.log(opts);
	}

	const filename = opts.args[0];
	const isYaml = opts.type === "yaml" || (!opts.type && path.extname(filename) === ".yaml");
	const isJson = opts.type === "json" || (!opts.type && path.extname(filename) === ".json");
	if (filename === '-') {
		const stdin = process.stdin;
		const inputChunks = [];

		stdin.resume();
		stdin.setEncoding('utf8');
		stdin.on('data', function(chunk) {
			inputChunks.push(chunk);
		});

		stdin.on('end', function() {
			const inputJSON = inputChunks.join();
			const parsed = JSON.parse(inputJSON);
			handleDesign(design, opts)
		});
	}
	else if (isYaml) {
		const design = yaml.load(filename);
		handleDesign(design, opts);
	}
}

module.exports = {
	run
};
