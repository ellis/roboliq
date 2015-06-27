var _ = require('lodash');
var naturalSort = require('javascript-natural-sort');

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

var commands = {
	"instruction.transporter.movePlate": {
		getEffects: function(params, objects) {
			var effects = {};
			effects[params.object+".location"] = params.destination;
			return effects;
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
			var cmd1 = {
				command: "instruction.transporter.movePlate",
				agent: params.agent,
				equipment: params.equipment,
				program: params.program,
				object: params.object,
				destination: params.destination
			};
			expansion["1"] = cmd1;
			return expansion;
		}
	}
};

var protocol = protocol1;

console.log(JSON.stringify(protocol, null, '\t'));

function processSteps(steps) {
	var keys = _.keys(steps);
	keys.sort(naturalSort);
	console.log(keys);
}

processSteps(protocol.steps);

