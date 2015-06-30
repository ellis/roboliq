var _ = require('lodash');
var naturalSort = require('javascript-natural-sort');
var ourlab = require('./ourlab.js');
var roboliq = require('./roboliq.js');

var protocol2 = {
  "objects": {
    "plate1": {
      "type": "Plate",
      "model": "ourlab.model1",
      "location": "ourlab.mario.P2"
    }
  },
  "steps": {
    "1": {
      "command": "action.transporter.movePlate",
      "object": "plate1",
      "destination": "ourlab.mario.P3"
    },
    "2": {
      "command": "action.transporter.movePlate",
      "object": "plate1",
      "destination": "ourlab.mario.SEALER"
    }
  }
};
_.merge(protocol2.objects, ourlab.objects);

var objectToLogicConverters = _.merge({}, roboliq.objectToLogicConverters, ourlab.objectToLogicConverters);
function fillStateItems(name, o, stateList) {
  //console.log("name: "+name);
  if (o.hasOwnProperty("type")) {
    //console.log("type: "+o.type);
    var type = o['type'];
    if (objectToLogicConverters.hasOwnProperty(type)) {
      var result = objectToLogicConverters[type](name, o);
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

var commandHandlers = roboliq.commandHandlers;

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
          var objectLogics = ourlab.logic.concat(createStateItems(objects));
					var result = handler(step, objects, objectLogics);
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
var protocol = protocol1;
console.log(JSON.stringify(protocol, null, '\t'));
var instructions = gatherInstructions("steps", protocol.steps, protocol.objects);
console.log(JSON.stringify(instructions, null, '\t'));
*/

//console.log(JSON.stringify(createStateItems(protocol2), null, '\t'));

protocol = protocol2;
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
