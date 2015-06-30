var _ = require('lodash');

var objectToLogicConverters = {
  "Plate": function(name, object) {
    return {value: [
      {"isLabware": {"labware": name}},
      {"isPlate": {"labware": name}},
      {"model": {"labware": name, "model": object.model}},
      {"location": {"labware": name, "site": object.location}}
    ]};
  },
  "PlateModel": function(name, object) {
    return {value: [{"isModel": {"model": name}}]};
  },
  "Site": function(name, object) {
    return {value: [{"isSite": {"model": name}}]};
  },
  "Transporter": function(name, object) {
    return {value: [{"isTransporter": {"equipment": name}}]};
  },
};

var planTaskHandlers = {
  "movePlateAction": function(params, parentParams, objects) {
    return [{
      command: "instruction.transporter.movePlate",
      agent: params.agent,
      equipment: params.equipment,
      program: params.program,
      object: params.labware,
      destination: params.destination
    }];
  },
  "print": function() { return []; }
}

var commandHandlers = {
	"instruction.transporter.movePlate": function(params, objects) {
		var effects = {};
		effects[params.object+".location"] = params.destination;
		return {effects: effects};
	},
	"action.transporter.movePlate": function(params, objects, objectLogics) {
    var movePlatePlanning = require('./movePlatePlanning.js');
    var taskList = [];
    if (params.hasOwnProperty("agent")) {
      taskList.push({"movePlate-a": {"agent": params.agent, "labware": params['object'], "destination": params.destination}});
    }
    else {
      taskList.push({"movePlate": {"labware": params['object'], "destination": params.destination}});
    }
    var tasks = {"tasks": {"ordered": taskList}};
    var input = [].concat(objectLogics, movePlatePlanning.taskDefs, [tasks]);
    console.log(JSON.stringify(input, null, '\t'));
    var shop = require('./HTN/shop.js');
    var planner = shop.makePlanner(input);
    var plan = planner.plan();
    console.log("plan:");
    console.log(JSON.stringify(plan, null, '  '));
    //var x = planner.ppPlan(plan);
    //console.log(x);
    var tasks = planner.listAndOrderTasks(plan, true);
    //console.log("Tasks:")
    //console.log(JSON.stringify(tasks, null, '  '));
    var cmdList = _(tasks).map(function(task) {
      return _(task).map(function(taskParams, taskName) {
        return (planTaskHandlers.hasOwnProperty(taskName))
          ? planTaskHandlers[taskName](taskParams, params, objects)
          : [];
      }).flatten().value();
    }).flatten().value();
    //console.log("cmdList:")
    //console.log(JSON.stringify(cmdList, null, '  '));

    // Create the expansion object
    var expansion = {};
    var i = 1;
    _.forEach(cmdList, function(cmd) {
      expansion[i.toString()] = cmd;
      i += 1;
    });

    // Create the effets object
    var effects = {};
		effects[params.object+".location"] = params.destination;

    return {
      expansion: expansion,
      effects: effects
    };
	}
};

module.exports = {
  objectToLogicConverters: objectToLogicConverters,
  commandHandlers: commandHandlers,
  planTaskHandlers: planTaskHandlers
};
