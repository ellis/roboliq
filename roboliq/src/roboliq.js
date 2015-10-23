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
var naturalSort = require('javascript-natural-sort');
var path = require('path');
var yaml = require('yamljs');
var expect = require('./expect.js');
var misc = require('./misc.js');
import * as WellContents from './WellContents.js';
var wellsParser = require('./parsers/wellsParser.js');

var version = "v1";

var nomnom = require('nomnom').options({
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
		help: 'specify output filename or "" for standard output, otherwise default is used',
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

var protocolEmpty = {
	objects: {},
	steps: {},
	effects: {},
	predicates: [],
	directiveHandlers: {},
	objectToPredicateConverters: {},
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

	var c = _.cloneDeep(_.pick(b,
		'objects',
		'steps',
		'effects',
		'predicates',
		'directiveHandlers',
		'objectToPredicateConverters',
		'commandHandlers',
		'planHandlers',
		'files',
		'errors',
		'warnings'
	));

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
	if (c.objects && !c.predicates)
	console.log(JSON.stringify(c, null, '\t'));

	return d;
}

/**
 * Remove properties with '?'-prefix.  If the propery value has a '!value' property,
 * add a new property to the object without the '?' prefix and with the given value.
 * Mutates the object.
 * Also add the path to the property to the protocol's `fillIns`
 * @param  {Protocol} protocol
 * @param  {any} obj
 * @param  {array} path
 */
function preProcessQuestionMarks(protocol, obj, path) {
	if (_.isPlainObject(obj)) {
		_.forEach(obj, (value, name) => {
			if (_.endsWith(name, "?")) {
				delete obj[name];
				const name1 = name.slice(0, -1);
				if (value.hasOwnProperty('value!')) {
					obj[name1] = value['value!'];
				}
				else {
					protocol.fillIns[path.concat(name1).join('.')] = value || {};
				}
			}
			else {
				preProcessQuestionMarks(protocol, value, path.concat(name));
			}
		});
	}
	else if (_.isArray(obj)) {
		_.forEach(obj, (value, index) => {
			preProcessQuestionMarks(protocol, value, path.concat(index));
		});
	}
}

/**
 * Remove properties with '?'-prefix.  If the propery value has a '!value' property,
 * add a new property to the object without the '?' prefix and with the given value.
 * Mutates the object.
 * Also add the path to the property to the protocol's `fillIns`
 * @param  {Protocol} protocol
 * @param  {any} obj
 * @param  {array} path
 */
function preProcessExclamationMarks(protocol, obj, path) {
	if (_.isPlainObject(obj)) {
		const keys = _.keys(obj);
		let reorder = false;
		for (const name of keys) {
			const value = obj[name];
			if (_.endsWith(name, "!")) {
				delete obj[name];
				const name1 = name.slice(0, -1);
				//console.log({name, name1});
				obj[name1] = value;
				//console.log({obj, name1})
				reorder = true;
			}
			else {
				preProcessExclamationMarks(protocol, value, path.concat(name));
			}
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
	//console.log("a.predicates:", a.predicates);
	//console.log("b.predicates:", b.predicates);

	var c = _.merge({}, a, b);
	c.predicates = a.predicates.concat(b.predicates || []);
	//console.log("c:", c);
	return c;
}

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
						assert(_.isEqual(_.rest(pair[0]), [name]), "well "+well+" already contains different contents: "+JSON.stringify(pair[0]));
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

function run(argv, userProtocol) {
	var opts = nomnom.parse(argv);
	if (_.isEmpty(opts.infiles) && !userProtocol) {
		console.log(nomnom.getUsage());
		process.exit(0);
	}

	var result = undefined;
	try {
		result = _run(opts, userProtocol);
	} catch (e) {
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

	if (result && result.output) {
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

function _run(opts, userProtocol) {

	if (opts.debug) {
		console.log("opts:", opts);
	}

	var filecache = {};
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

	var urls = _.uniq(_.compact(
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
	if (userProtocol)
		urlToProtocol_l.push([undefined, userProtocol]);

	// Load
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

	var objectToPredicateConverters = protocol.objectToPredicateConverters;

	function fillStateItems(name, o, stateList) {
		//console.log("name: "+name);
		if (o.hasOwnProperty("type")) {
			//console.log("type: "+o.type);
			var type = o['type'];
			if (objectToPredicateConverters.hasOwnProperty(type)) {
				var result = objectToPredicateConverters[type](name, o);
				if (result.value) {
					_.forEach(result.value, function(value) {
						stateList.push(value);
					});
				}
			}
		}

		var prefix = _.isEmpty(name) ? "" : name + ".";
		_.forEach(o, function(value, name2) {
			//console.log(name2, value);
			if (_.isPlainObject(value)) {
				fillStateItems(prefix + name2, value, stateList);
			}
		});
	}

	/** Create state items for SHOP planning from the protocol's objects. */
	function createStateItems(objects) {
		var stateList = [];
		fillStateItems("", objects, stateList);
		//console.log(JSON.stringify(stateList, null, '\t'));
		return stateList;
	}

	function createSimpleObject(nameList, value) {
		if (_.isEmpty(nameList)) return null;
		else {
			var o = {};
			o[nameList[0]] = (nameList.length == 1) ? value : createSimpleObject(_.rest(nameList), value);
			return o;
		}
	}

	function expandProtocol(protocol) {
		var objects0 = _.cloneDeep(protocol.objects);
		_.merge(protocol, {effects: {}, cache: {}, warnings: {}, errors: {}});
		expandStep(protocol, [], protocol.steps, objects0);
		return objects0;
	}

	function expandStep(protocol, prefix, step, objects) {
		//console.log("expandStep: "+prefix+JSON.stringify(step))
		var commandHandlers = protocol.commandHandlers;
		var id = prefix.join('.');
		if (opts.progress) {
			console.log("step "+id);
		}

		const params = misc.handleDirectiveDeep(step, protocol);
		//console.log({step, params})
		const commandName = params.command;
		if (commandName) {
			const handler = commandHandlers[commandName];
			if (!handler) {
				protocol.warnings[id] = ["unknown command: "+params.command];
			}
			else {
				var expand = true
				if (expand) {
					var predicates = protocol.predicates.concat(createStateItems(objects));
					var result = {};
					try {
						var data = {
							objects: objects,
							predicates: predicates,
							planHandlers: protocol.planHandlers,
							accesses: [],
							files: filecache
						};
						result = handler(params, data) || {};
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
					if (result.hasOwnProperty("expansion")) {
						if (_.isArray(result.expansion)) {
							//console.log("expansion0:\n"+JSON.stringify(result.expansion, null, '  '))
							var l = _.compact(_.flattenDeep(result.expansion));
							result.expansion = _.zipObject(_.range(1, l.length + 1), l);
							//console.log("expansion:\n"+JSON.stringify(result.expansion, null, '  '))
						}
						_.merge(step, result.expansion);
					}
					if (result.hasOwnProperty("effects") && !_.isEmpty(result.effects)) {
						//console.log(result.effects);
						// Add effects to protocol
						protocol.effects[id] = result.effects;
						//console.log("mixPlate.contents.C01 #0: "+_.get(objects, "mixPlate.contents.C01"));
						// Update object states
						_.forEach(result.effects, (value, key) => _.set(objects, key, value));
						//console.log("mixPlate.contents.C01 #1: "+_.get(objects, "mixPlate.contents.C01"));
					}
				}
			}
		}

		var keys = _.filter(_.keys(step), function(key) {
			var c = key[0];
			return (c >= '0' && c <= '9');
		});
		keys.sort(naturalSort);
		_.forEach(keys, function(key) {
			expandStep(protocol, prefix.concat(key), step[key], objects);
		});
	}

	/**
	 * Once the protocol is fully processed, call this to generate a list of instructions to pass to the robot compilers.
	 */
	function gatherInstructions(prefix, steps, objects, effects) {
		var instructions = [];
		var keys = _(steps).keys(steps).filter(function(key) {
			var c = key[0];
			return (c >= '0' && c <= '9');
		}).value();
		keys.sort(naturalSort);
		//console.log(keys);
		_.forEach(keys, function(key) {
			var step = steps[key];
			if (step.hasOwnProperty("command")) {
				if (step.command.indexOf("._") >= 0) {
					instructions.push(step);
				}
				var prefix2 = prefix.concat([key]);
				var id = prefix2.join('.');

				var instructions2 = gatherInstructions(prefix2, step, objects, effects);
				instructions = instructions.concat(instructions2);

				if (effects.hasOwnProperty(id)) {
					var item = {
						"let": effects[id]
					};
					if (_.isEmpty(instructions) || !_.isEqual(_.last(instructions), item))
						instructions.push(item);
				}
			} else if (step.hasOwnProperty("let")) {
				instructions.push({
					"let": step["let"]
				});
			}
		});
		return instructions;
	}

	var objectsFinal = protocol.objects;
	if (_.isEmpty(protocol.errors)) {
		objectsFinal = expandProtocol(protocol);
	}
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

	if (!_.isEmpty(protocol.errors)) {
		//return {protocol: protocol, output: _.pick(protocol, 'errors', 'warnings')};
		return {protocol: protocol, output: protocol};
	}
	else {
		//var instructions = gatherInstructions([], protocol.steps, protocol.objects, protocol.effects);
		var output = _.merge(
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

		var tables = {
			labware: [],
			sourceWells: [],
			wellContentsFinal: []
		};
		// Construct labware table
		var labwares = misc.getObjectsOfType(objectsFinal, ['Plate', 'Tube']);
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

		//
		_.merge(output, {tables: tables});

		return {protocol: protocol, output: output};
	}
}

module.exports = {
	run: run
}
