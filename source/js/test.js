var _ = require('lodash');
var naturalSort = require('javascript-natural-sort');
var movePlatePlanning = require('./movePlatePlanning.js');

var protocol1 = {
  "objects": {
    "ourlab": {
      "type": "Namespace",
      "mario": {
        "type": "Namespace",
        "evoware": {
          "type": "EvowareRobot"
        },
        "arm1": {
          "type": "Transporter",
          "evowareRoma": 0
        },
        "P2": {
          "type": "Site",
          "evowareCarrier": "MP 2Pos H+P Shake",
          "evowareGrid": 10,
          "evowareSite": 2
        },
        "P3": {
          "type": "Site",
          "evowareCarrier": "MP 2Pos H+P Shake",
          "evowareGrid": 10,
          "evowareSite": 4
        }
      },
      "model1": {
        "type": "PlateModel",
        "evowareName": "Ellis Nunc F96 MicroWell"
      }
    },
    "plate1": {
      "type": "Plate",
      "model": "ourlab.model1",
      "location": "ourlab.mario.P2"
    }
  },
  "steps": {
    "1": {
      "command": "instruction.transporter.movePlate",
      "agent": "ourlab.mario.evoware",
      "equipment": "ourlab.mario.arm1",
      "program": "Narrow",
      "object": "plate1",
      "destination": "ourlab.mario.P3"
    },
    "2": {
      "command": "instruction.transporter.movePlate",
      "agent": "ourlab.mario.evoware",
      "equipment": "ourlab.mario.arm1",
      "program": "Narrow",
      "object": "plate1",
      "destination": "ourlab.mario.P2"
    }
  }
};

var protocol2 = {
  "objects": {
    "ourlab": {
      "type": "Namespace",
      "mario": {
        "type": "Namespace",
        "evoware": {
          "type": "EvowareRobot"
        },
        "arm1": {
          "type": "Transporter",
          "evowareRoma": 0
        },
        "P2": {
          "type": "Site",
          "evowareCarrier": "MP 2Pos H+P Shake",
          "evowareGrid": 10,
          "evowareSite": 2
        },
        "P3": {
          "type": "Site",
          "evowareCarrier": "MP 2Pos H+P Shake",
          "evowareGrid": 10,
          "evowareSite": 4
        }
      },
      "model1": {
        "type": "PlateModel",
        "evowareName": "Ellis Nunc F96 MicroWell"
      }
    },
    "plate1": {
      "type": "Plate",
      "model": "ourlab.model1",
      "location": "ourlab.mario.P2"
    }
  },
  "steps": {
    "1": {
      "command": "action.transporter.movePlate",
      "agent": "ourlab.mario.evoware",
      "equipment": "ourlab.mario.arm1",
      "program": "Narrow",
      "object": "plate1",
      "destination": "ourlab.mario.P3"
    },
    "2": {
      "command": "action.transporter.movePlate",
      "agent": "ourlab.mario.evoware",
      "equipment": "ourlab.mario.arm1",
      "program": "Narrow",
      "object": "plate1",
      "destination": "ourlab.mario.P2"
    }
  }
};

function fillStateItems(name, o, stateList) {
  //console.log("name: "+name);
  if (o.hasOwnProperty("type")) {
    //console.log("type: "+o.type);
    switch (o['type']) {
      case 'Plate':
        stateList.push({"isLabware": {"labware": name}});
        stateList.push({"isPlate": {"labware": name}});
        stateList.push({"model": {"labware": name, "model": o.model}});
        stateList.push({"location": {"labware": name, "site": o.location}});
        break;
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

var commands = {
	"instruction.transporter.movePlate": {
		getEffects: function(params, objects) {
			var effects = {};
			effects[params.object+".location"] = params.destination;
			return {effects: effects};
		}
	},
	"action.transporter.movePlate": {
		getEffects: function(params, objects) {
			var effects = {};
			effects[params.object+".location"] = params.destination;
			return {effects: effects};
		},
		expand: function(params, objects) {
			var expansion = {};
      var stateList = createStateItems(objects);
      var movePlatePlanning = require('./movePlatePlanning.js');
      var taskList = [];
      if (params.hasOwnProperty("agent")) {
        taskList.push({"movePlate-a": {"agent": params.agent, "labware": params['object'], "destination": params.destination}});
      }
      else {
        taskList.push({"movePlate": {"labware": params['object'], "destination": params.destination}});
      }
      var tasks = {"tasks": {"ordered": taskList}};
      var input = [].concat(movePlatePlanning.evowareConfig, movePlatePlanning.taskDefs, stateList, [tasks]);
      console.log(JSON.stringify(input, null, '\t'));
      var shop = require('./HTN/Plan/shop.js');
      //var p = shop.makePlanner(sealerExample);
      var planner = shop.makePlanner(input);
      var plan = planner.plan();
      console.log("state:");
      console.log(JSON.stringify(plan.state));
      var x = planner.ppPlan(plan);
      CONTINUE
      console.log(x);
			var cmd1 = {
				command: "instruction.transporter.movePlate",
				agent: params.agent,
				equipment: params.equipment,
				program: params.program,
				object: params.object,
				destination: params.destination
			};
			expansion["1"] = cmd1;
			return {expansion: expansion};
		}
	}
};

function expandSteps(prefix, steps, objects) {
	var keys = _(steps).keys(steps).filter(function(key) {
		var c = key[0];
		return (c >= '0' && c <= '9');
	}).value();
	keys.sort(naturalSort);
	console.log(keys);
	_.forEach(keys, function(key) {
		var step = steps[key];
		if (step.hasOwnProperty("command")) {
			if (commands.hasOwnProperty(step.command)) {
				var command = commands[step.command];
				var canExpand = command.hasOwnProperty("expand");
				var isExpanded = command.hasOwnProperty("1");
				if (canExpand & !isExpanded) {
					var result = command.expand(step, objects);
					if (result.hasOwnProperty("expansion")) {
						_.merge(step, result.expansion);
					}
				}
				expandSteps(prefix+"."+key, step, objects);
			}
		}
	});
}

/**
 * Once the protocol is fully processed, call this to generate a list of instructions to pass to the robot compilers.
 */
function gatherInstructions(prefix, steps, objects) {
	var instructions = [];
	var keys = _(steps).keys(steps).filter(function(key) {
		var c = key[0];
		return (c >= '0' && c <= '9');
	}).value();
	keys.sort(naturalSort);
	console.log(keys);
	_.forEach(keys, function(key) {
		var step = steps[key];
		if (step.hasOwnProperty("command")) {
			if (step.command.indexOf("instruction.") == 0) {
				instructions.push(step);
			}
			if (commands.hasOwnProperty(step.command)) {
				var command = commands[step.command];
				var instructions2 = gatherInstructions(prefix+"."+key, step, objects);
				instructions = instructions.concat(instructions2);
				var canGetEffects = command.hasOwnProperty("getEffects");
				if (canGetEffects) {
					var result = command.getEffects(step, objects);
					if (result.hasOwnProperty("effects")) {
						instructions.push({"let": result.effects});
					}
				}
			}
		}
		else if (step.hasOwnProperty("let")) {
			instructions.push({"let": step["let"]});
		}
	});
	return instructions;
}

var protocol = protocol1;
console.log(JSON.stringify(protocol, null, '\t'));
var instructions = gatherInstructions("steps", protocol.steps, protocol.objects);
console.log(JSON.stringify(instructions, null, '\t'));

//console.log(JSON.stringify(createStateItems(protocol2), null, '\t'));

protocol = protocol2;
expandSteps("steps", protocol.steps, protocol.objects);
console.log(JSON.stringify(protocol.steps, null, '\t'));
instructions = gatherInstructions("steps", protocol.steps, protocol.objects);
console.log(JSON.stringify(instructions, null, '\t'));
