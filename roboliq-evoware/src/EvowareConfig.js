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
	const output = { predicates };

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

	const evowareConfigSpec = {
		getAgentName,
		getEquipmentName,
		getSiteName,
		getModelName,
		lookupSyringe,
		lookupTipModel,
	};

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
		_.set(output, ["objects", p.namespace, p.name, `timer${i+1}`], {
			type: "Timer",
			evowareId: i+1
		});
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
	_.forEach(p.siteModelCompatibilities, compat => {
		const siteModel = `${namespace}.siteModel${predicates.length + 1}`;
		predicates.push({isSiteModel: {model: siteModel}});
		_.forEach(compat.sites, site => {
			predicates.push({siteModel: {site: evowareConfigSpec.getSiteName(site), siteModel}});
		});
		_.forEach(compat.models, labwareModel => {
			predicates.push({stackable: {below: siteModel, above: evowareConfigSpec.getModelName(labwareModel)}})
		});
	});

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

	// Romas and vectors
	handleRomas(p, evowareConfigSpec, namespace, agent, output);

	handleTipModels(p, output);

	handleLiha(p, evowareConfigSpec, namespace, agent, output);

	handleEquipment(p, evowareConfigSpec, namespace, agent, output);

	output.objectToPredicateConverters = evowareEquipment.objectToPredicateConverters;

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
			const siteClique = `${agent}.siteClique${siteCliqueId}`;
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

	const tipModelToSyringes = {};

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
}

function handleEquipment(p, evowareConfigSpec, namespace, agent, output) {
	_.forEach(p.equipment, (value, key) => {
		console.log({key})
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


const evowareSpec = require('/Users/ellisw/src/roboliq/config/bsse-mario-new.js');
const orig = require('/Users/ellisw/src/roboliq/config/bsse-mario.js');

const protocol = process(evowareSpec);
// console.log(JSON.stringify(protocol, null, '\t'));
const diff = require('deep-diff');
const diffs = diff(orig, protocol);
console.log(JSON.stringify(diffs, null, '\t'));
