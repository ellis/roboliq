import _ from 'lodash';
import jsonfile from 'jsonfile';
import path from 'path';
import yaml from 'yamljs';
import * as Design from './design2.js';

const commander = require('commander')
	.version("1.0")
	.option("-d, --debug", "enable debugging output")
	.option("-t, --type [type]", "specify input type (yaml, json)")
	.option("-p, --path [path]", "path to design within an YAML or JSON object")
	.arguments("<input>")
	.description(
		"Arguments:\n"+
		"    file   path input file, or - to read from stdin\n"
	);

function handleDesign(design, opts) {
	console.log({opts})
	if (opts.path) {
		console.log({path, data: _.get(design, opts.path)})
		design = _.get(design, opts.path);
	}
	const table = Design.flattenDesign(design);
	Design.printRows(table);
}

function run(argv) {
	require('mathjs').config({
		number: 'bignumber', // Default type of number
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
