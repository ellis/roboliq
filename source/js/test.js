var _ = require('lodash');
var fs = require('fs');
var naturalSort = require('javascript-natural-sort');
var roboliq = require('./roboliq.js');
var commands = {
	sealer: require('./commands/sealer.js'),
	transporter: require('./commands/transporter.js')
};
var ourlab = require('./ourlab.js');

var protocolFilename = './protocols/protocol3.json';
var protocol0 = require(protocolFilename);

function mergeObjects(name) {
	return _.merge({},
		roboliq[name],
		commands.sealer[name],
		commands.transporter[name],
		ourlab[name],
		protocol0[name]
	);
}

function mergeArrays(name) {
	return _.compact([].concat(roboliq[name], commands.transporter[name], ourlab[name], protocol0[name]));
}
var protocol = {
	objects: mergeObjects("objects"),
	steps: mergeObjects("steps"),
	effects: mergeObjects("effects"),
	predicates: mergeArrays("predicates"),
	taskPredicates: mergeArrays("taskPredicates"),
	objectToPredicateConverters: mergeObjects("objectToPredicateConverters"),
	commandHandlers: mergeObjects("commandHandlers"),
	planHandlers: mergeObjects("planHandlers")
};

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
		if (_.isObject(value)) {
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

var commandHandlers = protocol.commandHandlers;

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
}

function expandSteps(protocol, prefix, steps, objects) {
	var keys = _(steps).keys(steps).filter(function(key) {
		var c = key[0];
		return (c >= '0' && c <= '9');
	}).value();
	keys.sort(naturalSort);
	//console.log(keys);
	_.forEach(keys, function(key) {
		var prefix2 = prefix.concat([key]);
		var id = prefix2.join('.');
		var step = steps[key];
		var isExpanded = step.hasOwnProperty("1");
		if (step.hasOwnProperty("command")) {
			if (commandHandlers.hasOwnProperty(step.command)) {
				var handler = commandHandlers[step.command];
				if (!isExpanded) {
					var predicates = protocol.predicates.concat(createStateItems(objects));
					var result = handler(step, objects, predicates, protocol.planHandlers);
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
					if (result.hasOwnProperty("effects")) {
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

expandProtocol(protocol);
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

if (!_.isEmpty(protocol.errors)) {
	console.log();
	console.log("Errors:");
	_.forEach(protocol.errors, function(err, id) {
		console.log(id+": "+err.toString());
	});
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
	console.log();
	console.log("Output:")
	console.log(JSON.stringify(output, null, '\t'));
	fs.writeFileSync('output.json', JSON.stringify(output, null, '\t')+"\n");
}
