import commandHelper from '../../commandHelper.js';

module.exports = {
	schemas: {
		"equipment.run": {
			properties: {
				agent: {description: "Agent identifier", type: "Agent"},
				equipment: {description: "Equipment identifier", type: "Equipment"},
				program: {description: "Program identifier for sealing", type: "string"}
			},
			required: ["agent", "equipment", "program"]
		}
	},
	commandHandlers: {
		"equipment.run": function(params, parsed, data) {
			const equipmentId = commandHelper.getParsedValue(parsed, data, "equipment", "evowareId");
			return {
				expansion: [{
					command: "evoware._facts",
					agent: parsed.objectName.agent,
					factsEquipment: equipmentId,
					factsVariable: equipmentId+"_Seal",
					factsValue: parsed.value.program
				}],
				//effects: _.fromPairs([[params.object + ".sealed", true]])
			};
		},
	}
};
