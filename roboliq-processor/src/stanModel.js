/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017, ETH Zurich, Ellis Whitehead
 * @license GPL-3.0
 */

/**
 * Function to generate an MCMC model for use with [Stan](http://mc-stan.org/).
 * @module stanModel
 */

const _ = require('lodash');
const assert = require('assert');
const fs = require('fs');
const wellsParser = require('./parsers/wellsParser');

// PROBLEM WITH beta:
// Will need to have a beta for each explicitly tested volume,
// but also need to calculate the bias for other volumes
// (e.g. we might add 3ul dye and 297ul water to a well, but 297ul isn't one of our test volumes).
// We might need to have several lines for assigning to RV_VTIPASP:
//
// - RV_TIPASP[i1] = d * (1 + beta[pd]) + RV_TIPASP_raw * (sigma_v0 + d * sigma_v1[psub])
// - RV_TIPASP[i2] = d + RV_TIPASP_raw * (sigma_v0 + d * sigma_v1[psub])
//
// The first line is for the nodal volumes we want beta estimate for.
// The second line is for the volumes where we completely ignore beta.
// A third line could be for interpolating volumes between two nodal volumes.
// A fourth line could be for extrapolating beta to volumes beyond our nodal volumes.

function Ref(name, i) {
	if (_.isPlainObject(name)) {
		const rv = name;
		return { name: rv.type, i: rv.i, idx: rv.idx };
	}
	return { name, i, idx: (_.isNumber(i)) ? i + 1 : undefined };
}
function RefRV(i) {
	return { name: "rvs", i, idx: (_.isNumber(i)) ? i + 1 : undefined };
}
function lookup(model, ref) {
	return model[ref.name][ref.i];
}

// Standard Normal variate using Box-Muller transform.
function randn_bm(mean, sigma) {
	var u = 1 - Math.random(); // Subtraction to flip [0, 1) to (0, 1].
	var v = 1 - Math.random();
	return mean + sigma * Math.sqrt( -2.0 * Math.log( u ) ) * Math.cos( 2.0 * Math.PI * v );
}

/**
 * Create the initial empty model.
 * You will add pipetting and measurement actions will be added to.
 * @param  {number[]} subclassNodes - sorted volumes after which a subclass starts (e.g. for subclasses 3-15,15.01-500,500.01-1000, subclassNodes = [3,15,500,1000])
 * @param {number[]} betaDs - volumes for which we want a beta parameter (dispense bias)
 * @param {number[]} gammaDs - volumes for which we want a gamma parameter (unintended dilution)
 * @return {object} an object with mostly empty properties representing the model's random variables and labware.
 */
function createEmptyModel(subclassNodes, betaDs, gammaDs) {
	assert(!_.isEmpty(subclassNodes));
	assert(!_.isEmpty(betaDs));
	return {
		subclassNodes,
		betaDs,
		gammaDs,
		models: {}, // labware models
		liquids: {},
		labwares: {},
		wells: {},
		tips: {},
		// liquidClass/pipettingParameters (p) + dispense volume (d) combinations
		pds: {},
		// liquidClass/pipettingParameters (p) + subclass (sub) combinations
		psubs: {},
		gammas: {},
		// Random variables
		RV_AL: [],
		RV_A0: [],
		RV_AV: [],
		RV_VTIPASP: [],
		RV_V: [],
		RV_U: [],
		RV_C: [],
		RV_A: [],
		RV_G0: [], // empty/starting weights
		RV_G: [], // weight
		// Pipetting operations
		pipOps: [],
		absorbanceMeasurements: [],
		weightMeasurements: [],
	};
}

function addRv(model, rv) {
	rv.i = model.rvs.length;
	rv.idx = model.rvs.length; // FIXME: change this to i + 1
	model.rvs.push(rv);
	return RefRV(rv.i);
}

function addRv2(model, group, rv) {
	const list = model[group];
	rv.i = list.length;
	rv.idx = list.length + 1;
	list.push(rv);
	return Ref(group, rv.i);
}

/**
 * Add a liquid to the model.
 * @param {object} model - the model
 * @param {string} k - liquid name
 * @param {object} spec - concentration specification
 * @param {string} [spec.type="fixed"] - set to "fixed" for a concentration, "normal" for a normally distributed concentration.  If set to "normal", you will need to supply the "loc" and "scale" values as input data when running Stan.
 * @param {number} [spec.value=0] - if fixed, this is the fixed concentration - otherwise the parameters will be estimated.
 */
function addLiquid(model, k, spec) {
	const liquidData = getLiquidData(model, k);
	liquidData.spec = spec;
}

function getLabwareData(model, l) {
	// console.log("getLabwareData: "+l);
	const m = "FIXME";
	if (!model.labwares.hasOwnProperty(l)) {
		model.labwares[l] = { m };
	}
	return model.labwares[l];
}

function getLiquidData(model, k) {
	if (!model.liquids.hasOwnProperty(k)) {
		model.liquids[k] = {k, idx: model.liquids.length};
	}
	return model.liquids[k];
}

function getWellData(model, well) {
	if (!model.wells.hasOwnProperty(well)) {
		const {labware: l, wellId: wellPos} = wellsParser.parseOne(well);
		model.wells[well] = { l, pos: wellPos };
	}
	return model.wells[well];
}

function getTipData(model, t) {
	if (!model.tips.hasOwnProperty(t)) {
		model.tips[t] = { };
	}
	return model.tips[t];
}

function getRv_al(model, l) {
	const labwareData = getLabwareData(model, l);
	if (!labwareData.hasOwnProperty("ref_al")) {
		const ref = addRv2(model, "RV_AL", {type: "RV_AL", l});
		labwareData.ref_al = ref;
	}
	return lookup(model, labwareData.ref_al);
}

function getRv_a0(model, well) {
	const wellData = getWellData(model, well);
	if (!wellData.hasOwnProperty("ref_a0")) {
		const rv = {type: "RV_A0", well};
		const ref = addRv2(model, "RV_A0", rv);
		wellData.ref_a0 = ref;
	}
	return lookup(model, wellData.ref_a0);
}

function getRv_av(model, well) {
	const wellData = getWellData(model, well);
	if (!wellData.hasOwnProperty("ref_av")) {
		const rv = {type: "RV_AV", well};
		const ref = addRv2(model, "RV_AV", rv);
		wellData.ref_av = ref;
	}
	return lookup(model, wellData.ref_av);
}

function getRv_g0(model, l) {
	const labwareData = getLabwareData(model, l);
	if (!labwareData.hasOwnProperty("ref_g0")) {
		const rv = {type: "RV_G0", l};
		const ref = addRv2(model, "RV_G0", rv);
		labwareData.ref_g0 = ref;
	}
	return lookup(model, labwareData.ref_g0);
}


function absorbance_A0(model, wells) {
	_.forEach(wells, well => {
		const {labware: l, wellId: wellPos} = wellsParser.parseOne(well);
		const rv_al = getRv_al(model, l);
		const rv_a0 = getRv_a0(model, well);
	});
}

function absorbance_AV(model, wells) {
	_.forEach(wells, well => {
		const {labware: l, wellId: wellPos} = wellsParser.parseOne(well);
		const rv_al = getRv_al(model, l);
		const rv_a0 = getRv_a0(model, well);
		const rv_av = getRv_av(model, well);
	});
}

/**
 * Add an absorbance measurement to the model.
 * @param  {object} model - the model
 * @param  {string[]} wells - the names of the measured wells
 */
function measureAbsorbance(model, wells) {
	_.forEach(wells, well => {
		const {labware: l, wellId: wellPos} = wellsParser.parseOne(well);

		const wellData = getWellData(model, well);
		const rv_al = getRv_al(model, l);
		const rv_a0 = getRv_a0(model, well);
		// console.log("wellData: "+JSON.stringify(wellData))

		// If the well already has an absorbance RV
		if (wellData.ref_a) {
			model.absorbanceMeasurements.push({ref_a: wellData.ref_a, well});
		}
		// If there's some volume in the well
		else if (wellData.ref_vWell) {
			const rv_av = getRv_av(model, well);
			const ref_av = Ref(rv_av);
			// console.log({wellData})
			// If there's some concentration in the well
			if (wellData.ref_cWell) {
				const rv_a = {type: "a", well, l, wellPos, ref_av, ref_vWell: wellData.ref_vWell, ref_cWell: wellData.ref_cWell};
				const ref_a = addRv2(model, "RV_A", rv_a);
				model.absorbanceMeasurements.push({ref_a});
				wellData.ref_a = ref_a;
			}
			// Otherwise the liquid is clear:
			else {
				const rv_a = {type: "a", well, l, wellPos, ref_av};
				const ref_a = addRv2(model, "RV_A", rv_a);
				model.absorbanceMeasurements.push({ref_a});
				wellData.ref_a = ref_a;
			}
		}
		// Otherwise, just measure A0
		else {
			const ref_a0 = Ref(rv_a0);
			const rv_a = {type: "a", well, l, wellPos, ref_a0};
			const ref_a = addRv2(model, "RV_A", rv_a);
			wellData.ref_a = ref_a;
			model.absorbanceMeasurements.push({ref_a});
		}
	});
}

/**
 * Add a weight measurement to the model.
 * @param  {object} model - the model
 * @param  {string} l - the labware name
 */
function measureWeight(model, l) {
	// console.log("measureWeight: "+l)
	const labwareData = getLabwareData(model, l);
	// console.log({labwareData})
	let ref_g;

	// If we already have an RV for the current weight, use it.
	if (labwareData.ref_g) {
		ref_g = labwareData.ref_g;
	}
	// Otherwise, calculate a new RV
	else {
		// Ensure we have a variable for the empty/starting weight of the plate
		const rv_g0 = getRv_g0(model, l);
		const ref_g0 = Ref(rv_g0);

		// Find wells on the plate
		const wells = _.filter(model.wells, wellData => wellData.l == l && wellData.ref_vWell);
		// Get their volumes
		const ref_vs = wells.map(x => x.ref_vWell);

		const rv_g = {type: "RV_G", l, ref_g0, ref_vs};
		// console.log({rv_g})
		ref_g = addRv2(model, "RV_G", rv_g);

		labwareData.ref_g = ref_g;
	}

	// console.log({ref_g})
	model.weightMeasurements.push({ref_g});
}

/**
 * Assign a liquid to a well in the model.
 * @param  {object} model - the model
 * @param  {string} well - the well name
 * @param  {string} k - the liquid name
 */
function assignLiquid(model, well, k) {
	const liquidData = getLiquidData(model, k);
	const wellData = getWellData(model, well);
	wellData.k = k;
	wellData.idx_k = liquidData.idx;
}

/**
 * Add an aspiration operation to the model.
 * @param  {object} model - the model
 * @param  {object} args
 * @param  {string} args.p - the name of the pipetting parameters
 * @param  {number|string} args.t - the tip identifier
 * @param  {number} args.d - volume in microliters
 * @param  {string} args.well - the well name
 */
function aspirate(model, {p, t, d, well}) {
	// create: RV for volume aspirated into tip
	// create: RV for concentration in tip
	// create: RV for new volume in src
	// input: previous volume of src
	// input: variable for k's concentration - we'll need a random variable for the original sources and a calculated variable for what we pipette together

	const wellData = getWellData(model, well);
	const labwareData = getLabwareData(model, wellData.l);
	const tipData = getTipData(model, t);
	const ref_vWell0 = wellData.ref_vWell; // volume of well before aspirating

	// console.log({p, t, d, well, wellData, tipData})

	// Need a variable for the well/liquid concentration.
	// If one doesn't already exist, we'll need to create one.
	// If well's liquid is known:
	//   if it's concentration is fixed,
	//     add a RV_C item, but not an RV_C_raw.
	//   if the concentration is user-defined or estimated,
	//     add both RV_C_raw and RC_C items.
	// Otherwise add an RV_C item with fixed value 0 (and no RV_C_raw).
	if (!wellData.hasOwnProperty("ref_cWell")) {
		let ref_c;
		if (wellData.hasOwnProperty("k")) {
			const liquidData = getLiquidData(model, wellData.k);
			if (_.get(liquidData.spec, "type") === "fixed") {
				if (liquidData.spec.value > 0) {
					const rv_c = {type: "c", k: liquidData.k, value: liquidData.spec.value, of: liquidData.k};
					ref_c = addRv2(model, "RV_C", rv_c);
				}
			}
			else {
				// const rv_c_raw = {type: "c", k, value: liquidData.spec.value};
				// const ref_c_raw = addRv2(model, "RV_C_raw", rv_c_raw);
				const rv_c = {type: "c", k: liquidData.k, alpha_k: `alpha_k_${liquidData.k}`, of: liquidData.k};
				ref_c = addRv2(model, "RV_C", rv_c);
			}
		}
		else {
			const rv_c = {type: "c", value: 0, well, of: well};
			ref_c = addRv2(model, "RV_C", rv_c);
		}
		wellData.ref_cWell = ref_c;
	}
	const ref_cWell0 = wellData.ref_cWell; // concentration of well before aspirating

	// Add new psub (liquid class + subclass) data
	const sub = (model.subclassNodes[0] == d)
		? 1
		: _.findIndex(model.subclassNodes, v => d <= v);
	assert(sub > 0, `didn't find subclass: ${JSON.stringify({sub, d, x: model.subclassNodes})}`);
	const psub = p+sub;
	if (!model.psubs.hasOwnProperty(psub)) {
		const idx = _.size(model.psubs) + 1;
		model.psubs[psub] = {idx, psub, p, sub};
	}
	const idx_psub = model.psubs[psub].idx;

	// Add new pd (liquid class + dispense volume) data, if d is in betaDs
	let idx_pd;
	if (_.includes(model.betaDs, d)) {
		const pd = p+d;
		if (!model.pds.hasOwnProperty(pd)) {
			const idx = _.size(model.pds) + 1;
			model.pds[pd] = {idx, idx_psub, psub, pd, p, sub, d};
		}
		idx_pd = model.pds[pd].idx;
		// console.log({p, d, idx_pd})
	}
	else {
		// console.log({d, betaDs: model.betaDs.join(",")})
	}

	const idx_pip = model.pipOps.length;
	const rv_vTipAsp = {type: "vTipAsp", idx_pip, idx_psub, idx_pd};
	// TODO: this is currently just calculated, so it'd be better not to have a
	// RV_raw entry for this, because that adds a superfluous parameter to the
	// model. We should differentiate between RV's that are calculated and RV's
	// that require their own parameter.
	const ref_vTipAsp = addRv2(model, "RV_VTIPASP", rv_vTipAsp);
	tipData.ref_vTipAsp = ref_vTipAsp;
	tipData.ref_cTipAsp = ref_cWell0;

	// // If we have information about the source concentration,
	// // then track the concentration in the tip too.
	// if (!_.isUndefined(ref_cWell0)) {
	// 	const idx_cTipAsp = model.rvs.length;
	// 	const rv_cTipAsp = {idx: idx_cTipAsp, type: "cTipAsp", ref_cWell0, idx_d};
	// 	model.rvs.push(rv_cTipAsp);
	// 	tipData.ref_cTipAsp = RefRV(idx_cTipAsp);
	// }

	// If we already have information about well volume,
	// then update it by removing the aliquot.
	if (ref_vWell0) {
		const rv_vWellAsp = {type: "vWellAsp", well, ref_vWell0, ref_vTipAsp};
		const ref_vWellAsp = addRv2(model, "RV_V", rv_vWellAsp);
		wellData.ref_vWell = ref_vWellAsp;
	}

	wellData.ref_a = undefined;
	labwareData.ref_g = undefined;


	const asp = {
		d
		// p, t, d, well, k,
		// idx_volTot0, idx_conc0,
		// idx_v, idx_c, idx_volTot,
	};
	model.pipOps.push(asp);
}

/**
 * Add a dispense operation to the model.
 * @param  {object} model - the model
 * @param  {object} args
 * @param  {string} args.p - the name of the pipetting parameters
 * @param  {number|string} args.t - the tip identifier
 * @param  {number} args.d - volume in microliters
 * @param  {string} args.well - the well name
 */
function dispense(model, {p, t, d, well}) {
	if (d === 0) return;

	// input: RV for volume aspirated into tip
	// input: RV for concentration in tip
	// input: previous volume of dst
	// input: previous conc of dst
	// create: RV for new volume of dst
	// create: RV for new conc of dst

	const wellData = getWellData(model, well);
	const labwareData = getLabwareData(model, wellData.l);
	const tipData = getTipData(model, t);
	const ref_vWell0 = wellData.ref_vWell; // volume of well before dispensing
	const ref_cWell0 = wellData.ref_cWell; // concentration of well before dispensing
	const ref_vTipAsp = tipData.ref_vTipAsp; // volume in tip
	const ref_cTipAsp = tipData.ref_cTipAsp; // concentration in tip

	// Unintended dilution
	let ref_uWellDis;
	if (_.includes(model.gammaDs, d)) {
		const pd = p + d;
		if (!model.gammas.hasOwnProperty(pd)) {
			const idx = _.size(model.gammas) + 1;
			model.gammas[pd] = {idx, pd, p, d};
		}
		const idx_gamma = model.gammas[pd].idx;
		const rv_uWellDis = {type: "RV_U", well, idx_gamma};
		ref_uWellDis = addRv2(model, "RV_U", rv_uWellDis);
	}

	// Add aliquot to well
	const rv_vWellDis = {type: "vWellDis", well, ref_vWell0, ref_vTipAsp, ref_uWellDis};
	const ref_vWellDis = addRv2(model, "RV_V", rv_vWellDis);
	tipData.ref_vTipAsp = undefined;
	wellData.ref_vWell = ref_vWellDis;

	// If we have information about the tip concentration,
	// then update the concentration in the destination well too.
	if (ref_cTipAsp || ref_cWell0) {
		const rv_cWellDis = {type: "cWellDis", ref_vWell0, ref_cWell0, ref_vWell1: ref_vWellDis, ref_vTipAsp, ref_cTipAsp, ref_uWellDis, well, of: well};
		const ref_cWellDis = addRv2(model, "RV_C", rv_cWellDis);
		wellData.ref_cWell = ref_cWellDis;
		tipData.ref_cTipAsp = undefined;
	}

	wellData.ref_a = undefined;
	labwareData.ref_g = undefined;

	const asp = {
		d
		// p, t, d, well, k,
		// idx_volTot0, idx_conc0,
		// idx_v, idx_c, idx_volTot,
	};
	model.pipOps.push(asp);
}

/**
 * Print the Stan model to stdout, and save a `basename`.R file that holds indexes to associate the random variables back to labware.
 * @param  {string} model - the model
 * @param  {string} [basename="stanModel"] - the basename for the R output
 */
function printModel(model, basename) {
	const output = {
		data: [],
		transformedData: {
			definitions: [],
			statements: []
		},
		parameters: [],
		transformedParameters: {
			definitions: [],
			statements: []
		},
		model: [],
		R: []
	};
	handle_transformed_data(model, output);
	handle_transformed_parameters(model, output);

	handle_model_psubs(model, output);
	handle_model_pds(model, output);
	handle_model_weightMeasurements(model, output);

	console.log();
	console.log("data {");
	output.data.forEach(s => console.log(s));
	console.log("  real<lower=0> sigma_a_scale;");
	if (model.RV_AL.length > 0) {
		//const NL = model.RV_AL.length;
		const NM = _.size(model.models) || 1;
		console.log(makeVectorOrRealVariable("alpha_l_loc", NM, "<lower=0>"));
		console.log(makeVectorOrRealVariable("alpha_l_scale", NM, "<lower=0>"));
	}
	if (model.RV_AV.length > 0) {
		const NM = _.size(model.models) || 1;
		console.log(makeVectorOrRealVariable("alpha_v_loc", NM, ""));
		console.log(makeVectorOrRealVariable("alpha_v_scale", NM, "<lower=0>"));
		console.log(makeVectorOrRealVariable("sigma_alpha_v_scale", NM, "<lower=0>"));
	}
	_.forEach(model.liquids, liquidData => {
		if (_.get(liquidData.spec, "type") === "normal") {
			console.log(`  real<lower=0> alpha_k_${liquidData.k}_loc; // concentration of liquid ${liquidData.k}`);
			console.log(`  real<lower=0> alpha_k_${liquidData.k}_scale; // concentration of liquid ${liquidData.k}`);
		}
	});
	if (model.absorbanceMeasurements.length > 0) {
		console.log();
		console.log(`  vector<lower=0>[${model.absorbanceMeasurements.length}] A; // Absorbance measurements`);
	}
	console.log("}");

	console.log();
	console.log("transformed data {");
	output.transformedData.definitions.forEach(s => console.log(s));
	console.log();
	output.transformedData.statements.forEach(s => console.log(s));
	console.log("}");

	console.log();
	console.log("parameters {");
	output.parameters.forEach(s => console.log(s));
	if (model.RV_AL.length > 0) {
		console.log("  vector[NM] alpha_l_raw;");
		const NM = 1;
		const times = (NM == 1) ? "*" : ".*";
		output.transformedParameters.definitions.push(`  vector<lower=0>[NM] alpha_l = alpha_l_loc + alpha_l_raw ${times} alpha_l_scale;`);
	}
	if (model.RV_AL.length > 1) {
		console.log("  vector<lower=0,upper=1>[NM] sigma_alpha_l;");
	}
	console.log("  vector<lower=0,upper=1>[NM] sigma_alpha_i;");
	if (model.RV_AV.length > 0) {
		console.log("  vector[NM] alpha_v_raw;");
		console.log("  vector<lower=0>[NM] sigma_alpha_v_raw;");
		const times = (model.RV_AL.length == 1 || (_.size(model.models) || 1) == 1) ? "*" : ".*";
		// console.log({times, NL: model.RV_AL.length, NM: model.models.length})
		output.transformedParameters.definitions.push(`  vector[NM] alpha_v = alpha_v_loc + alpha_v_raw ${times} alpha_v_scale;`);
		output.transformedParameters.definitions.push(`  vector<lower=0>[NM] sigma_alpha_v = sigma_alpha_v_raw ${times} sigma_alpha_v_scale;`);
	}
	console.log();
	if (model.RV_AL.length > 0) console.log(`  vector[${model.RV_AL.length}] RV_AL_raw;`);
	if (model.RV_A0.length > 0) console.log(`  vector[${model.RV_A0.length}] RV_A0_raw;`);
	if (model.RV_AV.length > 0) console.log(`  vector[${model.RV_AV.length}] RV_AV_raw;`);
	if (model.RV_VTIPASP.length > 0) console.log(`  vector[${model.RV_VTIPASP.length}] RV_VTIPASP_raw;`);
	// if (model.RV_C_raw.length > 0) console.log(`  vector[${model.RV_C_raw.length}] RV_C_raw;`)
	// console.log("  vector[NRV] RV_raw;");
	_.forEach(model.liquids, liquidData => {
		if (_.get(liquidData.spec, "type") === "normal") {
			// console.log(`  real<lower=${liquidData.spec.lower || 0}, upper=${liquidData.spec.upper}> alpha_k_${liquidData.k}; // concentration of liquid ${liquidData.k}`);
			console.log(`  real alpha_k_${liquidData.k}_raw; // unscaled concentration variance of liquid ${liquidData.k}`);
			output.transformedParameters.definitions.push(`  real alpha_k_${liquidData.k} = alpha_k_${liquidData.k}_loc + alpha_k_${liquidData.k}_raw * alpha_k_${liquidData.k}_scale;`);
		}
	});
	if (model.absorbanceMeasurements.length > 0) {
		console.log("  real<lower=0> sigma_a_raw;");
	}
	console.log("}");

	console.log();
	console.log("transformed parameters {");
	output.transformedParameters.definitions.forEach(s => console.log(s));
	console.log();
	output.transformedParameters.statements.forEach(s => console.log(s));
	console.log("}");

	console.log();
	console.log("model {");
	output.model.forEach(s => console.log(s));
	if (model.RV_AL.length > 0) console.log("  RV_AL_raw ~ normal(0, 1);");
	if (model.RV_A0.length > 0) console.log("  RV_A0_raw ~ normal(0, 1);");
	if (model.RV_AV.length > 0) console.log("  RV_AV_raw ~ normal(0, 1);");
	if (model.RV_VTIPASP.length > 0) console.log("  RV_VTIPASP_raw ~ normal(0, 1);");
	// if (model.RV_C_raw.length > 0) console.log("  RV_C_raw ~ normal(0, 1);");
	// console.log("  RV_raw ~ normal(0, 1);");
	if (model.absorbanceMeasurements.length > 0) {
		console.log("  sigma_a_raw ~ exponential(1);");
		if (model.RV_AV.length > 0) {
			console.log("  alpha_v_raw ~ normal(0, 1);");
			console.log("  sigma_alpha_v_raw ~ exponential(1);");
		}
		console.log();
		const idxsRv = model.absorbanceMeasurements.map(x => x.ref_a.idx);
		// console.log(`  A ~ normal(RV_A[{${idxsRv}}], RV_A[{${idxsRv}}] * sigma_a);`);
		console.log(`  A ~ normal(RV_A[A_i_A], RV_A[A_i_A] * sigma_a);`);
	}
	console.log("}");

	fs.writeFileSync((basename || "stanModel")+".R", output.R.join("\n")+"\n");
}

// There's appears to be a bug in RStan such that 1-element vectors
// are not passed to stan.  So we need to turn 1-element vectors into reals.
function makeVectorOrRealVariable(name, n, modifier, value) {
	const type = (_.isString(n) || n > 1)
		? `vector${modifier}[${n}]`
		: `real${modifier}`;
	return `  ${type} ${name}${_.isUndefined(value) ? "" : " = "+value};`;
}

/*function makeVectorVariable(name, n, modifier, value) {
	const type = `vector${modifier}[${n}]`;
	return `  ${type} ${name}${_.isUndefined(value) ? "" : " = "+value};`;
}*/

function handle_transformed_data(model, output) {
	output.transformedData.definitions.push(`  int NM = 1; // number of labware models`);
	output.transformedData.definitions.push(`  int NL = ${_.size(model.labwares)}; // number of labwares`);
	output.transformedData.definitions.push(`  int NI = ${_.size(model.wells)}; // number of wells`);
	output.transformedData.definitions.push(`  int NT = ${_.size(model.tips)}; // number of tips`);
	// output.transformedData.definitions.push(`  int NRV = ${_.size(model.rvs)}; // number of latent random variables`);
	output.transformedData.definitions.push(`  int NJ = ${model.pipOps.length}; // number of pipetting operations`);
	_.forEach(model.liquids, liquidData => {
		if (_.get(liquidData.spec, "type") === "fixed") {
			output.transformedData.definitions.push(`  real alpha_k_${liquidData.k} = ${liquidData.spec.value}; // concentration of liquid ${liquidData.k}`);
		}
	});
	if (!_.isEmpty(model.pipOps)) {
		// console.log(`  real d[NJ] = {${model.pipOps.map(x => x.d.toFixed(1))}};`);
		output.transformedData.definitions.push(`  vector<lower=0>[NJ] d; // desired volumes`);
	  output.transformedData.statements.push(`  {`);
	  output.transformedData.statements.push(`    real d0[NJ] = {${model.pipOps.map(x => x.d.toFixed(1))}};`);
	  output.transformedData.statements.push(`    for (i in 1:NJ) d[i] = d0[i];`);
	  output.transformedData.statements.push(`  }`);
	}
}

function addCentralizedParameter_sigma(name, n, nScales, output) {
	assert(n > 0);
	assert(nScales == 1 || nScales == n);

	// Scale
	if (nScales == 1) {
		output.data.push(`  real<lower=0> ${name}_scale;`);
	}
	else if (nScales > 1) {
		output.data.push(`  vector<lower=0>[${nScales}] ${name}_scale;`);
	}

	if (n == 1) {
		output.parameters.push(`  real<lower=0> ${name}_raw;`);
	}
	else if (n > 1) {
		output.parameters.push(`  vector<lower=0>[${n}] ${name}_raw;`);
	}

	if (n == 1) {
		output.transformedParameters.definitions.push(`  real<lower=0> ${name} = ${name}_raw * ${name}_scale;`);
	}
	else if (n > 1) {
		if (nScales == 1) {
			output.transformedParameters.definitions.push(`  vector<lower=0>[${n}] ${name} = ${name}_raw * ${name}_scale;`);
		}
		else {
			output.transformedParameters.definitions.push(`  vector<lower=0>[${n}] ${name} = ${name}_raw .* ${name}_scale;`);
		}
	}

	output.model.push(`  ${name}_raw ~ exponential(1);`);
}

/**
 * Add variables for a centralized mean
 * @param {string} name - name of variable
 * @param {boolean} withLoc - whether to include a location data variable
 * @param {integer} n - array size, or 1 for a scalar
 * @param {integer} nScales - number of scale parameters; 1 = same scale for each item in array; n = one scale parameter for each item.
 * @param {string} [nName] - optional name for array size (to be used in place of n in some places)
 * @param {string} limits - a limits modifier ("" if no limits)
 */
function addCentralizedParameter_mean(name, withLoc, n, nScales, nName, limits, output) {
	assert(n > 0);
	assert(nScales == 1 || nScales == n);

	// Location
	if (withLoc) {
		output.data.push(makeVectorOrRealVariable(`${name}_loc`, nScales, limits));
	}
	// Scale
	output.data.push(makeVectorOrRealVariable(`${name}_scale`, nScales, "<lower=0>"));

	const n2 = (_.isString(nName) && !_.isEmpty(nName)) ? nName : n;
	// Raw
	output.parameters.push(makeVectorOrRealVariable(`${name}_raw`, n2, ""));

	const times = (n == 1 || nScales == 1) ? "*" : ".*";
	const value = `${name}_loc + ${name}_raw ${times} ${name}_scale`;
	output.transformedParameters.definitions.push(makeVectorOrRealVariable(name, n2, limits, value));

	output.model.push(`  ${name}_raw ~ normal(0, 1);`);
}

function handle_model_psubs(model, output) {
	const n = _.size(model.psubs);
	if (n == 0) return;

	addCentralizedParameter_sigma("sigma_v0", 1, 1, output);
	addCentralizedParameter_sigma("sigma_v1", n, 1, output);

	output.R.push("dfModel_psubs = tibble(");
	output.R.push(`  idx = c(${_.map(model.psubs, "idx").join(", ")}),`);
	output.R.push(`  p = c(\"${_.map(model.psubs, "p").join('", "')}\"),`);
	output.R.push(`  sub = c(${_.map(model.psubs, "sub").join(", ")})`);
	output.R.push(")");
}

function handle_model_pds(model, output) {
	const n = _.size(model.pds);
	if (n == 0) return;

	output.transformedData.definitions.push(`  int NBETA = ${n}; // number of liquidClass+majorD combinations for beta`);

	// The second argument shouldn't be 'true', because it doesn't make
	// sense to have a single location variable for all betas.
	// Either none of them should have location, or all of them.
	// The problem with all of them having betas is that it's not so simple
	// for the user to know which order the location variables should be specified
	// in.
	addCentralizedParameter_mean("beta", true, n, 1, "NBETA", "", output);

	output.R.push("dfModel_pds = tibble(");
	output.R.push(`  idx = c(${_.map(model.pds, "idx").join(", ")}),`);
	output.R.push(`  idx_psub = c(${_.map(model.pds, "idx_psub").join(", ")}),`);
	output.R.push(`  p = c(\"${_.map(model.pds, "p").join('", "')}\"),`);
	output.R.push(`  sub = c(${_.map(model.pds, "sub").join(", ")}),`);
	output.R.push(`  d = c(${_.map(model.pds, "d").join(", ")})`);
	output.R.push(")");
}

function handle_model_weightMeasurements(model, output) {
	const n = model.weightMeasurements.length;
	if (n == 0) return;

	output.data.push(`  vector<lower=0>[${n}] G; // weight measurements`);

	addCentralizedParameter_sigma("sigma_g", 1, 1, output);

	output.data.push(`  real<lower=0> rho; // density of liquid`);

	output.parameters.push(`  vector<lower=0>[${model.RV_G0.length}] RV_G0; // empty/starting weights`);

	output.transformedParameters.definitions.push(`  vector<lower=0>[${n}] RV_G; // empty/starting weights`);
	_.forEach(model.RV_G, rv_g => {
		const idx_g0 = rv_g.ref_g0.idx;
		const idxs_v = rv_g.ref_vs.map(x => x.idx);
		let s = `  RV_G[${rv_g.idx}] = RV_G0[${idx_g0}]`;
		if (!_.isEmpty(rv_g.ref_vs))
			s = s + ` + rho * sum(RV_V[{${idxs_v}}])`;
		s += `; // weight of ${rv_g.l}`;
		output.transformedParameters.statements.push(s);
	});

	const idxs_g = model.weightMeasurements.map(x => x.ref_g.idx);
	output.model.push(`  G ~ normal(RV_G[{${idxs_g}}], sigma_g);`);
}

function handle_transformed_parameters(model, output) {
	/*
	if (!_.isEmpty(model.betas)) {
		output.transformedParameters.definitions.push("");
		output.transformedParameters.definitions.push("  vector[NBETA] beta0 = beta0_raw * beta_scale;");
		output.transformedParameters.definitions.push("  vector[NBETA] beta1 = beta1_raw * beta_scale;");
		output.transformedParameters.definitions.push("  vector<lower=0>[NBETA] sigma_v = sigma_v_raw * sigma_v_scale;");

		const rvs_gamma = _.values(model.betas).filter(rv => rv.withGamma);
		if (!_.isEmpty(rvs_gamma)) {
			output.transformedData.definitions.push(`  int<lower=0> NGAMMA = ${rvs_gamma.length};`);
			output.transformedData.definitions.push(`  int<lower=1> gamma_i_raw[NGAMMA] = ${rvs_gamma.map(rv => rv.idx)};`);
			output.transformedParameters.definitions.push("  vector<lower=0>[NBETA] gamma = 0;"); // need the same number of gamma variables as betas, but we probably have fewer gamma_raw parameters, since it's difficult to identify gamma in many experiments with small volumes.
			output.transformedParameters.definitions.push("  real sigma_gamma = sigma_gamma_raw * sigma_gamma_scale;");

			output.transformedParameters.statements.push("");

			output.transformedParameters.statements.push("  for (i in 1:NGAMMA) gamma[gamma_i_raw[i]] = max({0, gamma_raw[i] * sigma_gamma});");
		}
	}
	*/
	output.transformedParameters.definitions.push("  real<lower=0> sigma_a = sigma_a_raw * sigma_a_scale;");
	// output.transformedParameters.definitions.push(`  vector[NRV] RV;`);
	handle_transformed_parameters_RV_al(model, output);
	handle_transformed_parameters_RV_a0(model, output);
	handle_transformed_parameters_RV_av(model, output);
	handle_transformed_parameters_RV_vTipAsp(model, output);
	handle_transformed_parameters_RV_U(model, output);
	handle_transformed_parameters_RV_V(model, output);
	handle_transformed_parameters_RV_C(model, output);
	handle_transformed_parameters_RV_A(model, output);
}
function handle_transformed_parameters_RV_al(model, output) {
	const rvs = model.RV_AL;
	if (rvs.length == 0) return;

	const idxs_m = Array(rvs.length);
	for (let i = 0; i < rvs.length; i++) {
		const rv = rvs[i];
		idxs_m[i] = 0 + 1;
	}
	output.transformedParameters.definitions.push(`  vector<lower=0>[${rvs.length}] RV_AL; // average absorbance of labware`);
	output.transformedParameters.statements.push("");
	output.transformedParameters.statements.push("  // AL[l] ~ normal(alpha_l[m], sigmal_alpha_l[m])")
	if (model.RV_AL.length > 1) {
		output.transformedParameters.statements.push(`  RV_AL = alpha_l[{${idxs_m}}] + RV_AL_raw .* sigma_alpha_l[{${idxs_m}}];`);
	}
	else {
		output.transformedParameters.statements.push(`  RV_AL = alpha_l[{${idxs_m}}];`);
	}
}
function handle_transformed_parameters_RV_a0(model, output) {
	const rvs = model.RV_A0;
	const idxs_al = Array(rvs.length);
	const idxs_m = Array(rvs.length);
	for (let i = 0; i < rvs.length; i++) {
		const rv = rvs[i];
		const wellData = model.wells[rv.well];
		const labwareData = model.labwares[wellData.l];
		idxs_al[i] = labwareData.ref_al.idx;
		idxs_m[i] = 0 + 1;
	}

	output.transformedData.definitions.push(`  int<lower=1> RV_A0_i_AL[${idxs_al.length}] = {${idxs_al}};`)
	output.transformedData.definitions.push(`  int<lower=1> RV_A0_i_m[${idxs_m.length}] = {${idxs_m}};`)
	output.transformedParameters.definitions.push(`  vector<lower=0>[${rvs.length}] RV_A0; // absorbance of empty wells`);
	output.transformedParameters.statements.push("");
	output.transformedParameters.statements.push("  // A0[i] ~ normal(AL[m[i]], sigma_alpha_i[m[i]])");
	output.transformedParameters.statements.push(`  RV_A0 = RV_AL[RV_A0_i_AL] + RV_A0_raw .* sigma_alpha_i[RV_A0_i_m];`);
}
function handle_transformed_parameters_RV_av(model, output) {
	const rvs = model.RV_AV;
	if (rvs.length == 0) return;

	const idxs_a0 = Array(rvs.length);
	// const idxs_l = Array(rvs.length);
	const idxs_m = Array(rvs.length);
	for (let i = 0; i < rvs.length; i++) {
		const rv = rvs[i];
		const wellData = model.wells[rv.well];
		idxs_a0[i] = wellData.ref_a0.idx;
		// const labwareData = model.labwares[wellData.l];
		// // console.log({rv, wellData, labwareData})
		// idxs_l[i] = labwareData.idx_al;
		idxs_m[i] = 0 + 1;
	}

	output.transformedData.definitions.push(`  int<lower=1> RV_AV_i_A0[${idxs_a0.length}] = {${idxs_a0}};`)
	output.transformedData.definitions.push(`  int<lower=1> RV_AV_i_m[${idxs_m.length}] = {${idxs_m}};`)
	// There's a bug in Rstan for data vectors of length 1, so this check is necessary...
	// const NM = model.models.length || 1;
	// CONTINUE
	// if (NM == 1) {
	// 	output.transformedParameters.definitions.push(`  vector[NM] alpha_v = alpha_v_loc + alpha_v_raw * alpha_v_scale;`);
	// 	output.transformedParameters.definitions.push(`  vector<lower=0>[NM] sigma_alpha_v = sigma_alpha_v_raw * sigma_alpha_v_scale;`);
	// }
	// else if (NM > 1) {
	// 	output.transformedParameters.definitions.push(`  vector[NM] alpha_v = alpha_v_loc + alpha_v_raw .* alpha_v_scale;`);
	// 	output.transformedParameters.definitions.push(`  vector<lower=0>[NM] sigma_alpha_v = sigma_alpha_v_raw .* sigma_alpha_v_scale;`);
	// }
	output.transformedParameters.definitions.push(`  vector<lower=0>[${rvs.length}] RV_AV; // absorbance of water-filled wells`);
	output.transformedParameters.statements.push("");
	output.transformedParameters.statements.push("  // AV[i] ~ normal(A0[i] + alpha_v[m[i]], sigma_alpha_v[m[i]])");
	output.transformedParameters.statements.push(`  RV_AV = RV_A0[RV_AV_i_A0] + alpha_v[RV_AV_i_m] + RV_AV_raw .* sigma_alpha_v[RV_AV_i_m];`);
}
function handle_transformed_parameters_RV_vTipAsp(model, output) {
	const rvs = model.RV_VTIPASP;
	if (rvs.length == 0) return;

	const idxs_pip = rvs.map(rv => rv.idx_pip + 1);
	const idxs_psubsId = rvs.map(rv => rv.idx_psub);
	const rvs_withBeta = rvs.filter(rv => _.isNumber(rv.idx_pd));
	const idxs_withBeta = rvs_withBeta.map(rv => rv.idx);
	const idxs_withBeta_beta = rvs_withBeta.map(rv => rv.idx_pd);

	output.transformedParameters.definitions.push(`  vector<lower=0>[${rvs.length}] RV_VTIPASP; // volume aspirated into tip`);
	output.transformedParameters.statements.push("");

	output.transformedData.definitions.push(`  int<lower=1> RV_VTIPASP_i_d[${idxs_pip.length}] = {${idxs_pip}};`);
	if (_.size(model.psubs) > 1) {
		output.transformedData.definitions.push(`  int<lower=1> RV_VTIPASP_i_psub[${idxs_pip.length}] = {${idxs_psubsId}};`);
	}
	output.transformedData.definitions.push(`  int<lower=1> RV_VTIPASP_i_withBeta[${idxs_withBeta.length}] = {${idxs_withBeta}};`);
	output.transformedData.definitions.push(`  int<lower=1> RV_VTIPASP_i_withBeta_beta[${idxs_withBeta_beta.length}] = {${idxs_withBeta_beta}};`);

	output.transformedParameters.statements.push(`  {`);
	output.transformedParameters.statements.push(`    // - RV_TIPASP[i1] = d * (1 + beta[pd]) + RV_TIPASP_raw * (sigma_v0 + d * sigma_v1[psub])`);
	output.transformedParameters.statements.push(`    // - RV_TIPASP[i2] = d + RV_TIPASP_raw * (sigma_v0 + d * sigma_v1[psub])`);
	output.transformedParameters.statements.push(`    vector[${model.RV_VTIPASP.length}] temp = d[RV_VTIPASP_i_d];`);
	output.transformedParameters.statements.push(`    temp[RV_VTIPASP_i_withBeta] = temp[RV_VTIPASP_i_withBeta] .* (1 + beta[RV_VTIPASP_i_withBeta_beta]);`);
	const times = (_.size(model.psubs) > 1) ? ".*" : "*";
	output.transformedParameters.statements.push(`    RV_VTIPASP = temp + RV_VTIPASP_raw .* (sigma_v0 + temp ${times} sigma_v1${(_.size(model.psubs) > 1) ? "[RV_VTIPASP_i_psub]" : ""}); // volume aspirated into tip`);
	output.transformedParameters.statements.push(`  }`);
}
function handle_transformed_parameters_RV_U(model, output) {
	const rvs = model.RV_U;
	if (rvs.length == 0) return;

	const n = _.size(model.gammas);
	output.transformedData.definitions.push(`  int NGAMMA = ${n}; // Number of gamma parameters`);

	addCentralizedParameter_mean("gamma", true, n, 1, "NGAMMA", "<lower=0>", output);
	addCentralizedParameter_sigma("cv_gamma", 1, 1, output);

	output.parameters.push(`  vector<lower=0>[${rvs.length}] RV_U_raw; // unintended dilution due to extra water dispense volumes`);

	output.transformedParameters.definitions.push(`  vector<lower=0>[${rvs.length}] RV_U = gamma[{${rvs.map(rv => rv.idx_gamma)}}] .* (1 + RV_U_raw * cv_gamma); // unintended dilution due to extra water dispense volumes`)

	output.model.push(`  RV_U_raw ~ normal(0, 1);`);
}
function handle_transformed_parameters_RV_V(model, output) {
	const rvs = model.RV_V;
	if (rvs.length == 0) return;

	output.transformedParameters.definitions.push(`  vector<lower=0>[${rvs.length}] RV_V; // concentrations`);
	output.transformedParameters.statements.push("");

	_.forEach(rvs, (rv, i) => {
		if (rv.type === "vWellAsp") {
			// if (rv.ref_vWell0) {
				// VolTot_t[j] = sum of volumes
				output.transformedParameters.statements.push(`  RV_V[${rv.idx}] = RV_V[${rv.ref_vWell0.idx}] - RV_VTIPASP[${rv.ref_vTipAsp.idx}]; // volume in ${rv.well} after aspirating`);
			// }
			// else {
			// 	// VolTot_t[j] = sum of volumes
			// 	output.transformedParameters.statements.push(`  RV[${idx + 1}] = -RV_VTIPASP[${rv.ref_vTipAsp.idx}]; // volume aspirated from well`);
			// }
		}
		else if (rv.type == "vWellDis") {
			// Summands for well volume
			const l = [];
			if (rv.ref_vWell0) {
				l.push(`RV_V[${rv.ref_vWell0.idx}]`);
			}
			l.push(`RV_VTIPASP[${rv.ref_vTipAsp.idx}]`);
			if (rv.ref_uWellDis) {
				l.push(`RV_U[${rv.ref_uWellDis.idx}]`);
			}
			// Add volume to well as sum of volumes
			output.transformedParameters.statements.push(`  RV_V[${rv.idx}] = ${l.join(" + ")}; // volume in ${rv.well} after dispensing`);
		}
	});
}
function handle_transformed_parameters_RV_C(model, output) {
	const rvs = model.RV_C;
	if (rvs.length == 0) return;

	output.transformedParameters.definitions.push(`  vector<lower=0>[${rvs.length}] RV_C; // concentrations`);
	output.transformedParameters.statements.push("");
	_.forEach(rvs, (rv, i) => {
		if (rv.type === "c") {
			if (rv.hasOwnProperty("value")) {
				output.transformedParameters.statements.push(`  RV_C[${i+1}] = ${rv.value}; // concentration of ${rv.of}`);
			}
			else if (rv.hasOwnProperty("alpha_k")) {
				output.transformedParameters.statements.push(`  RV_C[${i+1}] = ${rv.alpha_k}; // concentration of ${rv.of}`);
			}
		}
		else if (rv.type == "cWellDis") {
			// console.log({rv})
			// C_t[j] = (c[i,j-1] * v[i,j-1] + cTip * vTip) / (v[i,j-1] + vTip)
			if (rv.ref_cWell0 && rv.ref_cTipAsp) {
				const cWell0 = `RV_C[${rv.ref_cWell0.idx}]`;
				output.transformedParameters.statements.push(`  RV_C[${rv.idx}] = (${cWell0} * RV_V[${rv.ref_vWell0.idx}] + RV_C[${rv.ref_cTipAsp.idx}] * RV_VTIPASP[${rv.ref_vTipAsp.idx}]) / RV_V[${rv.ref_vWell1.idx}]; // concentration of ${rv.of}`);
			}
			else if (rv.ref_cWell0) {
				const cWell0 = `RV_C[${rv.ref_cWell0.idx}]`;
				output.transformedParameters.statements.push(`  RV_C[${rv.idx}] = (${cWell0} * RV_V[${rv.ref_vWell0.idx}]) / RV_V[${rv.ref_vWell1.idx}]; // concentration of ${rv.of}`);
			}
			else if (rv.ref_uWellDis) {
				output.transformedParameters.statements.push(`  RV_C[${rv.idx}] = (RV_C[${rv.ref_cTipAsp.idx}] * RV_VTIPASP[${rv.ref_vTipAsp.idx}]) / RV_V[${rv.ref_vWell1.idx}]; // concentration of ${rv.of}`);
			}
			else {
				output.transformedParameters.statements.push(`  RV_C[${rv.idx}] = RV_C[${rv.ref_cTipAsp.idx}]; // concentration of ${rv.of}`);
			}
		}
	});
}
function handle_transformed_parameters_RV_A(model, output) {
	const rvs = model.RV_A;
	if (rvs.length == 0) return;

	output.transformedParameters.definitions.push(`  vector<lower=0>[${rvs.length}] RV_A; // absorbance measurements`);
	output.transformedParameters.statements.push("");

	// A ~ normal(Av + vWell * cWell, (Av + vWell * cWell) * sigma_a)

	// A0 readouts
	const rvs_A0 = rvs.filter(rv => rv.ref_a0);
	if (rvs_A0.length > 0) {
		const idxs = rvs_A0.map(rv => rv.idx);
		const idxs_A0 = rvs_A0.map(rv => rv.ref_a0.idx);
		output.transformedData.definitions.push(`  int<lower=1> RV_A_i1[${idxs.length}] = {${idxs}};`)
		output.transformedData.definitions.push(`  int<lower=1> RV_A_i1_A0[${idxs_A0.length}] = {${idxs_A0}};`)
		output.transformedParameters.statements.push(`  RV_A[RV_A_i1] = RV_A0[RV_A_i1_A0]; // absorbance of empty wells`);
	}

	// AV readouts
	const rvs_AV = rvs.filter(rv => rv.ref_av && !rv.ref_cWell);
	if (rvs_AV.length > 0) {
		const idxs = rvs_AV.map(rv => rv.idx);
		const idxs_AV = rvs_AV.map(rv => rv.ref_av.idx);
		output.transformedData.definitions.push(`  int<lower=1> RV_A_i2[${idxs.length}] = {${idxs}};`)
		output.transformedData.definitions.push(`  int<lower=1> RV_A_i2_AV[${idxs_AV.length}] = {${idxs_AV}};`)
		output.transformedParameters.statements.push(`  RV_A[RV_A_i2] = RV_AV[RV_A_i2_AV]; // absorbance of water-filled wells`);
	}

	// A readouts
	const rvs_A = rvs.filter(rv => rv.ref_cWell);
	if (rvs_A.length > 0) {
		const idxs = rvs_A.map(rv => rv.idx);
		const idxs_AV = rvs_A.map(rv => rv.ref_av.idx);
		const idxs_V = rvs_A.map(rv => rv.ref_vWell.idx);
		const idxs_C = rvs_A.map(rv => rv.ref_cWell.idx);
		output.transformedData.definitions.push(`  int<lower=1> RV_A_i3[${idxs.length}] = {${idxs}};`)
		output.transformedData.definitions.push(`  int<lower=1> RV_A_i3_AV[${idxs_AV.length}] = {${idxs_AV}};`)
		output.transformedData.definitions.push(`  int<lower=1> RV_A_i3_V[${idxs_V.length}] = {${idxs_V}};`)
		output.transformedData.definitions.push(`  int<lower=1> RV_A_i3_C[${idxs_C.length}] = {${idxs_C}};`)
		output.transformedParameters.statements.push(`  RV_A[RV_A_i3] = RV_AV[RV_A_i3_AV] + RV_V[RV_A_i3_V] .* RV_C[RV_A_i3_C]; // absorbance of wells with dye`);
	}

	// Indexes for A ~ normal(...) model
	if (model.absorbanceMeasurements.length > 0) {
		const idxs_A = model.absorbanceMeasurements.map(x => x.ref_a.idx);
		output.transformedData.definitions.push(`  int<lower=1> A_i_A[${idxs_A.length}] = {${idxs_A}};`)

		output.R.push("df_RV_A = tribble(");
		output.R.push("  ~l, ~well, ~a, ~A, ~A0, ~AV, ~V, ~C");
		_.forEach(model.absorbanceMeasurements, (x, i) => {
			const ref_a = x.ref_a;
			const rv = model.RV_A[ref_a.i];
			if (rv.ref_a0) {
				output.R.push(` ,"${rv.l}", "${rv.wellPos}", ${i + 1}, ${rv.idx}, ${rv.ref_a0.idx}, NA, NA, NA`)
			}
			else if (rv.ref_av && !rv.ref_cWell) {
				output.R.push(` ,"${rv.l}", "${rv.wellPos}", ${i + 1}, ${rv.idx}, NA, ${rv.ref_av.idx}, NA, NA`)
			}
			else if (rv.ref_cWell) {
				output.R.push(` ,"${rv.l}", "${rv.wellPos}", ${i + 1}, ${rv.idx}, NA, ${rv.ref_av.idx}, ${rv.ref_vWell.idx}, ${rv.ref_cWell.idx}`)
			}
		});
		output.R.push(")");
	}

}

module.exports = {
	createEmptyModel, addLiquid, assignLiquid, measureAbsorbance, measureWeight, aspirate, dispense,
	printModel,
};
