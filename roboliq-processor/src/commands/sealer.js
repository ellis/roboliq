import _ from 'lodash';
import yaml from 'yamljs';
import commandHelper from '../commandHelper.js';
import expect from '../expect.js';
import misc from '../misc.js';

const objectToPredicateConverters = {
	"Sealer": function(name, object) {
		return [{ "isSealer": { "equipment": name } }];
	},
};

const commandHandlers = {
	// TODO:
	// - [ ] raise and error if the sealer site is occupied
	// - [ ] raise error if plate's location isn't set
	// - [ ] return result of query for possible alternative settings
	"sealer.sealPlate": function(params, parsed, data) {
		const model = commandHelper.getParsedValue(parsed, data, 'object', 'model');
		const location0 = commandHelper.getParsedValue(parsed, data, 'object', 'location');

		const destinationAfter
			= (parsed.value.destinationAfter === "stay") ? null
			: _.isUndefined(parsed.objectName.destinationAfter) ? location0
			: parsed.objectName.destinationAfter;

		const predicates = [
			{"sealer.canAgentEquipmentProgramModelSite": {
				"agent": parsed.objectName.agent,
				"equipment": parsed.objectName.equipment,
				"program": parsed.objectName.program,
				"model": model,
				"site": parsed.objectName.site
			}}
		];
		const [params2, alternatives] = commandHelper.queryLogic(data, predicates, "sealer.canAgentEquipmentProgramModelSite");
		//console.log("params2:\n"+JSON.stringify(params2, null, '  '))

		const expansion = _.flatten([
			(params2.site === location0) ? null : {
				"command": "transporter.movePlate",
				"object": parsed.objectName.object,
				"destination": params2.site
			},
			// Make `count` copies of the equipment.run-command
			_.range(parsed.value.count).map(() => _.clone({
				command: "equipment.run|"+params2.agent+"|"+params2.equipment,
				agent: params2.agent,
				equipment: params2.equipment,
				program: params2.program,
				object: parsed.objectName.object
			})),
			(destinationAfter === null) ? null : {
				"command": "transporter.movePlate",
				"object": parsed.objectName.object,
				"destination": location0
			},
		]);

		// Update 'sealed' and 'sealPunctures' effects
		const effects = {};
		const sealedId = parsed.objectName.object + ".sealed";
		const sealPuncturesId = parsed.objectName.object + ".sealPunctures";
		if (_.get(data.objects, sealedId) !== true) {
			effects[sealedId] = true;
		}
		if (_.has(data.objects, sealPuncturesId)) {
			effects[sealPuncturesId] = null;
		}

		return { expansion, effects, alternatives };
	}
};

module.exports = {
	roboliq: "v1",
	objectToPredicateConverters,
	schemas: yaml.load(__dirname+"/../schemas/sealer.yaml"),
	commandHandlers
};
