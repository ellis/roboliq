var _ = require('lodash');
var assert = require('assert');
var fs = require('fs');
var naturalSort = require('javascript-natural-sort');
var path = require('path');
var yaml = require('yamljs');
var expect = require('./expect.js');
var misc = require('./misc.js');
var pipetterUtils = require('./commands/pipetter/pipetterUtils.js');
var wellsParser = require('./parsers/wellsParser.js');

function run(argv, userProtocol) {
	var opts = require('nomnom')
		.options({
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
					return "version 0.1";
				}
			},
		})
		.parse(argv);

	if (opts.debug) {
		console.log("opts:", opts);
	}

	var urls = _.uniq(_.compact(
		[
			'config/roboliq.js',
			'commands/centrifuge.js',
			'commands/pipetter.js',
			'commands/sealer.js',
			'commands/transporter.js',
			'config/ourlab.js'
		].concat(opts.infiles)
	));
	if (opts.debug) {
		console.log("urls:", urls);
	}

	function loadProtocolUrl(url) {
		var content = null;
		if (url.indexOf("./") != 0)
			url = "./"+url;
		if (path.extname(url) === ".yaml") {
			content = yaml.load(url);
		}
		else {
			content = require(url);
		}
		return content;
	}

	function mergeProtocols(a, b) {
		//console.log("a.predicates:", a.predicates);
		//console.log("b.predicates:", b.predicates);
		var c = misc.handleDirectiveDeep(b, a);
		//console.log("c.predicates:", c.predicates);
		var d = _.merge({}, a, c);
		//console.log("d.predicates:", d.predicates);
		d.predicates = a.predicates.concat(c.predicates || []);
		return d;
	}

	function mergeProtocolUrls(urls) {
		var protocol = {
			objects: {},
			steps: {},
			effects: {},
			predicates: [],
			directiveHandlers: {},
			objectToPredicateConverters: {},
			commandHandlers: {},
			planHandlers: {},
			errors: {},
		};
		_.forEach(urls, function(url) {
			var protocol2 = loadProtocolUrl(url);
			protocol = mergeProtocols(protocol, protocol2)
		});
		if (userProtocol) {
			protocol = mergeProtocols(protocol, userProtocol);
		}
		return protocol;
	}

	// Post-processing of protocol
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

	var protocol = mergeProtocolUrls(urls);
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
		expandSteps(protocol, [], protocol.steps, objects0);
		return objects0;
	}

	function expandSteps(protocol, prefix, steps, objects) {
		var commandHandlers = protocol.commandHandlers;
		var keys = _(steps).keys(steps).filter(function(key) {
			var c = key[0];
			return (c >= '0' && c <= '9');
		}).value();
		keys.sort(naturalSort);
		//console.log("keys: ",keys);
		_.forEach(keys, function(key) {
			var prefix2 = prefix.concat([key]);
			var id = prefix2.join('.');
			//console.log("id: "+id)
			var step = steps[key];
			var isExpanded = step.hasOwnProperty("1");
			if (step.hasOwnProperty("command")) {
				if (!commandHandlers.hasOwnProperty(step.command)) {
					protocol.warnings[id] = ["unknown command: "+step.command];
				}
				else {
					var handler = commandHandlers[step.command];
					if (!isExpanded) {
						var predicates = protocol.predicates.concat(createStateItems(objects));
						var result = {};
						try {
							var data = {
								objects: objects,
								predicates: predicates,
								planHandlers: protocol.planHandlers
							};
							result = handler(step, data);
						} catch (e) {
							if (e.hasOwnProperty("errors")) {
								result = {errors: e.errors};
							}
							else {
								result = {errors: [e.toString()]};
							}
							if (opts.throw)
								throw e;
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
					expandSteps(protocol, prefix2, step, objects);
				}
			}
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
				if (step.command.indexOf("instruction.") >= 0) {
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
		expandProtocol(protocol);
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
		return {protocol: protocol};
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
				objects: protocol.objects,
				steps: protocol.steps,
				effects: protocol.effects,
				warnings: protocol.warnings,
				errors: protocol.errors
			}
		);

		var tables = {
			labware: [],
			sourceWells: []
		}
		// Construct labware table
		_.forEach(misc.getObjectsOfType(objectsFinal, ['Plate']), function(labware, name) {
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
		}
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
		}
		tabulateWELLS(objectsFinal['__WELLS__'] || {}, []);

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
