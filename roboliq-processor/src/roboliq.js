/**
 * Roboliq's top module with functions for processing protocols.
 * @module roboliq
 */

/**
 * Protocol specification.
 * @typedef {Object} Protocol
 * @property {Object} objects
 * @property {Object} steps
 * @property {Object} effects
 * @property {Array} predicates
 * @property {Object} directiveHandlers
 * @property {Object} objectToPredicateConverters
 * @property {Object} commandHandlers
 * @property {Object} planHandlers
 * @property {Object} files
 * @property {Object} errors
 * @property {Object} warnings
 */

/**
 * Command handler result.
 * @typedef {Object} CommandHandlerResult
 * @property {Array} errors - array of error strings
 * @property {Array} warnings - array of warning strings
 * @property {Object|Array} expansion - an array or map of sub-steps
 * @property {Object} effects - a map of object property effects
 * @property {Object} alternatives - ???
 */

//Error.stackTraceLimit = Infinity;
/**
 * Well contents.
 *
 * Well contents are encoded as an array.
 * The first element always holds the volume in the well.
 * If the array has exactly one element, the volume should be 0l.
 * If the array has exactly two elements, the second element is the name of the substance.
 * If the array has more than two elements, each element after the volume has the same
 * structure as the top array and they represent the mixture originally dispensed in the well.
 *
 *          objects:
 *              plate1:
 *                  contents:
 *                      A01: ["30ul", ["25ul", "water"], ["5ul", "reagent1"]]

 * @typedef {array} WellContents
 */

var _ = require('lodash');
var assert = require('assert');
var fs = require('fs');
import handlebars from 'handlebars';
var jiff = require('jiff');
var jsonfile = require('jsonfile');
import mkdirp from 'mkdirp';
import naturalSort from 'javascript-natural-sort';
var path = require('path');
var yaml = require('yamljs');
import commandHelper from './commandHelper.js';
var expect = require('./expect.js');
var misc = require('./misc.js');
import stripUndefined from './stripUndefined.js';
import * as WellContents from './WellContents.js';
var wellsParser = require('./parsers/wellsParser.js');
import * as Design from './design.js';

const version = "v1";

const nomnom = require('nomnom').options({
	infiles: {
		position: 0,
		help: 'input files, .json or .js',
		list: true
	},
	debug: {
		abbr: 'd',
		flag: true,
		help: 'Print debugging info'
	},
	evoware: {
		help: "Invoke evoware supplier and pass the comma-separated arguements"
	},
	fileData: {
		full: 'file-data',
		list: true,
		help: "Supply filedata on the command line in the form of 'filename:filedata'"
	},
	fileJson: {
		full: 'file-json',
		list: true,
		help: "Supply a JSON file on the command line in the form of 'filename:filedata'"
	},
	loadRoboliqConfig: {
		full: 'load-roboliq-config',
		flag: true,
		default: true
	},
	output: {
		abbr: 'o',
		help: 'specify output filename or "" for none; otherwise the default filename is used',
		metavar: 'FILE'
	},
	outputDir: {
		abbr: 'O',
		full: 'output-dir',
		help: 'specify output directory',
		metavar: 'DIR'
	},
	parentDir: {
		abbr: 'P',
		full: 'parent-dir',
		help: "specify output's parent directory, under which a new subdirectory will be created with the protocol's name",
		metavar: 'DIR'
	},
	print: {
		abbr: 'p',
		flag: true,
		help: 'print output'
	},
	printProtocol: {
		abbr: 'r',
		full: 'print-protocol',
		flag: true,
		help: 'print combined protocol'
	},
	printDesigns: {
		full: 'print-designs',
		flag: true,
		help: 'print design tables'
	},
	progress: {
		flag: true,
		help: 'print progress indicator while processing the protocol'
	},
	quiet: {
		flag: true,
		help: "suppress printing of information, erros, and warning"
	},
	subdir: {
		abbr: 'S',
		full: 'subdir',
		help: "specify an extra subdirectory beneath the parent directory; for use when grouping several protocols together.",
		metavar: 'DIR'
	},
	throw: {
		abbr: 'T',
		flag: true,
		help: 'throw error when errors encountered during processing (in order to get a backtrace)'
	},
	varset: {
		help: "Variable set to load",
		list: true
	},
	version: {
		flag: true,
		help: 'print version and exit',
		callback: function() {
			return "version "+version;
		}
	},
});

const protocolEmpty = {
	objects: {},
	steps: {},
	effects: {},
	predicates: [],
	directiveHandlers: {},
	objectToPredicateConverters: {},
	schemas: {},
	commandHandlers: {},
	planHandlers: {},
	files: {},
	fillIns: {},
	reports: {},
	errors: {},
	warnings: {},
	COMPILER: {},
};

/**
 * Loads the raw content at the given URL.
 * Supported formats are: JSON, YAML, JavaScript, and pre-cached file data.
 *
 * @param  {string} url - URL to load.
 * @param  {object} filecache - map of cached file data, map from URL to data.
 * @return content at URL.
 */
function loadUrlContent(url, filecache) {
	url = path.posix.join(url);
	//if (!path.isAbsolute(url))
	if (!path.isAbsolute(url))
		url = "./" + url;
	//console.log("in cache:", filecache.hasOwnProperty(url))
	const absolutePath = path.resolve(url);
	if (filecache.hasOwnProperty(url))
		return filecache[url];
	else if (path.extname(url) === ".yaml")
		return yaml.load(url);
	else if (path.extname(url) === ".json")
		return jsonfile.readFileSync(url);
	else {
		let relativePath = path.relative(__dirname, absolutePath);
		if (!_.startsWith(relativePath, ".")) {
			relativePath = "./" + relativePath;
		}
		//console.log({url, absolutePath, relativePath, __dirname})
		return require(relativePath);
	}
}

/**
 * Finishing loading/processing an unprocessed protocol: handle imports, directives, and file nodes
 * @param  {Object} a - Previously loaded protocol data
 * @param  {Object} b - The protocol to pre-process
 * @param  {String} [url] - The url of the protocol
 * @return {Object} protocol with
 */
function loadProtocol(a, b, url, filecache) {
	// Require 'roboliq' property
	expect.truthy({}, b.roboliq, "'roboliq' property must be specified with targetted version number for protocol at URL "+url);

	//console.log("loadProtocol:", url);
	//if (url.indexOf("roboliq") > 0)
	//	console.log(JSON.stringify(b))
	// Handle imports
	var imported = _.cloneDeep(protocolEmpty);
	if (b.imports) {
		var urls = _.map(_.flatten([b.imports]), function(imp) {
			// console.log("paths:", path.dirname(url), imp, path.join(path.dirname(url), imp))
			const path1 = path.posix.join(path.dirname(url), imp);
			const path2 = (_.startsWith(path1, "/")) ? path1 : `./${path1}`;
			//console.log({url, absolutePath, relativePath, __dirname})
			return path2;
		});
		var protocols2 = _.map(urls, function(url2) {
			// console.log("url:", url2)
			var protocol2 = loadUrlContent(url2, filecache);
			return loadProtocol(protocolEmpty, protocol2, url2, filecache);
		});
		imported = mergeProtocolList(protocols2);
	}

	if (_.isPlainObject(b.files) && !_.isEmpty(b.files)) {
		_.merge(filecache, b.files)
	}

	/*
	// Add variables to `objects`
	// TODO: Remove this in favor of  (ellis 2016-11-09)
	if (b.variables) {
		_.forEach(b.variables, (value, key) => {
			b.objects[key] = _.merge({}, {type: "Variable"}, value);
		});
	}*/

	// Add parameters to `objects.PARAMS`
	if (b.parameters) {
		// console.log("parameters")
		_.forEach(b.parameters, (param, key) => {
			// console.log("parameter: "+key)
			// If this parameter needs to be 'calculate'd
			const value0 = param.calculate || param.value;
			expect.try({path: key, paramName: "value"}, () => {
				const calculate = _.cloneDeep(value0);
				const data = {
					objects: {PARAMS: _.merge({}, _.get(a, ["objects", "PARAMS"]), _.get(b, ["objects", "PARAMS"]))},
					directiveHandlers: _.merge({}, a.directiveHandlers, b.directiveHandlers)
				};
				// console.log({data})
				const value = expandDirectivesDeep(calculate, data);
				// console.log({value0, calculate, value})
				param.value = value;
			});
			_.set(b.objects, ["PARAMS", key], param.value);
		});
	}

	// Create a clone keeping only valid protocol properties.
	var c = _.cloneDeep(_.pick(b,
		"description",
		"config",
		"parameters",
		"objects",
		"steps",
		"effects",
		"predicates",
		"directiveHandlers",
		"objectToPredicateConverters",
		"schemas",
		"commandHandlers",
		"planHandlers",
		"files",
		"errors",
		"warnings",
		"COMPILER"
	));
	if (_.isUndefined(c.errors)) {
		c.errors = {};
	}

	// Pre-process properties with ?-suffixes and !-suffixes.
	if (!c.fillIns)
		c.fillIns = {};
	preProcessQuestionMarks(c, c.objects, ['objects']);
	preProcessQuestionMarks(c, c.steps, ['steps']);
	// console.log("A: "+JSON.stringify(c.fillIns["objects.plate1.model"]))
	preProcessExclamationMarks(c, c.objects, ['objects']);
	preProcessExclamationMarks(c, c.steps, ['steps']);

	var data = {
		objects: _.merge({}, a.objects, imported.objects, c.objects),
		directiveHandlers: _.defaults({}, b.directiveHandlers, imported.directiveHandlers, a.directiveHandlers)
	};
	// Handle directives for predicates
	var l = [
		'predicates'
	];
	_.forEach(l, function(key) {
		// console.log({key, c: c[key]})
		misc.mutateDeep(c[key], function(x) { return misc.handleDirective(x, data); });
	});

	// Deep mutation for two modifications:
	// 1. Handle file nodes, resolve path relative to current directory, add to "files" key of protocol
	// 2. Substitute parameter values
	misc.mutateDeep(c, function(x) {
		//console.log("x: "+x)
		if (_.isString(x)) {
			// Return filename relative to current directory
			if (_.startsWith(x, "./") || _.startsWith(x, "../")) {
				var filename = "./" + path.posix.join(path.dirname(url), x);
				// If the file hasn't been loaded yet:
				if (!filecache.hasOwnProperty(filename)) {
					try {
						var filedata = fs.readFileSync(filename);
						filecache[filename] = filedata;
						//console.log("filename: "+filename);
						//console.log(filedata);
						//console.log(filedata.toString('utf8'))
					} catch (e) {
						c.errors[url] = [`could not load file (${filename})`, e.toString()];
					}
				}
				return filename;
			}
			// Substitute parameter value
			else if (_.startsWith(x, "$#")) {
				// HACK: modified from misc.handleDirective
				const key = x.substr(2);
				const value = _.get(c, ["parameters", key, "value"]) || _.get(imported, ["parameters", key, "value"]);
				if (_.isUndefined(value)) {
					throw new Error("undefined parameter value: "+x);
				}
				return value;
			}
		}
		return x;
	});

	// Merge in the imports
	var d = mergeProtocols(imported, c);
	//if (url.indexOf("roboliq") > 0)
	//if (c.objects && !c.predicates)
	//	console.log(JSON.stringify(c, null, '\t'));

	// console.log("B: "+JSON.stringify(d.fillIns["objects.plate1.model"]))
	return d;
}

/**
 * Remove properties with '?'-suffix.  If the propery value has a 'value!' property,
 * add a new property to the object without the '?'-suffix and with the given value.
 * Mutates the object.
 * Also add the path to the property to the protocol's `fillIns`
 * @param  {Protocol} protocol
 * @param  {any} obj
 * @param  {array} path
 */
function preProcessQuestionMarks(protocol, obj, path) {
	// console.log("preProcessQuestionMarks")
	if (_.isPlainObject(obj)) {
		const pairs0 = _.toPairs(obj);
		let changed = false;
		const pairs1 = pairs0.map(pair => {
			const [name, value] = pair;
			if (_.endsWith(name, "?")) {
				// console.log("endsWith: "+name)
				changed = true;
				const name1 = name.slice(0, -1);
				if (value.hasOwnProperty('value!')) {
					return [name1, value['value!']];
				}
				else {
					protocol.fillIns[path.concat(name1).join('.')] = value || {};
					// console.log(`protocol.fillIns[${path.concat(name1).join('.')}] = ${JSON.stringify(value)}`)
					return null;
				}
			}
			else {
				preProcessQuestionMarks(protocol, value, path.concat(name));
				return [name, obj[name]];
			}
		});
		if (changed) {
			// Remove all properties
			pairs0.forEach(pair => delete obj[pair[0]]);
			// Add them all back in again, with new names/values
			_.compact(pairs1).forEach(pair => obj[pair[0]] = pair[1]);
		}
	}
	else if (_.isArray(obj)) {
		_.forEach(obj, (value, index) => {
			preProcessQuestionMarks(protocol, value, path.concat(index));
		});
	}
}

/**
 * Any properties that have a "!" suffix are renamed to not have that suffix,
 * overwritting an already existing property if necessary.
 * Mutates the object.
 * @param  {Protocol} protocol
 * @param  {any} obj
 * @param  {array} path
 */
function preProcessExclamationMarks(protocol, obj, path) {
	//console.log(JSON.stringify(obj));
	if (_.isPlainObject(obj)) {
		const pairs0 = _.toPairs(obj);
		let changed = false;
		const obj1 = [];
		for (var i = 0; i < pairs0.length; i++) {
			const [name, value] = pairs0[i];
			if (_.endsWith(name, "!")) {
				changed = true;
				const name1 = name.slice(0, -1);
				obj1[name1] = value;
			}
			// if an object has both ! and non ! properties, the ! property should take precedence
			else if (!obj1.hasOwnProperty(name)) {
				preProcessExclamationMarks(protocol, value, path.concat(name));
				obj1[name] = obj[name];
			}
		}
		if (changed) {
			// Remove all properties
			pairs0.forEach(pair => delete obj[pair[0]]);
			// Add them all back in again, with new names/values
			const pairs1 = _.toPairs(obj1);
			pairs1.forEach(pair => obj[pair[0]] = pair[1]);
		}
	}
	else if (_.isArray(obj)) {
		_.forEach(obj, (value, index) => {
			preProcessExclamationMarks(protocol, value, path.concat(index));
		});
	}
}

/**
 * Merge protocols A & B, returning a new protocol.
 *
 * @param  {Object} a   protocol representing the result of all previous mergeProtocols
 * @param  {Object} b   newly loaded protocol to merge into previous protocols
 * @return {Object}     result of merging protocol B into A.
 */
function mergeProtocols(a, b) {
	//console.log("BEFORE")
	//console.log("a.predicates: "+JSON.stringify(a.predicates));
	//console.log("b.predicates: "+JSON.stringify(b.predicates));

	var c = _.merge({}, _.omit(a, 'predicates'), _.omit(b, 'predicates'));
	//console.log("AFTER")
	//console.log("a.predicates: "+JSON.stringify(a.predicates));
	//console.log("b.predicates: "+JSON.stringify(b.predicates));
	c.predicates = a.predicates.concat(b.predicates || []);
	//console.log("c:", c);
	return c;
}

/**
 * Merge a list of protocols.
 *
 * @param  {array} protocols - list of protocols.
 * @return {Protocol} merged protocol.
 */
function mergeProtocolList(protocols) {
	var protocol = _.cloneDeep(protocolEmpty);
	_.forEach(protocols, function(b) {
		protocol = mergeProtocols(protocol, b);
	});
	return protocol;
}

/**
 * Post-process protocol: flatten predicate list, parse wells strings for Liquid objects.
 *
 * Mutates the passed protocol.
 *
 * @param  {Object} protocol A protocol.
 */
function postProcessProtocol(protocol) {
	// Make sure predicates is a flat list
	protocol.predicates = _.flattenDeep(protocol.predicates);

	// Calculate values for variables
	postProcessProtocol_variables(protocol);

	// For all liquids, if they specify source wells, make sure the source well
	// has a reference to the liquid in its contents (the contents will be added
	// if necessary).
	var liquids = misc.getObjectsOfType(protocol.objects, 'Liquid');
	_.forEach(liquids, function(liquid, name) {
		if (_.isString(liquid.wells)) {
			try {
				liquid.wells = wellsParser.parse(liquid.wells, protocol.objects);
				_.forEach(liquid.wells, function(well) {
					var pair = WellContents.getContentsAndName(well, protocol);
					// If well already has contents:
					if (pair[0]) {
						assert(_.isEqual(_.tail(pair[0]), [name]), "well "+well+" already contains different contents: "+JSON.stringify(pair[0]));
						// Don't need to set contents, leave as is with the given volume.
					}
					else {
						var path = pair[1];
						_.set(protocol.objects, path, ['Infinity l', name]);
					}
				});
			} catch (e) {
				protocol.errors[name+".wells"] = [e.toString(), e.stack];
				//console.log(e.toString());
			}
		}
	});
}

/**
 * For all variables that have a `calculate` property, handle the calculation and put the
 * result in the `value` property.
 *
 * Mutates protocol.
 *
 * @param  {Protocol} protocol - The protocol to inspect.
 */
function postProcessProtocol_variables(protocol) {
	const data = _.clone(protocol);

	_.forEach(protocol.objects, (obj, key) => {
		expect.try({path: key, paramName: "calculate"}, () => {
			// If this is a variable with a 'calculate' property
			if (obj.type === 'Variable' && obj.calculate) {
				const calculate = _.cloneDeep(obj.calculate);
				const value = expandDirectivesDeep(calculate, data);
				obj.value = value;
			}
		});
	});
}

// Recursively expand all directives
function expandDirectivesDeep(x, data) {
	if (_.isPlainObject(x)) {
		for (var key in x) {
			var value1 = x[key];
			if (_.isArray(value1)) {
				x[key] = _.map(value1, function(x2) { return misc.handleDirectiveDeep(x2, data); });
			}
			else {
				x[key] = expandDirectivesDeep(value1, data);
			}
		}
	}
	// Make sure this property exists in order to avoid an exception
	if (!data.hasOwnProperty("accesses")) {
		data.accesses = [];
	}
	return misc.handleDirective(x, data);
}

/**
 * Perorms a schema check, makes sure that all objects are valid.
 *
 * Throws an error if the protocol isn't valid.
 *
 * @param  {Protocol} protocol - The protocol to validate.
 */
function validateProtocol1(protocol, o, path) {
	// console.log({objects: protocol.objects})
	if (_.isUndefined(o)) {
		o = protocol.objects;
		path = [];
	}
	for (const [name, value] of _.toPairs(o)) {
		const path2 = path.concat(name);
		const fullName = path2.join(".");
		const doit = () => {
			//console.log({name, value, fullName})
			if (name !== 'type' && name !== "DATA" && name !== "SCOPE" && name !== "PARAMS") {
				assert(!_.isEmpty(value.type), "Missing `type` property: "+JSON.stringify(value));
				if (value.type === "Namespace") {
					validateProtocol1(protocol, value, path.concat(name));
				}
				else {
					const schema = protocol.schemas[value.type];
					assert(schema, "Unknown type: "+value.type);
					if (schema) {
						const data = {
							objects: protocol.objects,
							predicates: protocol.predicates,
							planHandlers: protocol.planHandlers,
							schemas: protocol.schemas,
							accesses: [],
							files: protocol.files, // or filecache?
							protocol,
							path: [fullName]
						};
						commandHelper.parseParams(value, data, schema);
					}
				}
			}
		}
		expect.context({objectName: fullName}, doit);
	}
}

function run(argv, userProtocol, loadRoboliqProcessorYaml = true) {
	argv = argv || process.argv.slice(2);

	if (loadRoboliqProcessorYaml && fs.existsSync("roboliq-processor.yaml")) {
		const env = yaml.load("roboliq-processor.yaml");
		if (env.preload) {
			argv = env.preload.concat(argv);
		}
		if (env.args) {
			argv = env.args.concat(argv);
		}
	}

	// Validate the command line arguments
	var opts = nomnom.parse(argv);
	if (_.isEmpty(opts.infiles) && !userProtocol) {
		console.log(nomnom.getUsage());
		if (require.main === module) {
			process.exit(0);
		}
	}
	else {
		return runWithOpts(opts, userProtocol);
	}
}

/**
 * Process a roboliq protocol.
 *
 * @param  {array} argv - command line options.
 * @param  {Protocol} [userProtocol] - an optional protocol that can be directly passed into the function rather than supplied via argv; currently this is only for testing purposes.
 * @return {object} Processing results with properties `output` (the final processed protocol) and `protocol` (the result of merging all input protocols).
 */
function runWithOpts(opts, userProtocol) {

	// Configure mathjs to use bignumbers
	require('mathjs').config({
		number: 'BigNumber', // Default type of number
		precision: 64        // Number of significant digits for BigNumbers
	});

	// Try to process the protocol
	var result = undefined;
	try {
		result = _run(opts, userProtocol);
	} catch (e) {
		// If _run throws an exception, we don't get any results,
		// so try to set `error` in the result or at least print
		// messages to the console.
		if (opts.debug || opts.throw) {
			console.log("RUN ERROR:")
			console.log(e);
			console.log(e.message);
			console.log(e.stack);
		}
		if (e.isRoboliqError) {
			result = {};
			const errors = expect.RoboliqError.getErrors(e);
			const path = e.path || "";
			_.set(result, `output.errors[${path}]`, errors);
			//console.log(JSON.stringify(errors))
		}
		else if (!opts.quiet) {
			console.log(JSON.stringify(e));
		}
	}

	// If processing finished without exceptions:
	if (result && result.output) {
		if (!opts.quiet) {
			// Print errors, if any:
			if (!_.isEmpty(result.output.errors)) {
				console.log();
				console.log("Errors:");

				if (_.isPlainObject(result.output.errors)) {
					// Find all sub-steps (properties that start with a digit)
					var keys = _.keys(result.output.errors);
					// Sort them in "natural" order
					keys.sort(naturalSort);

					_.forEach(keys, key => {
						const err = result.output.errors[key];
						console.log(key+": "+err.toString());
					});
				}
				else {
					_.forEach(result.output.errors, function(err, id) {
						if (id)
							console.log(id+": "+err.toString());
						else
							console.log(err.toString());
					});
				}
			}

			// Print warnings, if any:
			if (!_.isEmpty(result.output.warnings)) {
				console.log();
				console.log("Warnings:");
				_.forEach(result.output.warnings, function(err, id) {
					if (id)
						console.log(id+": "+err.toString());
					else
						console.log(err.toString());
				});
			}
		}

		if (opts.debug) {
			console.log();
			console.log("Output:");
		}
		var outputText = JSON.stringify(result.output, null, '\t');
		if (opts.debug || opts.print)
			console.log(outputText);

		// If compilation was suspended, crease a dumpfile for later continuation
		if (_.get(result, ["protocol", "COMPILER", "suspend"])) {
			result.dump = _.clone(result.protocol);
			// Resume where this compilation suspended
			result.dump.COMPILER = {
				resumeStepId: result.protocol.COMPILER.suspendStepId
			};
		}

		// If the output is not suppressed, write the protocol to an output file.
		if (opts.output !== '') {
			var inpath = _.last(opts.infiles);
			var basename = path.basename(inpath, path.extname(inpath));
			var dir
				= (opts.outputDir)
				? opts.outputDir
				: (opts.parentDir)
					? (opts.subdir)
						? path.join(opts.parentDir, opts.subdir, basename)
						: path.join(opts.parentDir, basename)
					: path.dirname(inpath);
			var outpath = opts.output || path.join(dir, basename+".out.json");
			if (!opts.quiet) {
				console.log("output written to: "+outpath);
			}

			// Write output protocol
			mkdirp.sync(path.dirname(outpath));
			fs.writeFileSync(outpath, JSON.stringify(result.output, null, '\t')+"\n");

			// Write extra files if parentDir or outputDir was specified
			// console.log({a: !_.isEmpty(result.output.simulatedOutput), b: opts.parentDir})
			if (opts.parentDir || opts.outputDir) {
				if (!_.isEmpty(result.output.simulatedOutput)) {
					writeSimulatedOutput(opts, dir, result);
				}

				writeHtml(opts, dir, result);
			}

			// Write dump data (2016-11-05 ELLIS: What's this for??)
			if (result.dump) {
				const dumppath = path.join(path.dirname(output), `${result.dump.COMPILER.resumeStepId}.dump.json`);
				if (!opts.quiet) {
					console.log("dump written to: "+dumppath);
				}
				fs.writeFileSync(dumppath, JSON.stringify(result.dump, null, '\t')+"\n");
			}

			// Send through the Evoware compiler
			if (opts.evoware) {
				const evowareArgs = _.clone(opts.evoware.split(","));
				assert(evowareArgs.length >= 3, "at least three arguments must be passed to --evoware options: carrier file, table file, and one or more agent names");
				// Insert
				const evowareRun = require("roboliq-evoware/dist/EvowareMain").run;
				evowareArgs.splice(2, 0, outpath);
				if (!opts.quiet) {
					console.log(`calling evoware: ${evowareArgs.join(" ")}`);
				}
				evowareRun({args: evowareArgs});
			}
		}
	}

	return result;
}

function writeSimulatedOutput(opts, dir, result) {
	const simulatedDir = path.join(dir, "simulated");
	// console.log({simulatedDir})
	mkdirp.sync(simulatedDir);
	_.forEach(result.output.simulatedOutput, (value, filename) => {
		const simulatedFile = path.join(simulatedDir, filename);
		// console.log({filename, simulatedFile})
		if (!opts.quiet) {
			console.log("saving simulated output: "+simulatedFile);
		}
		const ext = path.extname(simulatedFile);
		if (ext === ".json") {
			fs.writeFileSync(simulatedFile, JSON.stringify(value, null, "\t")+"\n");
		}
		else if (ext === ".jsonl") {
			const contents = value.map(x => JSON.stringify(x)).join("\n") + "\n";
			fs.writeFileSync(simulatedFile, contents);
		}
		else {
			fs.writeFileSync(simulatedFile, value);
		}
	});
}

function writeHtml(opts, dir, result) {
	const source = fs.readFileSync(__dirname + "/html/index.html", "utf8");
	const template = handlebars.compile(source);
  const html = template(result.output);

	const filename = path.join(dir, "index.html");
	if (!opts.quiet) {
		console.log("saving HTML output: "+filename);
	}
	fs.writeFileSync(filename, html);
}


/**
 * Process the protocol(s) given by the command line options and an optional
 * userProtocol passed in separately to the API (currently this is just for testing).
 *
 * @param  {object} opts - command line arguments as processed by nomnom.
 * @param  {Protocol} [userProtocol] - an optional protocol that can be directly passed into the function rather than supplied via argv; currently this is only for testing purposes.
 * @return {object} Processing results with properties `output` (the final processed protocol) and `protocol` (same as output, but without tables).
 */
function _run(opts, userProtocol) {

	if (opts.debug) {
		console.log("opts:", opts);
	}

	const filecache = {};

	_.forEach(opts.fileData, function(s) {
		var pair = splitInlineFile(s);
		var data = pair[1];
		filecache[pair[0]] = data;
	});
	_.forEach(opts.fileJson, function(s) {
		var pair = splitInlineFile(s);
		var data = JSON.parse(pair[1]);
		//console.log("fileJson:", s, data);
		filecache[pair[0]] = data;
	});

	// Add config/roboliq.js to URLs by default.
	const urls = _.uniq(_.compact(
		_.compact([
			(opts.loadRoboliqConfig) ? __dirname+'/config/roboliq.js' : undefined
		]).concat(opts.infiles)
	));
	if (opts.debug) {
		console.log("urls:", urls);
	}

	// Load all the protocols in unprocessed form
	var urlToProtocol_l = _.map(urls, function(url) {
		return [url, loadUrlContent(url, filecache)];
	});
	// Append the optional user protocol to the list
	// (this lets unit tests pass in JSON protocols rather than loading them from files).
	if (userProtocol)
		urlToProtocol_l.push([undefined, userProtocol]);

	// Load varsets
	// console.log({opts})
	_.forEach(opts.varset, varsetString => {
		// console.log({varsetString})
		let url;
		let varset;
		if (_.isPlainObject(varsetString)) { // This is strange, apparently nomnom automatically converted the string to an object!
			varset = varsetString;
		}
		else if (_.startsWith(varsetString, "{")) {
			varset = JSON.parse(varsetString);
		}
		else {
			url = varsetString;
			varset = loadUrlContent(url, filecache);
		}
		const varsetProtocol = {
			roboliq: version,
			objects: {
				SCOPE: varset
			}
		};
		// console.log({varsetProtocol})
		urlToProtocol_l.push([url, varsetProtocol]);
	});

	// Reduce the list of URLs by merging or patching them together, starting
	// with the empty protocol.
	var protocol = _.reduce(
		urlToProtocol_l,
		(protocol, [url, raw]) => {
			if (_.isArray(raw)) {
				return jiff.patch(raw, protocol);
			}
			else {
				var b = loadProtocol(protocol, raw, url || "", filecache);
				return mergeProtocols(protocol, b);
			}
		},
		protocolEmpty
	);
	/*if (opts.debug) {
		console.log(protocol);
	}*/

	// Add command line options
	//console.log({opts})
	protocol.COMPILER.roboliqOpts = opts;
	protocol.COMPILER.filecache = filecache;

	try {
		postProcessProtocol(protocol);
		//console.log("A")
		validateProtocol1(protocol);
	} catch(e) {
		if (opts.debug || opts.throw) {
			console.log("Error type = "+(typeof e).toString());
		}
		if (e.isRoboliqError) {
			const prefix = expect.getPrefix(e.context);
			protocol.errors["_"] = _.map(e.errors, s => prefix+s);
		}
		else if (_.has(e, "errors")) {
			protocol.errors["_"] = e.errors;
		}
		else {
			protocol.errors["_"] = _.compact([JSON.stringify(e), e.stack]);
		}
		if (opts.throw) {
			if (_.isPlainObject(e))
				console.log("e:\n"+JSON.stringify(e));
			expect.rethrow(e, {stepName: id});
		}
		return {protocol: protocol, output: protocol};
	}
	//console.log("B")

	var objectToPredicateConverters = protocol.objectToPredicateConverters;

	// If initial processing didn't result in any errors,
	//  expand steps and get final objects.
	const objectsFinal = (_.isEmpty(protocol.errors))
		? expandProtocol(opts, protocol)
		: protocol.objects;

	if (opts.debug || opts.printProtocol) {
		console.log();
		console.log("Protocol:");
		console.log(JSON.stringify(protocol, null, '\t'));
		/*console.log();
		console.log("Steps:")
		console.log(JSON.stringify(protocol.steps, null, '\t'));
		console.log();
		console.log("Effects:")
		console.log(JSON.stringify(effects, null, '\t'));
		*/
	}

	if (opts.debug || opts.printDesigns) {
		const designs = misc.getObjectsOfType(protocol.objects, "Design");
		_.forEach(designs, (design, name) => {
			console.log();
			console.log(`Design "${name}":`);
			// console.log(JSON.stringify(design, null, '\t'))
			design = misc.handleDirectiveDeep(design, protocol);
			design = commandHelper.substituteDeep(design, protocol, {}, []);
			const rows = Design.flattenDesign(design);
			Design.printRows(rows);
		});
	}

	// If there were errors,
	if (!_.isEmpty(protocol.errors)) {
		//return {protocol: protocol, output: _.pick(protocol, 'errors', 'warnings')};
		console.log("WITH ERRORS")
		return {protocol: protocol, output: protocol};
	}
	// Otherwise create tables
	else {
		const output = _.merge(
			{roboliq: version},
			_.pick(protocol, "description", "config", "parameters", "objects", "schemas", "steps", "effects", "reports", "simulatedOutput", "warnings", "errors", "fillIns")
		);
		// Handle protocol.COMPILER
		if (!_.isEmpty(protocol.COMPILER)) {
			output.COMPILER = _.pick(protocol.COMPILER, "resumeStepId", "suspendStepId");
		}
		// console.log("SIMULATED OUTPUT")
		// console.log(JSON.stringify(protocol.simulatedOutput))
		// process.exit(-1);

		const tables = {
			labware: [],
			sourceWells: [],
			wellContentsFinal: []
		};
		// Construct labware table
		const labwares = misc.getObjectsOfType(objectsFinal, ['Plate', 'Tube']);
		_.forEach(labwares, function(labware, name) {
			tables.labware.push(_.merge({}, {
				labware: name,
				type: labware.type,
				model: labware.model,
				locationInitial: expect.objectsValue({}, name+'.location', protocol.objects),
				locationFinal: labware.location
			}));
		});
		// Construct sourceWells table
		var tabulateWELLSSource = function(o, id) {
			//console.log("tabulateWELLSSource", o, id)
			if (o.isSource) {
				/* Example:
				- source: water
		          well: plate1(A01)
		          volume: 0ul
		          volumeRemoved: 60ul
				*/
				var wellName = (id.indexOf(".contents.") >= 0)
					? id.replace('.contents.', '(')+')'
					: id.replace('.contents', '()');
				var contents = expect.objectsValue({}, id, objectsFinal);
				var source = (contents.length == 2 && _.isString(contents[1]))
					? contents[1]
					: wellName;
				var volumeInitial = misc.findObjectsValue(id, protocol.objects, null, ["0ul"])[0];
				var volumeFinal = contents[0];
				tables.sourceWells.push({source: source, well: wellName, volumeInitial: volumeInitial, volumeFinal: volumeFinal, volumeRemoved: o.volumeRemoved || "0"});
			}
		};
		// For each well in object.__WELLS__, add to the appropriate table
		var tabulateWELLS = function(objects, prefix) {
			//console.log("tabulateWELLS", prefix)
			_.forEach(objects, function(x, field) {
				if (field === 'isSource') {
					tabulateWELLSSource(objects, prefix.join('.'));
				}
				else if (_.isPlainObject(x)) {
					tabulateWELLS(x, prefix.concat([field]));
				}
			});
		};
		tabulateWELLS(objectsFinal['__WELLS__'] || {}, []);
		// Construct wellContentsFinal table
		var tabulateWellContents = function(contents, labwareName, wellName) {
			//console.log("tabulateWellContents:", JSON.stringify(contents), labwareName, wellName);
			if (_.isArray(contents)) {
				var map = WellContents.flattenContents(contents);
				var wellName2 = (wellName) ? labwareName+"("+wellName+")" : labwareName;
				tables.wellContentsFinal.push(_.merge({well: wellName2}, map));
			}
			else if (_.isPlainObject(contents)) {
				_.forEach(contents, function(contents2, name2) {
					var wellName2 = _.compact([wellName, name2]).join('.');
					tabulateWellContents(contents2, labwareName, wellName2);
				});
			}
		};
		_.forEach(labwares, function(labware, name) {
			if (labware.contents) {
				tabulateWellContents(labware.contents, name);
			}
		});

		// Get tables for all designs
		const designs = misc.getObjectsOfType(protocol.objects, "Design");
		const designTables = _.mapValues(designs, design => {
			design = misc.handleDirectiveDeep(design, protocol);
			design = commandHelper.substituteDeep(design, protocol, {}, []);
			return Design.flattenDesign(design);
		});
		if (!_.isEmpty(designTables))
			tables.designs = designTables;

		output.tables = tables;

		return {protocol: protocol, output: output};
	}
}

// Handle fileData and fileJson options, where file data is passed on the command line.
function splitInlineFile(s) {
	var i = s.indexOf(':');
	assert(i > 0);
	var name = "./" + path.posix.join(s.substr(0, i));
	var data = s.substr(i + 1);
	return [name, data];
}

/**
 * This function recurively iterates through all objects, and for each
 * object whose type has an entry in protocol.objectToPredicateConverters,
 * it generates the logical predicates and appends them to stateList.
 *
 * Mutates stateList.
 *
 * @param  {string} name - name of current object
 * @param  {object} o - current object
 * @param  {array} stateList - array of logical predicates
 */
function createStateItems(objectToPredicateConverters, o, name = "", stateList = []) {
	//console.log("name: "+name);
	if (o.hasOwnProperty("type")) {
		//console.log("type: "+o.type);
		var type = o['type'];
		if (objectToPredicateConverters.hasOwnProperty(type)) {
			var result = objectToPredicateConverters[type](name, o);
			if (result.value) {
				stateList.push.apply(stateList, result.value);
			}
		}
	}

	var prefix = _.isEmpty(name) ? "" : name + ".";
	_.forEach(o, function(value, name2) {
		//console.log(name2, value);
		if (_.isPlainObject(value)) {
			createStateItems(objectToPredicateConverters, value, prefix + name2, stateList);
		}
	});

	return stateList;
}

/**
 * Expand the protocol's steps.
 * This means that commands are passed to command handlers to possibly
 * be expanded to lower-level sub-commands.
 *
 * Mutates protocol.
 *
 * @param  {Protocol} The protocol.
 * @return {object} The final state of objects.
 */
function expandProtocol(opts, protocol) {
	var objects0 = _.cloneDeep(protocol.objects);
	_.merge(protocol, {effects: {}, cache: {}, warnings: {}, errors: {}});
	// If we should resume expansion at a particular step:
	delete protocol.COMPILER.suspend;
	// console.log({COMPILER: protocol.COMPILER})
	if (protocol.COMPILER.resumeStepId) {
		protocol.COMPILER.skipTo = protocol.COMPILER.resumeStepId; // HACKy...
	}
	expandStep(opts, protocol, [], protocol.steps, objects0);
	return objects0;
}

/**
 * Expand the given step by passing a command to its command handler
 * and recursively expanding sub-steps.
 *
 * Mutates protocol.  However, since protocol.objects should still hold the
 * *initial* objects after processing, rather than mutating protocol.objects
 * during processing, a separate `objects` variable is mutated, which
 * starts out as a deep copy of protocol.objects.
 *
 * @param  {Protocol} protocol - the protocol
 * @param  {array} prefix - array of string representing the current step ID (initially []).
 * @param  {object} step - the current step (initially protocol.steps).
 * @param  {object} objects - a mutable copy of the protocol's objects.
 */
function expandStep(opts, protocol, prefix, step, objects, SCOPE = {}, DATA = []) {
	// If protocol.COMPILER.suspend is set, compiling should be suspended and continued later
	if (protocol.COMPILER.suspend) {
		return;
	}

	//console.log("expandStep: "+prefix+JSON.stringify(step))
	var commandHandlers = protocol.commandHandlers;
	var id = prefix.join('.');
	// console.log({id, RESUME: protocol.COMPILER})
	if (opts.progress) {
		console.log(_.compact(["step "+id, step.command, step.description]).join(": "));
	}

	const accesses = [];
	const data0 = commandHelper.createData(protocol, objects, SCOPE, DATA, prefix, protocol.COMPILER.filecache);
	const {DATAs, SCOPEs, foreach} = commandHelper.updateSCOPEDATA(step, data0, SCOPE, DATA);

	// Check for command and its handler
	const commandName = step.command;
	const handler = (commandName) ? commandHandlers[commandName] : undefined;
	if (commandName && !handler) {
		protocol.warnings[id] = ["unknown command: "+step.command];
		return;
	}

	const step0 = _.omit(step, "data");
	for (let groupIndex = 0; groupIndex < DATAs.length; groupIndex++) {
		const prefix2 = prefix.concat([groupIndex + 1]);
		const DATA = DATAs[groupIndex];
		const SCOPE = SCOPEs[groupIndex];
		const data = commandHelper.createData(protocol, objects, SCOPE, DATA, prefix2, protocol.COMPILER.filecache);
		const SCOPE2 = data.objects.SCOPE;
		const params = misc.handleDirectiveDeep(step0, data);

		if (foreach) {
			const groupKey = (groupIndex+1).toString();
			// For each DATA set, parse the command's parameters in order substitute in DATA and SCOPE variables,
			// then expand for each DATA in DATAs.
			let step2;
			if (commandName) {
				if (protocol.schemas[commandName]) {
					//if (!_.isEmpty(data.objects.SCOPE)) { console.log({SCOPE: data.objects.SCOPE})}
					const schema = protocol.schemas[commandName];
					//console.log("params: "+JSON.stringify(params))
					const parsed = commandHelper.parseParams(params, data, schema);
					//console.log("parsed:"+JSON.stringify(parsed, null, '\t'))
					const params2 = _.merge({}, params, parsed.value, parsed.objectName);
					step2 = params2;
				}
				else {
					step2 = params;
				}
			}
			else if (params.steps) {
				step2 = params.steps;
			}

			if (step2) {
				expandStep(opts, protocol, prefix2, step2, objects, SCOPE2, DATA);
				/*// if there are substeps, then don't include the parameters again
				const substepKeys = commandHelper.getStepKeys(step2);
				if (!_.isEmpty(substepKeys)) {
				}
				if (step2.)*/
				step[groupKey] = step2;
			}
		}
		else {
			// If we're skipping to a specific step
			// console.log({COMPILER: protocol.COMPILER})
			if (protocol.COMPILER.skipTo) {
				// If the step has been reached:
				if (protocol.COMPILER.skipTo === id) {
					protocol.COMPILER.skipTo = undefined;
					assert(commandName === "system.runtimeLoadVariables", "Roboliq can only resume compiling at a `system.runtimeLoadVariables` command");
				}
				else {
					expandSubsteps(opts, protocol, prefix, step, objects, SCOPE2, DATA);
				}
			}
			else if (commandName === "system.runtimeLoadVariables") {
				protocol.COMPILER.suspend = true;
				protocol.COMPILER.suspendStepId = id;
			}
			else {
				if (commandName) {
					expandCommand(protocol, prefix, step, objects, SCOPE2, params, commandName, handler, DATA, id);
				}
				expandSubsteps(opts, protocol, prefix, step, objects, SCOPE2, DATA);
			}
		}
	}
}

function expandSubsteps(opts, protocol, prefix, step, objects, SCOPE, DATA) {
	// Find all sub-steps (properties that start with a digit)
	const keys = commandHelper.getStepKeys(step);
	// Try to expand the substeps
	for (const key of keys) {
		expandStep(opts, protocol, prefix.concat(key), step[key], objects, SCOPE, DATA);
	}
}

function expandCommand(protocol, prefix, step, objects, SCOPE, params, commandName, handler, DATA, id) {
	// Take the initial predicates and append predicates for the current state
	// REFACTOR: this might be a time-consuming process, which could perhaps be
	// sped up by using Immutablejs and checking which objects have changed
	// rather than regenerating predicates for all objects.
	const predicates = protocol.predicates.concat(createStateItems(protocol.objectToPredicateConverters, objects));
	const opts = protocol.COMPILER.roboliqOpts || {};
	let result = {};
	const objects2 = _.merge({}, objects, {SCOPE});
	if (!_.isUndefined(DATA))
		objects2.DATA = DATA;
	const data = {
		objects: objects2,
		predicates,
		planHandlers: protocol.planHandlers,
		schemas: protocol.schemas,
		accesses: [],
		files: protocol.COMPILER.filecache,
		protocol,
		path: prefix,
		simulatedOutput: protocol.simulatedOutput || {}
	};
	try {
		//if (!_.isEmpty(data.objects.SCOPE)) { console.log({SCOPE: data.objects.SCOPE})}
		// If a schema is given for the command, parse its parameters
		const schema = protocol.schemas[commandName];
		// console.log("params: "+JSON.stringify(params))
		const parsed = (schema)
			? commandHelper.parseParams(params, data, schema)
			: undefined;
		// If the handler has an input specification, parse it
		if (_.isPlainObject(handler.inputSpec)) {
			const input = commandHelper.parseInputSpec(handler.inputSpec, parsed, data);
			parsed.input = input;
		}
		// Try to run the command handler
		//console.log("A")
		//console.log(handler)
		result = handler(params, parsed, data) || {};
		result = stripUndefined(result);
		//console.log("B")
		//console.log("result: "+JSON.stringify(result))
	} catch (e) {
		// console.log("Some Error:");
		// console.log(JSON.stringify(e, null, "\t"))
		if (opts.debug || opts.throw) {
			console.log("Error type = "+(typeof e).toString());
		}
		if (e.isRoboliqError) {
			// console.log("RoboliqError:");
			// console.log(JSON.stringify(e, null, "\t"))
			const prefix = expect.getPrefix(e.context);
			result = {errors: _.map(e.errors, s => prefix+s)};
		}
		else if (_.has(e, "errors")) {
			result = {errors: e.errors};
		}
		else {
			result = {errors: _.compact([JSON.stringify(e), e.stack])};
		}
		console.log(`ERROR: `+result.errors.join("\n"));
		if (opts.throw) {
			if (_.isPlainObject(e))
				console.log("e:\n"+JSON.stringify(e));
			expect.rethrow(e, {stepName: id});
		}
	}
	// If debugging, store the result verbatim
	if (protocol.COMPILER.roboliqOpts.debug)
		protocol.cache[id] = result;

	// If there were errors:
	if (!_.isEmpty(result.errors)) {
		protocol.errors[id] = result.errors;
		// Abort expansion of protocol
		return false;
	}
	// If there were warnings
	if (!_.isEmpty(result.warnings)) {
		protocol.warnings[id] = result.warnings;
	}
	// If the command was expanded, merge the expansion into the protocol as substeps:
	if (!_.isEmpty(result.expansion)) {
		// If an array was returned rather than an object, put it in the proper form
		//console.log({expansion: result.expansion, stepified: commandHelper.stepify(result.expansion)})
		result.expansion = commandHelper.stepify(result.expansion);
		result.expansion = commandHelper.substituteDeep(result.expansion, data, data.objects.SCOPE, data.objects.DATA);
		//console.log({expansion: result.expansion})
		_.merge(step, result.expansion);
	}
	// If the command has effects
	if (!_.isEmpty(result.effects)) {
		//console.log(result.effects);
		// Add effects to protocol's record of effects
		protocol.effects[id] = result.effects;
		//console.log("mixPlate.contents.C01 #0: "+_.get(objects, "mixPlate.contents.C01"));
		// Update object states
		_.forEach(result.effects, (value, key) => _.set(objects, key, value));
		//console.log("mixPlate.contents.C01 #1: "+_.get(objects, "mixPlate.contents.C01"));
	}
	// If the command has reports
	if (!_.isEmpty(result.reports)) {
		_.set(protocol, ["reports", id], result.reports);
	}
	// If the command has simulated output
	if (!_.isEmpty(result.simulatedOutput)) {
		_.forEach(result.simulatedOutput, (value, key) => {
			_.set(protocol, ["simulatedOutput", key], value);
		});
	}
}

module.exports = {
	run,
	runWithOpts,
}

if (require.main === module) {
	run();
}
