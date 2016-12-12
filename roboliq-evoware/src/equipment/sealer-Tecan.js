import commandHelper from 'roboliq-processor/dist/commandHelper.js';

module.exports = {
	getSchemas: (agentName, equipmentName) => ({
		[`equipment.run|${agentName}|${equipmentName}`]: {
			properties: {
				agent: {description: "Agent identifier", type: "Agent"},
				equipment: {description: "Equipment identifier", type: "Equipment"},
				program: {description: "Program identifier for sealing", type: "string"}
			},
			required: ["agent", "equipment", "program"]
		}
	}),
	getCommandHandlers: (agentName, equipmentName) => ({
		[`equipment.run|${agentName}|${equipmentName}`]: function(params, parsed, data) {
			const equipmentId = commandHelper.getParsedValue(parsed, data, "equipment", "evowareId");
			return {
				expansion: [{
					command: "evoware._facts",
					agent: agentName,
					factsEquipment: equipmentId,
					factsVariable: equipmentId+"_Seal",
					factsValue: parsed.value.program
				}],
				//effects: _.fromPairs([[params.object + ".sealed", true]])
			};
		},
	})
};
