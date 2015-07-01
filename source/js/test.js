var _ = require('lodash');
var naturalSort = require('javascript-natural-sort');
var roboliq = require('./roboliq.js');
var commands = {
  transporter: require('./commands/transporter.js')
};
var ourlab = require('./ourlab.js');

var protocolFilename = './protocols/protocol2.json';
var protocol0 = require(protocolFilename);

function mergeObjects(name) {
  return _.merge({}, roboliq[name], commands.transporter[name], ourlab[name], protocol0[name]);
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
        _.forEach(result.value, function(value) { stateList.push(value); });
      }
    }
  }

  var prefix = _.isEmpty(name) ? "" : name+".";
  _.forEach(o, function(value, name2) {
    //console.log(name2, value);
    if (_.isObject(value)) {
      fillStateItems(prefix+name2, value, stateList);
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
    o[nameList[0]] = (nameList.length == 1)
      ? value : createSimpleObject(_.rest(nameList), value);
    return o;
  }
}

function expandSteps(prefix, steps, objects, effects) {
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
					if (result.hasOwnProperty("expansion")) {
						_.merge(step, result.expansion);
					}
          if (result.hasOwnProperty("effects")) {
            console.log(result.effects);
            effects[id] = result.effects;
            _.forEach(result.effects, function(value, key) {
              var nameList = key.split('.');
              var o = createSimpleObject(nameList, value);
              _.merge(objects, o);
            });
          }
				}
				expandSteps(prefix2, step, objects, effects);
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
			if (step.command.indexOf("instruction.") == 0) {
				instructions.push(step);
			}
      var prefix2 = prefix.concat([key]);
      var id = prefix2.join('.');

			var instructions2 = gatherInstructions(prefix2, step, objects, effects);
			instructions = instructions.concat(instructions2);

			if (effects.hasOwnProperty(id)) {
        var item = {"let": effects[id]};
        if (_.isEmpty(instructions) || !_.isEqual(_.last(instructions), item))
				  instructions.push(item);
			}
		}
		else if (step.hasOwnProperty("let")) {
			instructions.push({"let": step["let"]});
		}
	});
	return instructions;
}

/*
var objects0 = _.cloneDeep(protocol.objects);
var effects = {};
expandSteps([], protocol.steps, objects0, effects);
console.log();
console.log("Steps:")
console.log(JSON.stringify(protocol.steps, null, '\t'));
console.log();
console.log("Effects:")
console.log(JSON.stringify(effects, null, '\t'));

instructions = gatherInstructions([], protocol.steps, protocol.objects, effects);
console.log();
console.log("Instructions:")
console.log(JSON.stringify(instructions, null, '\t'));
*/
var llpl = require('./HTN/llpl.js');
var predicates = protocol.predicates.concat(createStateItems(protocol.objects));
llpl.initializeDatabase(predicates);
var q1 = {"sealer.canAgentEquipmentProgramModelSite": {"agent": "?agent", "equipment": "?equipment", "program": "?program", "model": "?model", "site": "?site"}};
var resultList = llpl.query(q1);
//console.log(resultList);
console.log(JSON.stringify(resultList, null, '  '));
var misc = require('./misc.js');
var acc = misc.extractValuesFromQueryResults(resultList, "sealer.canAgentEquipmentProgramModelSite");
console.log(JSON.stringify(acc, null, '  '))
/*
var x = _(resultList).map(function(x) {
  return _.map(x, function(x) {
    return _.map(x, function(x, name) {
      return [name, x];
    });
  });
}).flatten().flatten().groupBy(function(l) { return l[0]; }).value();
console.log(JSON.stringify(x, null, '  '));
*/
