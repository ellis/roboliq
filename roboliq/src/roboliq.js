var _ = require('lodash');
var assert = require('assert');
var fs = require('fs');
var jsonfile = require('jsonfile');
var naturalSort = require('javascript-natural-sort');
var path = require('path');
var yaml = require('yamljs');
var expect = require('./expect.js');
var misc = require('./misc.js');
var pipetterUtils = require('./commands/pipetter/pipetterUtils.js');
var wellsParser = require('./parsers/wellsParser.js');

var version = "v0.1";

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
		help: "don't automatically load config/ourlab.js"
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
	errors: {},
	warnings: {}
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
 * Pre-process a protocol: handle imports, directives, and file nodes
 * @param  {Object} a   Previously loaded protocol data
 * @param  {Object} b   The protocol to pre-process
 * @param  {String} url (optional) The url of the protocol
 * @return {Object}
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
	var data = {
		objects: _.merge({}, a.objects, imported.objects, c.objects),
		directiveHandlers: _.merge({}, a.directiveHandlers, imported.directiveHandlers, b.directiveHandlers)
	};

	// Handle directives for objects first
	// This is more complicated than for the properties, because objects may have directives which reference other objects.
	function mutateObjects(x, path) {
		if (_.isPlainObject(x)) {
			for (var key in x) {
				var value1 = x[key];
				if (_.isArray(value1)) {
					x[key] = _.map(value1, function(x2) { return misc.handleDirective(x2, data); });
				}
				else {
					x[key] = mutateObjects(value1, path.concat([key]));
				}
			}
		}
		var x2 = misc.handleDirective(x, data);
		_.set(data.objects, path.join('.'), x2);
		return x2;
	}
	mutateObjects(c.objects, []);

	// Handle directives for other properties
	var l = [
		'steps',
		'effects',
		'predicates',
		'files',
		'errors',
		'warnings'
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
	//	console.log(JSON.stringify(b))

	return d;
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
	var liquids = misc.getObjectsOfType(protocol.objects, 'Liquid');
	_.forEach(liquids, function(liquid, name) {
		if (_.isString(liquid.wells)) {
			try {
				liquid.wells = wellsParser.parse(liquid.wells, protocol.objects);
			} catch (e) {
				protocol.errors[name+".wells"] = [e.toString()];
				//console.log(e.toString());
			}
		}
	})
}

function run(argv, userProtocol) {
	var opts = nomnom.parse(argv);

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
		function(protocol, pair) {
			var url = pair[0];
			var raw = pair[1];
			var b = loadProtocol(protocol, raw, url, filecache);
			return mergeProtocols(protocol, b);
		},
		protocolEmpty
	);

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

		if (step.hasOwnProperty("command")) {
			if (!commandHandlers.hasOwnProperty(step.command)) {
				protocol.warnings[id] = ["unknown command: "+step.command];
			}
			else {
				var handler = commandHandlers[step.command];
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
						result = handler(step, data) || {};
					} catch (e) {
						if (e.hasOwnProperty("errors")) {
							result = {errors: e.errors};
						}
						else {
							result = {errors: [e.toString()]};
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
						// Update object states
						_.forEach(result.effects, function(value, key) {
							var nameList = key.split('.');
							var o = createSimpleObject(nameList, value);
							_.merge(objects, o);
						});
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
		console.log("Protocol:")
		console.log(JSON.stringify(protocol, null, '\t'));
		/*console.log();
		console.log("Steps:")
		console.log(JSON.stringify(protocol.steps, null, '\t'));
		console.log();
		console.log("Effects:")
		console.log(JSON.stringify(effects, null, '\t'));
		*/
	}

	var output = undefined;
	if (!_.isEmpty(protocol.errors)) {
		console.log();
		console.log("Errors:");
		_.forEach(protocol.errors, function(err, id) {
			console.log(id+": "+err.toString());
		});
		return {protocol: protocol, output: _.merge({}, {errors: protocol.errors, warnings: protocol.warnings})};
	}
	else {
		if (!_.isEmpty(protocol.warnings)) {
			console.log();
			console.log("Warnings:");
			_.forEach(protocol.warnings, function(err, id) {
				console.log(id+": "+err.toString());
			});
		}
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
		}
		// Construct labware table
		var labwares = misc.getObjectsOfType(objectsFinal, ['Plate', 'Tube'])
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
			//console.log("tabulateWellContents:", contents, labwareName, wellName);
			if (_.isArray(contents)) {
				var map = pipetterUtils.flattenContents(contents);
				var wellName2 = (wellName) ? labwareName+"("+wellName+")" : labwareName;
				tables.wellContentsFinal.push(_.merge({well: wellName2}, map));
			}
			else if (_.isPlainObject(contents)) {
				_.forEach(contents, function(contents2, name2) {
					var wellName2 = _.compact([wellName, name2]).join('.');
					tabulateWellContents(contents2, labwareName, wellName2);
				})
			}
		};
		_.forEach(labwares, function(labware, name) {
			if (labware.contents) {
				tabulateWellContents(labware.contents, name);
			}
		});

		//
		_.merge(output, {tables: tables});

		if (opts.debug) {
			console.log();
			console.log("Output:")
		}
		var outputText = JSON.stringify(output, null, '\t');
		if (opts.debug || opts.print)
			console.log(outputText);

		if (opts.output !== '') {
			var inpath = _.last(opts.infiles);
			var dir = opts.outputDir || path.dirname(inpath);
			var outpath = opts.output || path.join(dir, path.basename(inpath, path.extname(inpath))+".out.json");
			console.log("output written to: "+outpath);
			fs.writeFileSync(outpath, JSON.stringify(output, null, '\t')+"\n");
		}

		return {protocol: protocol, output: output};
	}
}

module.exports = {
	run: run
}
