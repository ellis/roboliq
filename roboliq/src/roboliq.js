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
var jiff = require('jiff');
var jsonfile = require('jsonfile');
import naturalSort from 'javascript-natural-sort';
var path = require('path');
var yaml = require('yamljs');
import commandHelper from './commandHelper.js';
var expect = require('./expect.js');
var misc = require('./misc.js');
import * as WellContents from './WellContents.js';
var wellsParser = require('./parsers/wellsParser.js');

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
	ourlab: {
		full: 'ourlab',
		flag: true,
		default: true,
		help: "automatically load config/ourlab.js"
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
	progress: {
		flag: true,
		help: 'print progress indicator while processing the protocol'
	},
	throw: {
		abbr: 'T',
		flag: true,
		help: 'throw error when errors encountered during processing (in order to get a backtrace)'
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
	errors: {},
	warnings: {},
};

// REFACTOR: If nodejs version >= 0.12, then use path.isAbsolute instead
function isAbsolute(p) {
	return path.normalize(p + '/') === path.normalize(path.resolve(p) + '/');
}

/**
 * Loads the raw content at the given URL.
 * Supported formats are: JSON, YAML, JavaScript, and pre-cached file data.
 *
 * @param  {string} url - URL to load.
 * @param  {object} filecache - map of cached file data, map from URL to data.
 * @return content at URL.
 */
function loadUrlContent(url, filecache) {
	url = path.join(url);
	//if (!path.isAbsolute(url))
	if (!isAbsolute(url))
		url = "./" + url;
	//console.log("in cache:", filecache.hasOwnProperty(url))
	//console.log("absolute:", path.resolve(url))
	if (filecache.hasOwnProperty(url))
		return filecache[url];
	else if (path.extname(url) === ".yaml")
		return yaml.load(url);
	else if (path.extname(url) === ".json")
		return jsonfile.readFileSync(url);
	else
		return require(url);
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
			//console.log("paths:", path.dirname(url), imp, path.join(path.dirname(url), imp))
			return "./" + path.join(path.dirname(url), imp);
		});
		var protocols2 = _.map(urls, function(url2) {
			//console.log("url:", url2)
			var protocol2 = loadUrlContent(url2, filecache);
			return loadProtocol(protocolEmpty, protocol2, url2, filecache);
		});
		imported = mergeProtocolList(protocols2);
	}

	if (_.isPlainObject(b.files) && !_.isEmpty(b.files)) {
		_.merge(filecache, b.files)
	}

	// Create a clone keeping only valid protocol properties.
	var c = _.cloneDeep(_.pick(b,
		'objects',
		'steps',
		'effects',
		'predicates',
		'directiveHandlers',
		'objectToPredicateConverters',
		'schemas',
		'commandHandlers',
		'planHandlers',
		'files',
		'errors',
		'warnings'
	));

	// Pre-process properties with ?-suffixes and !-suffixes.
	if (!c.fillIns)
		c.fillIns = {};
	preProcessQuestionMarks(c, c.objects, ['objects']);
	preProcessQuestionMarks(c, c.steps, ['steps']);
	preProcessExclamationMarks(c, c.objects, ['objects']);
	preProcessExclamationMarks(c, c.steps, ['steps']);

	var data = {
		objects: _.merge({}, a.objects, imported.objects, c.objects),
		directiveHandlers: _.merge({}, a.directiveHandlers, imported.directiveHandlers, b.directiveHandlers)
	};
	// Handle directives for predicates
	var l = [
		'predicates',
	];
	_.forEach(l, function(key) {
		misc.mutateDeep(c[key], function(x) { return misc.handleDirective(x, data); });
	});

	// Handle file nodes, resolve path relative to current directory, add to "files" key of protocol
	misc.mutateDeep(c, function(x) {
		//console.log("x: "+x)
		// Return filename relative to current directory
		if (_.isString(x) && _.startsWith(x, ".")) {
			var filename = "./" + path.join(path.dirname(url), x);
			// If the file hasn't been loaded yet:
			if (!filecache.hasOwnProperty(filename)) {
				var filedata = fs.readFileSync(filename);
				filecache[filename] = filedata;
				//console.log("filename: "+filename);
				//console.log(filedata);
				//console.log(filedata.toString('utf8'))
			}
			return filename;
		}
		else {
			return x;
		}
	});

	// Merge in the imports
	var d = mergeProtocols(imported, c);
	//if (url.indexOf("roboliq") > 0)
	//if (c.objects && !c.predicates)
	//	console.log(JSON.stringify(c, null, '\t'));

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
	if (_.isPlainObject(obj)) {
		const pairs0 = _.toPairs(obj);
		let changed = false;
		const pairs1 = pairs0.map(pair => {
			const [name, value] = pair;
			if (_.endsWith(name, "?")) {
				changed = true;
				const name1 = name.slice(0, -1);
				if (value.hasOwnProperty('value!')) {
					return [name1, value['value!']];
				}
				else {
					protocol.fillIns[path.concat(name1).join('.')] = value || {};
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
	// Recursively expand all directives
	function expandDirectives(x) {
		if (_.isPlainObject(x)) {
			for (var key in x) {
				var value1 = x[key];
				if (_.isArray(value1)) {
					x[key] = _.map(value1, function(x2) { return misc.handleDirectiveDeep(x2, protocol); });
				}
				else {
					x[key] = expandDirectives(value1);
				}
			}
		}
		data.accesses = [];
		return misc.handleDirective(x, data);
	}

	_.forEach(protocol.objects, (obj, key) => {
		expect.try({path: key, paramName: "calculate"}, () => {
			// If this is a variable with a 'calculate' property
			if (obj.type === 'Variable' && obj.calculate) {
				const calculate = _.cloneDeep(obj.calculate);
				const value = expandDirectives(calculate);
				obj.value = value;
			}
		});
	});
}

/**
 * Perorms a schema check, makes sure that all objects are valid.
 *
 * Throws an error if the protocol isn't valid.
 *
 * @param  {Protocol} protocol - The protocol to validate.
 */
function validateProtocol1(protocol, o, path) {
	if (_.isUndefined(o)) {
		o = protocol.objects;
		path = [];
	}
	for (const [name, value] of _.toPairs(o)) {
		const fullName = path.concat(name).join(".");
		//console.log({name, value})
		if (name !== 'type') {
			expect.truthy({objectName: fullName}, !_.isEmpty(value.type), "Missing `type` property.")
			if (value.type === "Namespace") {
				validateProtocol1(protocol, value, path.concat(name));
			}
			else {
				const schema = protocol.schemas[value.type];
				expect.truthy({objectName: fullName}, schema, "Unknown type: "+value.type);
				if (schema) {
					commandHelper.parseParams(value, protocol, schema);
				}
			}
		}
	}
}

/**
 * Process a roboliq protocol.
 *
 * @param  {array} argv - command line options.
 * @param  {Protocol} [userProtocol] - an optional protocol that can be directly passed into the function rather than supplied via argv; currently this is only for testing purposes.
 * @return {object} Processing results with properties `output` (the final processed protocol) and `protocol` (the result of merging all input protocols).
 */
function run(argv, userProtocol) {
	// Validate the command line arguments
	var opts = nomnom.parse(argv);
	if (_.isEmpty(opts.infiles) && !userProtocol) {
		console.log(nomnom.getUsage());
		process.exit(0);
	}

	// Try to process the protocol
	var result = undefined;
	try {
		result = _run(opts, userProtocol);
	} catch (e) {
		// If _run throws an exception, we don't get any results,
		// so try to set `error` in the result or at least print
		// messages to the console.
		console.log(JSON.stringify(e));
		console.log(e.message);
		console.log(e.stack);
		if (e.hasOwnProperty("errors")) {
			const path = e.path || "''";
			result = {}
			_.set(result, `output.errors[${path}]`, e.errors);
		}
		else {
			console.log(JSON.stringify(e));
		}
	}

	// If processing finished without exceptions:
	if (result && result.output) {
		// Print errors, if any:
		if (!_.isEmpty(result.output.errors)) {
			console.log();
			console.log("Errors:");
			_.forEach(result.output.errors, function(err, id) {
				if (id)
					console.log(id+": "+err.toString());
				else
					console.log(err.toString());
			});
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

		if (opts.debug) {
			console.log();
			console.log("Output:");
		}
		var outputText = JSON.stringify(result.output, null, '\t');
		if (opts.debug || opts.print)
			console.log(outputText);

		// If the output is not suppressed, write the protocol to an output file.
		if (opts.output !== '') {
			var inpath = _.last(opts.infiles);
			var dir = opts.outputDir || path.dirname(inpath);
			var outpath = opts.output || path.join(dir, path.basename(inpath, path.extname(inpath))+".out.json");
			console.log("output written to: "+outpath);
			fs.writeFileSync(outpath, JSON.stringify(result.output, null, '\t')+"\n");
		}
	}

	return result;
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

	// Handle fileData and fileJson options, where file data is passed on the command line.
	function splitInlineFile(s) {
		var i = s.indexOf(':');
		assert(i > 0);
		var name = "./" + path.join(s.substr(0, i));
		var data = s.substr(i + 1);
		return [name, data];
	}
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

	// Add ourlab.js config to URLs by default (it also include config/roboliq.js),
	// but if --no-ourlab is specified, make sure that config/roboliq.js gets loaded.
	const urls = _.uniq(_.compact(
		_.compact([
			(opts.ourlab) ? 'config/ourlab.js' : 'config/roboliq.js'
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

	// Reduce the list of URLs by merging or patching them together, starting
	// with the empty protocol.
	var protocol = _.reduce(
		urlToProtocol_l,
		(protocol, [url, raw]) => {
			if (_.isArray(raw)) {
				return jiff.patch(raw, protocol);
			}
			else {
				var b = loadProtocol(protocol, raw, url, filecache);
				return mergeProtocols(protocol, b);
			}
		},
		protocolEmpty
	);
	/*if (opts.debug) {
		console.log(protocol);
	}*/

	postProcessProtocol(protocol);
	validateProtocol1(protocol);

	var objectToPredicateConverters = protocol.objectToPredicateConverters;

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
	function createStateItems(o, name = "", stateList = []) {
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
				createStateItems(value, prefix + name2, stateList);
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
	function expandProtocol(protocol) {
		var objects0 = _.cloneDeep(protocol.objects);
		_.merge(protocol, {effects: {}, cache: {}, warnings: {}, errors: {}});
		expandStep(protocol, [], protocol.steps, objects0);
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
	function expandStep(protocol, prefix, step, objects, scope = {}) {
		//console.log("expandStep: "+prefix+JSON.stringify(step))
		var commandHandlers = protocol.commandHandlers;
		var id = prefix.join('.');
		if (opts.progress) {
			console.log("step "+id);
		}

		// Process any directives in this step
		const params = misc.handleDirectiveDeep(step, protocol);
		//console.log({step, params})

		// Add `_scope` variables to scope, which are automatically inserted into `protocol.objects.SCOPE` before a command handler is called.
		//console.log({_scope: params._scope})
		if (!_.isEmpty(params._scope)) {
			scope = _.clone(scope);
			_.forEach(params._scope, (value, key) => scope[key] = value);
			//console.log("scope: "+JSON.stringify(scope))
		}

		// If this step is a command:
		const commandName = params.command;
		if (commandName) {
			const handler = commandHandlers[commandName];
			if (!handler) {
				protocol.warnings[id] = ["unknown command: "+params.command];
			}
			// Otherwise, the command has a handler:
			else {
				// Take the initial predicates and append predicates for the current state
				// REFACTOR: this might be a time-consuming process, which could perhaps be
				// sped up by using Immutablejs and checking which objects have changed
				// rather than regenerating predicates for all objects.
				const predicates = protocol.predicates.concat(createStateItems(objects));
				let result = {};
				try {
					const data = {
						objects: _.merge({}, objects, {SCOPE: scope}),
						predicates: predicates,
						planHandlers: protocol.planHandlers,
						schemas: protocol.schemas,
						accesses: [],
						files: filecache
					};
					//if (!_.isEmpty(data.objects.SCOPE)) { console.log({SCOPE: data.objects.SCOPE})}
					// If a schema is given for the command, parse its parameters
					const schema = protocol.schemas[commandName];
					//console.log("params: "+JSON.stringify(params))
					const parsed = (schema)
						? commandHelper.parseParams(params, data, schema)
						: undefined;
					// Try to run the command handler
					//console.log("A")
					//console.log(handler)
					result = handler(params, parsed, data) || {};
					//console.log("B")
					//console.log("result: "+JSON.stringify(result))
				} catch (e) {
					if (e.hasOwnProperty("errors")) {
						result = {errors: e.errors};
					}
					else {
						result = {errors: _.compact([e.toString(), e.stack])};
					}
					if (opts.throw) {
						if (_.isPlainObject(e))
							console.log("e:\n"+JSON.stringify(e));
						throw e;
					}
				}
				// If debugging, store the result verbatim
				if (opts.debug)
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
				if (result.hasOwnProperty("expansion")) {
					// If an array was returned rather than an object, put it in the proper form
					if (_.isArray(result.expansion)) {
						//console.log("expansion0:\n"+JSON.stringify(result.expansion, null, '  '))
						var l = _.compact(_.flattenDeep(result.expansion));
						result.expansion = _.zipObject(_.range(1, l.length + 1), l);
						//console.log("expansion:\n"+JSON.stringify(result.expansion, null, '  '))
					}
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
			}
		}

		// Find all sub-steps (properties that start with a digit)
		var keys = _.filter(_.keys(step), function(key) {
			var c = key[0];
			return (c >= '0' && c <= '9');
		});
		// Sort them in "natural" order
		keys.sort(naturalSort);
		// Try to expand the substeps
		for (const key of keys) {
			expandStep(protocol, prefix.concat(key), step[key], objects, scope);
		}
	}

	// If initial processing didn't result in any errors,
	//  expand steps and get final objects.
	const objectsFinal = (_.isEmpty(protocol.errors))
		? expandProtocol(protocol)
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

	// If there were errors,
	if (!_.isEmpty(protocol.errors)) {
		//return {protocol: protocol, output: _.pick(protocol, 'errors', 'warnings')};
		return {protocol: protocol, output: protocol};
	}
	// Otherwise create tables
	else {
		const output = _.merge(
			{},
			{
				roboliq: version,
				objects: protocol.objects,
				steps: protocol.steps,
				effects: protocol.effects,
				warnings: protocol.warnings,
				errors: protocol.errors
			}
		);

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

		output.tables = tables;

		return {protocol: protocol, output: output};
	}
}

module.exports = {
	run: run
}
