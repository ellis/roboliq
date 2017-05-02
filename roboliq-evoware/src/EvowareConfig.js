const _ = require('lodash');
const assert = require('assert');
const math = require('mathjs');
const commandHelper = require('roboliq-processor/dist/commandHelper.js');
const expect = require('roboliq-processor/dist/expect.js');
const evowareEquipment = require('./equipment/evoware.js');



function process(p, data = {objects: {}, predicates: []}) {
	assert(p.namespace, "you must supply a `namespace` property");
	assert(p.name, "you must supply a `name` property");

	const namespace = [p.namespace, p.name].join(".");
	const agent = [p.namespace, p.name, "evoware"].join(".");
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
	// use the model name for lab, as defined by p.namespace.
	function getModelName(base) {
		assert(base, "modelName is undefined");
		const model1 = [p.namespace, p.name, "model", base];
		const model2 = [p.namespace, "model", base];
		const isOnlyOnRobot = _.has(output.objects, model1);
		return (isOnlyOnRobot) ? model1.join(".") : model2.join(".");
	}
	function lookupSyringe(base) {
		assert(base, "lookupSyringe: base name must be defined");
		const id1 = [p.namespace, p.name, "liha", "syringe", base.toString()];
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
		const id1 = [p.namespace, p.name, "liha", "tipModel", base];
		const id2 = [p.namespace, "tipModel", base];
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
				output.predicates.push({siteModel: {site: evowareConfigSpec.getSiteName(site), siteModel}});
			});
			_.forEach(compat.models, labwareModel => {
				output.predicates.push({stackable: {below: siteModel, above: evowareConfigSpec.getModelName(labwareModel)}})
			});
		});
	}

	const evowareConfigSpec = {
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
	_.set(output, ["objects", p.namespace, "type"], "Namespace");
	_.set(output, ["objects", p.namespace, "model", "type"], "Namespace");
	_.set(output, ["objects", p.namespace, p.name, "type"], "Namespace");
	_.set(output, ["objects", p.namespace, p.name, "evoware", "type"], "EvowareRobot");
	_.set(output, ["objects", p.namespace, p.name, "evoware", "config"], p.config);
	_.set(output, ["objects", p.namespace, p.name, "site", "type"], "Namespace");
	_.set(output, ["objects", p.namespace, p.name, "liha", "type"], "Pipetter");

	// Add 5 timers
	_.forEach(_.range(5), i => {
		const equipment = [p.namespace, p.name, `timer${i+1}`].join(".");
		_.set(output.objects, equipment, {
			type: "Timer",
			evowareId: i+1
		});
		output.predicates.push({ "timer.canAgentEquipment": {agent, equipment} });
	});

	// Add bench sites (equipment sites will be added by the equipment modules)
	_.forEach(p.sites, (value, key) => {
		_.set(output, ["objects", p.namespace, p.name, "site", key], _.merge({type: "Site"}, value));
	});

	// Add explicitly defined models to p.namespace
	_.forEach(p.models, (value, key) => {
		_.set(output, ["objects", p.namespace, "model", key], value);
	});

	// Add predicates for siteModelCompatibilities
	addSiteModelCompatibilities(p.siteModelCompatibilities, output);

	// Lid and plate stacking
	_.forEach(p.lidStacking, lidsModels => {
		_.forEach(lidsModels.lids, lid => {
			_.forEach(lidsModels.models, model => {
				const below = getModelName(model);
				const above = getModelName(lid);
				predicates.push({stackable: {below, above}});
			});
		});
	});

	handleEquipment(p, evowareConfigSpec, namespace, agent, output);

	handleRomas(p, evowareConfigSpec, namespace, agent, output);

	handleTipModels(p, output);

	handleLiha(p, evowareConfigSpec, namespace, agent, output);

	output.objectToPredicateConverters = evowareEquipment.objectToPredicateConverters;

	// User-defined commandHandlers
	_.forEach(p.commandHandlers, (fn, key) => {
		output.commandHandlers[key] = fn;
	});

	return output;
}

/**
 * @typedef Roma
 * @type {Object}
 * @property {?string} description - an optional description of this roma
 * @property {!SafeVectorClique[]} safeVectorCliques - list of cliques of sites that the roma can safetly move plates between using a given vector
 */

/**
 * @typedef SafeVectorClique
 * @type {Object}
 * @property {!string} vector - name of the evoware vector
 * @property {!string[]} clique - names of sites that the ROMA can safely move plates between using this vector
 */

/**
 * Create the predictates to be added to Roboliq's robot
 * configuration for Evoware's RoMa relationships.
 *
 * Expect specs of this form:
 * ``{<transporter>: {<program>: [site names]}}``
 * @param {Object} p
 * @param {Roma[]} p.romas
 */
function handleRomas(p, evowareConfigSpec, namespace, agent, output) {
	let siteCliqueId = 1;
	_.forEach(p.romas, (roma, i) => {
		// Add the roma object
		_.set(output, ["objects", p.namespace, p.name, `roma${i+1}`], {type: "Transporter", evowareRoma: i});

		const equipment = [p.namespace, p.name, `roma${i+1}`].join(".");
		_.forEach(roma.safeVectorCliques, safeVectorClique => {
			const siteClique = `${namespace}.siteClique${siteCliqueId}`;
			siteCliqueId++;

			const program = safeVectorClique.vector;

			// Add the site clique predicates
			_.forEach(safeVectorClique.clique, base => {
				const site = evowareConfigSpec.getSiteName(base);
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

/**
 * @typedef TipModel
 * @type {Object}
 * @property {!string} programCode - a string to use for generating the liquid class names for this tip model
 * @property {!string} min - minimum volume (requires volume units, e.g. "3ul")
 * @property {!string} max - maximum volume (requires volume units, e.g. "950ul")
 * @property {!boolean} canHandleSeel - true if this tip can be used with sealed plates
 * @property {!boolean} canHandleCells - true if this tip can handle cells
 */

/**
 * Handle the `tipModels` spec
 * @param {Object} p
 * @param {TipModel[]} p.tipModels
 */
function handleTipModels(p, output) {
	if (_.isPlainObject(p.tipModels)) {
		const tipModels = _.mapValues(p.tipModels, x => _.merge({type: "TipModel"}, x));
		// console.log(tipModels)
		_.set(output.objects, [p.namespace, p.name, "liha", "tipModel"], tipModels);
		// console.log({stuff: _.get(output, ["object", p.namespace, p.name, "liha", "tipModel"])});
	}
}

/**
 * @typedef Liha
 * @type {Object}
 * @property {Syringe[]} syringes
 * @property {Object.<string, WashProgram>} washPrograms
 */

/**
 * @typedef Syringe
 * @type {Object}
 * @property {string} tipModelPermanent - if the syringe has a fixed tip, then the tip model name should be specified here
 */

/**
 * @typedef WashProgram
 * @type {Object}
 * See EvowareWashProgram
 */

function handleLiha(p, evowareConfigSpec, namespace, agent, output) {
	if (!p.liha) return;

	const equipment = [p.namespace, p.name, "liha"].join(".");

	const tipModelToSyringes = {};

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
	_.set(output.objects, [p.namespace, p.name, "liha", "syringe"], {});
	_.forEach(p.liha.syringes, (syringeSpec, i) => {
		// console.log({syringeSpec})
		const syringe = [p.namespace, p.name, "liha", "syringe", (i+1).toString()].join(".");
		const syringeObj = {
			type: "Syringe",
			row: i + 1
		};
		// console.log({syringeObj})
		_.set(output.objects, syringe, syringeObj);

		// Handle permanent tips
		if (syringeSpec.tipModelPermanent) {
			const tipModel = evowareConfigSpec.lookupTipModel(syringeSpec.tipModelPermanent);
			syringeObj.tipModel = tipModel;
			syringeObj.tipModelPermanent = tipModel;

			tipModelToSyringes[tipModel] = (tipModelToSyringes[tipModel] || []).concat([syringe]);
		}
		else {
			assert(false, "roboliq-evoware currently only supports fixed tips; please contact the software developer to add support for disposable tips.")
		}
	});

	// Handle tipModelToSyringes mapping for non-permantent tips
	_.forEach(p.liha.tipModelToSyringes, (syringes0, tipModel0) => {
		const tipModel = evowareConfigSpec.lookupTipModel(syringeSpec.tipModelPermanent);
		const syringes = syringes0.map(evowareConfigSpec.lookupSyringe);
		tipModelToSyringes[tipModel] = (tipModelToSyringes[tipModel] || []).concat(syringes);
	});
	// console.log({tipModelToSyringes})
	_.set(output.objects, [p.namespace, p.name, "liha", "tipModelToSyringes"], tipModelToSyringes);

	// Handle washPrograms
	if (p.liha.washPrograms) {
		const washPrograms = _.merge({type: "Namespace"}, _.mapValues(p.liha.washPrograms, x => _.merge({type: "EvowareWashProgram"}, x)));
		_.set(output.objects, [p.namespace, p.name, "washProgram"], washPrograms);
	}

	// Add system liquid
	const syringeCount = p.liha.syringes.length;
	assert(syringeCount <= 8, "roboliq-evoware has only been configured to handle 8-syringe LiHas; please contact the software developer to accommodate your needs.");
	_.set(output.objects, [p.namespace, p.name, "systemLiquidLabwareModel"], {
		"type": "PlateModel",
		"description": "dummy labware model representing the system liquid source",
		"rows": syringeCount,
		"columns": 1,
		"evowareName": "SystemLiquid"
	});
	_.set(output.objects, [p.namespace, p.name, "systemLiquid"], {
		"type": "Liquid",
		"wells": _.map(_.range(syringeCount), i => `${p.namespace}.${p.name}.systemLiquidLabware(${String.fromCharCode(65 + i)}01)`)
	});
	_.set(output.objects, [p.namespace, p.name, "systemLiquidLabware"], {
		"type": "Plate",
		"description": "dummy labware representing the system liquid source",
		"model": `${namespace}.systemLiquidLabwareModel`,
		"location": evowareConfigSpec.getSiteName("SYSTEM"),
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
	_.forEach(p.liha.syringes, (syringeSpec, i) => {
		output.predicates.push({
			"pipetter.canAgentEquipmentSyringe": {
				agent,
				equipment,
				syringe: `ourlab.mario.liha.syringe.${i+1}`
			}
		})
	});
	// Site predicates
	_.forEach(p.liha.sites, site0 => {
		const site = evowareConfigSpec.getSiteName(site0);
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

function handleEquipment(p, evowareConfigSpec, namespace, agent, output) {
	_.forEach(p.equipment, (value, key) => {
		// console.log({key})
		const module = require(__dirname+"/equipment/"+value.module);
		const protocol = module.configure(evowareConfigSpec, key, value.params);
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

module.exports = {
	process
};
