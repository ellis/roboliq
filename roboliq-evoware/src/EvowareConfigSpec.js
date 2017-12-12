/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017 Ellis Whitehead
 * @license GPL-3.0
 */

/**
 * Module for converting an EvowareConfigSpec to a general Roboliq configuration protocol.
 * @module
 */
const _ = require('lodash');
const assert = require('assert');
const math = require('mathjs');
const commandHelper = require('roboliq-processor/dist/commandHelper.js');
const expect = require('roboliq-processor/dist/expect.js');
const evowareEquipment = require('./equipment/evoware.js');
// For validate():
const Validator = require('jsonschema').Validator;
const YAML = require('yamljs');


/**
 * Convert an EvowareConfigSpec into the format of a Roboliq Protocol.
 * This function exists, because it is much simpler to write an
 * EvowareConfigSpec than the equivalent Protocol.
 * @param  {EvowareConfigSpec} spec - specification of an Evoware robot configuration
 * @param  {Object} [data] - (not currently used) protocol data loaded before this configuration
 * @return {Protocol} - an Evoware robot configuration in the format of a Protocol.
 */
function makeProtocol(spec, data = {objects: {}, predicates: []}) {
	// Validate
	const validation = validate(spec);
	if (!_.isEmpty(validation.errors)) {
		return {
			errors: {
				EvowareConfigSpec: validation.errors
			}
		};
	}

	const namespace = [spec.namespace, spec.name].join(".");
	const agent = [spec.namespace, spec.name, "evoware"].join(".");
	const predicates = [];
	const output = { roboliq: "v1", predicates, commandHandlers: evowareEquipment.getCommandHandlers() };

	function getAgentName() {
		return [namespace, "evoware"].join(".");
	}
	function getEquipmentName(equipmentName) {
		assert(equipmentName, "equipmentName is undefined");
		return [namespace, equipmentName].join(".");
	}
	function getSiteName(siteName) {
		assert(siteName, "siteName is undefined");
		return [namespace, "site", siteName].join(".");
	}
	// Return fully qualified model name - first lookup to see whether the
	// model is specific to this robot, and if so, use that name.  Otherwise
	// use the model name for lab, as defined by spec.namespace.
	function getModelName(base) {
		assert(base, "modelName is undefined");
		const model1 = [spec.namespace, spec.name, "model", base];
		const model2 = [spec.namespace, "model", base];
		const isOnlyOnRobot = _.has(output.objects, model1);
		return (isOnlyOnRobot) ? model1.join(".") : model2.join(".");
	}
	function lookupSyringe(base) {
		assert(base, "lookupSyringe: base name must be defined");
		const id1 = [spec.namespace, spec.name, "liha", "syringe", base.toString()];
		const id2 = base.toString();
		const id =
			_.has(output.objects, id1) ? id1 :
			_.has(data.objects, id2) ? id2 :
			undefined;
		assert(!_.isUndefined(id), "syringe not found: "+base);
		return id.join(".");
	}
	function lookupTipModel(base) {
		assert(base, "lookupTipModel: base name must be defined");
		const id1 = [spec.namespace, spec.name, "liha", "tipModel", base];
		const id2 = [spec.namespace, "tipModel", base];
		const id3 = base;
		// console.log({id1, id2, id3, b1: _.has(output.objects, id1), b2: _.has(data.objects, id2), b3: _.has(data.objects, id3)})
		const id =
			_.has(output.objects, id1) ? id1 :
			_.has(data.objects, id2) ? id2 :
			_.has(data.objects, id3) ? id3 :
			undefined;
		assert(!_.isUndefined(id), "tipModel not found: "+base);
		return id.join(".");
	}
	let siteModelCount = 0;
	function addSiteModelCompatibilities(siteModelCompatibilities, output) {
		if (_.isUndefined(output.predicates))
			output.predicates = [];
		// Add predicates for siteModelCompatibilities
		_.forEach(siteModelCompatibilities, compat => {
			siteModelCount++;
			const siteModel = `${namespace}.siteModel${siteModelCount}`;
			output.predicates.push({isSiteModel: {model: siteModel}});
			_.forEach(compat.sites, site => {
				output.predicates.push({siteModel: {site: helpers.getSiteName(site), siteModel}});
			});
			_.forEach(compat.models, labwareModel => {
				output.predicates.push({stackable: {below: siteModel, above: helpers.getModelName(labwareModel)}})
			});
		});
	}

	const helpers = {
		getAgentName,
		getEquipmentName,
		getSiteName,
		getModelName,
		lookupSyringe,
		lookupTipModel,
		addSiteModelCompatibilities,
	};

	output.schemas = evowareEquipment.getSchemas();

	_.set(output, ["roboliq"], "v1");
	_.set(output, ["objects", spec.namespace, "type"], "Namespace");
	_.set(output, ["objects", spec.namespace, "model", "type"], "Namespace");
	_.set(output, ["objects", spec.namespace, spec.name, "type"], "Namespace");
	_.set(output, ["objects", spec.namespace, spec.name, "evoware", "type"], "EvowareRobot");
	_.set(output, ["objects", spec.namespace, spec.name, "evoware", "config"], spec.config);
	_.set(output, ["objects", spec.namespace, spec.name, "site", "type"], "Namespace");
	_.set(output, ["objects", spec.namespace, spec.name, "liha", "type"], "Pipetter");

	// Add 5 timers
	_.forEach(_.range(5), i => {
		const equipment = [spec.namespace, spec.name, `timer${i+1}`].join(".");
		_.set(output.objects, equipment, {
			type: "Timer",
			evowareId: i+1
		});
		output.predicates.push({ "timer.canAgentEquipment": {agent, equipment} });
	});

	// Add bench sites (equipment sites will be added by the equipment modules)
	_.forEach(spec.sites, (value, key) => {
		_.set(output, ["objects", spec.namespace, spec.name, "site", key], _.merge({type: "Site"}, value));
	});

	// Add explicitly defined models to spec.namespace
	_.forEach(spec.models, (value, key) => {
		_.set(output, ["objects", spec.namespace, "model", key], value);
	});

	// Add predicates for siteModelCompatibilities
	addSiteModelCompatibilities(spec.siteModelCompatibilities, output);

	// Lid and plate stacking
	_.forEach(spec.lidStacking, lidsModels => {
		_.forEach(lidsModels.lids, lid => {
			_.forEach(lidsModels.models, model => {
				const below = getModelName(model);
				const above = getModelName(lid);
				predicates.push({stackable: {below, above}});
			});
		});
	});

	handleEquipment(spec, helpers, namespace, agent, output);

	handleRomas(spec, helpers, namespace, agent, output);

	handleLiha(spec, helpers, namespace, agent, output);

	output.objectToPredicateConverters = evowareEquipment.objectToPredicateConverters;

	if (spec.planAlternativeChoosers) {
		output.planAlternativeChoosers = spec.planAlternativeChoosers;
		// console.log({planAlternativeChoosers: output.planAlternativeChoosers})
	}

	// User-defined commandHandlers
	_.forEach(spec.commandHandlers, (fn, key) => {
		output.commandHandlers[key] = fn;
	});

	return output;
}

/**
 * Create the predictates to be added to Roboliq's robot
 * configuration for Evoware's RoMa relationships.
 *
 * Expect specs of this form:
 * ``{<transporter>: {<program>: [site names]}}``
 * @param {Object} spec
 * @param {Roma[]} spec.romas
 */
function handleRomas(spec, helpers, namespace, agent, output) {
	let siteCliqueId = 1;
	_.forEach(spec.romas, (roma, i) => {
		// Add the roma object
		_.set(output, ["objects", spec.namespace, spec.name, `roma${i+1}`], {type: "Transporter", evowareRoma: i});

		const equipment = [spec.namespace, spec.name, `roma${i+1}`].join(".");
		_.forEach(roma.safeVectorCliques, safeVectorClique => {
			const siteClique = `${namespace}.siteClique${siteCliqueId}`;
			siteCliqueId++;

			const program = safeVectorClique.vector;

			// Add the site clique predicates
			_.forEach(safeVectorClique.clique, base => {
				const site = helpers.getSiteName(base);
				output.predicates.push({"siteCliqueSite": {siteClique, site}});
			});

			// Add the transporter predicates
			output.predicates.push({
				"transporter.canAgentEquipmentProgramSites": {
					agent,
					equipment,
					program,
					siteClique
				}
			});
		});
	});
}

function handleLiha(spec, helpers, namespace, agent, output) {
	if (!spec.liha) return;

	const equipment = [spec.namespace, spec.name, "liha"].join(".");

	const tipModelToSyringes = {};

	if (_.isPlainObject(spec.liha.tipModels)) {
		const tipModels = _.mapValues(spec.liha.tipModels, x => _.merge({type: "TipModel"}, x));
		// console.log(tipModels)
		_.set(output.objects, [spec.namespace, spec.name, "liha", "tipModel"], tipModels);
		// console.log({stuff: _.get(output, ["object", spec.namespace, spec.name, "liha", "tipModel"])});
	}

	output.schemas[`pipetter.cleanTips|${agent}|${equipment}`] = {
		description: "Clean the pipetter tips.",
		properties: {
			agent: {description: "Agent identifier", type: "Agent"},
			equipment: {description: "Equipment identifier", type: "Equipment"},
			program: {description: "Program identifier", type: "string"},
			items: {
				description: "List of which syringes to clean at which intensity",
				type: "array",
				items: {
					type: "object",
					properties: {
						syringe: {description: "Syringe identifier", type: "Syringe"},
						intensity: {description: "Intensity of the cleaning", type: "pipetter.CleaningIntensity"}
					},
					required: ["syringe", "intensity"]
				}
			}
		},
		required: ["agent", "equipment", "items"]
	};

	// Add syringes
	_.set(output.objects, [spec.namespace, spec.name, "liha", "syringe"], {});
	_.forEach(spec.liha.syringes, (syringeSpec, i) => {
		// console.log({syringeSpec})
		const syringe = [spec.namespace, spec.name, "liha", "syringe", (i+1).toString()].join(".");
		const syringeObj = {
			type: "Syringe",
			row: i + 1
		};
		// console.log({syringeObj})
		_.set(output.objects, syringe, syringeObj);

		// Handle permanent tips
		if (syringeSpec.tipModelPermanent) {
			const tipModel = helpers.lookupTipModel(syringeSpec.tipModelPermanent);
			syringeObj.tipModel = tipModel;
			syringeObj.tipModelPermanent = tipModel;

			tipModelToSyringes[tipModel] = (tipModelToSyringes[tipModel] || []).concat([syringe]);
		}
		else {
			assert(false, "roboliq-evoware currently only supports fixed tips; please contact the software developer to add support for disposable tips.")
		}
	});

	// Handle tipModelToSyringes mapping for non-permantent tips
	_.forEach(spec.liha.tipModelToSyringes, (syringes0, tipModel0) => {
		const tipModel = helpers.lookupTipModel(syringeSpec.tipModelPermanent);
		const syringes = syringes0.map(helpers.lookupSyringe);
		tipModelToSyringes[tipModel] = (tipModelToSyringes[tipModel] || []).concat(syringes);
	});
	// console.log({tipModelToSyringes})
	_.set(output.objects, [spec.namespace, spec.name, "liha", "tipModelToSyringes"], tipModelToSyringes);

	// Handle washPrograms
	if (spec.liha.washPrograms) {
		const washPrograms = _.merge({type: "Namespace"}, _.mapValues(spec.liha.washPrograms, x => _.merge({type: "EvowareWashProgram"}, x)));
		_.set(output.objects, [spec.namespace, spec.name, "washProgram"], washPrograms);
	}

	// Add system liquid
	const syringeCount = spec.liha.syringes.length;
	assert(syringeCount <= 8, "roboliq-evoware has only been configured to handle 8-syringe LiHas; please contact the software developer to accommodate your needs.");
	_.set(output.objects, [spec.namespace, spec.name, "systemLiquidLabwareModel"], {
		"type": "PlateModel",
		"description": "dummy labware model representing the system liquid source",
		"rows": syringeCount,
		"columns": 1,
		"evowareName": "SystemLiquid"
	});
	_.set(output.objects, [spec.namespace, spec.name, "systemLiquid"], {
		"type": "Liquid",
		"wells": _.map(_.range(syringeCount), i => `${spec.namespace}.${spec.name}.systemLiquidLabware(${String.fromCharCode(65 + i)}01)`)
	});
	_.set(output.objects, [spec.namespace, spec.name, "systemLiquidLabware"], {
		"type": "Plate",
		"description": "dummy labware representing the system liquid source",
		"model": `${namespace}.systemLiquidLabwareModel`,
		"location": helpers.getSiteName("SYSTEM"),
		"contents": ["Infinity l", "systemLiquid"]
	});

	// Equipment predicates
	output.predicates.push({
		"pipetter.canAgentEquipment": {
			agent,
			equipment
		}
	});
	// Syringe predicates
	_.forEach(spec.liha.syringes, (syringeSpec, i) => {
		output.predicates.push({
			"pipetter.canAgentEquipmentSyringe": {
				agent,
				equipment,
				syringe: `ourlab.mario.liha.syringe.${i+1}`
			}
		})
	});
	// Site predicates
	_.forEach(spec.liha.sites, site0 => {
		const site = helpers.getSiteName(site0);
		output.predicates.push({
			"pipetter.canAgentEquipmentSite": {
				agent,
				equipment,
				site
			}
		});
	});

	// Command handler for `pipetter.cleanTips`
	output.commandHandlers[`pipetter.cleanTips|${agent}|${equipment}`] = makeCleanTipsHandler(namespace);
}

function makeCleanTipsHandler(namespace) {
	return function cleanTips(params, parsed, data) {
		//console.log("pipetter.cleanTips|ourlab.mario.evoware|ourlab.mario.liha")
		//console.log(JSON.stringify(parsed, null, '  '))

		const cleaningIntensities = data.schemas["pipetter.CleaningIntensity"].enum;
		const syringeNameToItems = _.map(parsed.value.items, (item, index) => [parsed.objectName[`items.${index}.syringe`], item]);
		//console.log(syringeNameToItems);

		const expansionList = [];
		const sub = function(syringeNames, volume) {
			const syringeNameToItems2 = _.filter(syringeNameToItems, ([syringeName, ]) =>
				_.includes(syringeNames, syringeName)
			);
			//console.log({syringeNameToItems2})
			if (!_.isEmpty(syringeNameToItems2)) {
				const value = _.max(_.map(syringeNameToItems2, ([, item]) => cleaningIntensities.indexOf(item.intensity)));
				if (value >= 0) {
					const intensity = cleaningIntensities[value];
					const syringes = _.map(syringeNameToItems2, ([syringeName, ]) => syringeName);
					expansionList.push({
						command: "pipetter._washTips",
						agent: parsed.objectName.agent,
						equipment: parsed.objectName.equipment,
						program: `${namespace}.washProgram.${intensity}_${volume}`,
						intensity: intensity,
						syringes: syringeNames
					});
				}
			}
		}
		// Get list of syringes on the liha
		const syringesName = `${namespace}.liha.syringe`;
		const syringesObj = _.get(data.objects, syringesName);
		assert(syringesObj, "didn't find LiHa syringes "+syringesName);
		// Lists of [syringeName, tipModelName, programCode]
		const l = _.map(syringesObj, (syringeObj, syringeName0) => {
			const syringeName = `${namespace}.liha.syringe.${syringeName0}`;
			// console.log({syringeObj})
			const tipModelName = syringeObj.tipModel;
			const tipModelObj = _.get(data.objects, tipModelName);
			assert(tipModelObj, "didn't find tipModel "+tipModelName);
			return [syringeName, tipModelName, tipModelObj.programCode];
		});
		// console.log({l})
		// Group by program code, and call `sub()`
		const m = _.groupBy(l, x => x[2]);
		_.forEach(m, (l, programCode) => {
			sub(l.map(x => x[0]), programCode);
		})
		return {expansion: expansionList};
	};
}

function handleEquipment(spec, helpers, namespace, agent, output) {
	_.forEach(spec.equipment, (value, key) => {
		// console.log({key})
		const module = require(__dirname+"/equipment/"+value.module);
		const protocol = module.configure(helpers, key, value.params);
		// console.log(key+": "+JSON.stringify(protocol, null, '\t'))
		// console.log(key+".objects: "+JSON.stringify(protocol.objects, null, '\t'))
		_.merge(output, _.omit(protocol, "predicates"));
		// console.log("output.objects: "+JSON.stringify(output.objects, null, '\t'))
		if (!_.isEmpty(protocol.predicates))
			output.predicates.push(...protocol.predicates);
	});
}

/*
function test() {
	const evowareSpec = require('/Users/ellisw/src/roboliq/config/bsse-mario-new.js');
	const orig = require('/Users/ellisw/src/roboliq/config/bsse-mario.js');

	const protocol = process(evowareSpec);
	// console.log(JSON.stringify(protocol, null, '\t'));
	const diff = require('deep-diff');
	// console.log("isSiteModel predicates: "+JSON.stringify(_.filter(protocol.predicates, x => Object.keys(x)[0] == "isSiteModel")));
	// console.log("siteCliqueSite predicates: "+JSON.stringify(_.filter(protocol.predicates, x => Object.keys(x)[0] == "siteCliqueSite"), null, '\t'));
	protocol.predicates = _.fromPairs(_.sortBy(protocol.predicates.map(x => [JSON.stringify(x), x]), x => x[0]));
	orig.predicates = _.fromPairs(_.sortBy(orig.predicates.map(x => [JSON.stringify(x), x]), x => x[0]));
	const diffs = diff(_.omit(orig, "objectToPredicateConverters"), _.omit(protocol, "objectToPredicateConverters"));
	const diffs2 = _.filter(diffs, d => (
		(d.kind == "E" && d.path[0] == "commandHandlers") ? false
		: (d.kind == "E" && d.path[0] == "planHandlers") ? false
		: true
	));
	console.log(JSON.stringify(diffs2, null, '\t'));
}
*/

/**
 * Validates a EvowareConfigSpec against the JSON schema.
 * @param  {EvowareConfigSpec} evowareSpec - evoware config spec
 * @return {object} - returns the validation results from the npm package `jsonschema`
 */
function validate(evowareSpec) {
	const v = new Validator();

	const schemas = YAML.load(__dirname+"/schemas/EvowareConfigSpec.yaml");
	// console.log(JSON.stringify(schemas, null, '\t'));
	_.forEach(schemas, (schema, name) => {
		const id = "/"+name;
		v.addSchema(_.merge({id}, schema), id);
	});

	// console.log(JSON.stringify(evowareSpec, null, '\t'));
	// console.log(evowareSpec);

	// See: http://json-schema.org/example2.html
	// See: https://spacetelescope.github.io/understanding-json-schema/structuring.html
	// TODO: raise error on unknown type
	// TODO: add some extra types, such as `function`, see
	//  https://www.npmjs.com/package/jsonschema
	//  https://www.npmjs.com/package/jsonschema-extra
	const result = v.validate(evowareSpec, schemas.EvowareConfigSpec);
	// console.log(result);
	return result;
}

module.exports = {
	makeProtocol,
	validate
};
