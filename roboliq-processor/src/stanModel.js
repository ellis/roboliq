const _ = require('lodash');
const wellsParser = require('./parsers/wellsParser');

function Ref(name, i) {
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

function createEmptyModel(majorDValues) {
	return {
		majorDValues,
		models: {},
		liquids: {},
		labwares: {},
		wells: {},
		tips: {},
		majorDs: {},
		// Fixed variables
		fvs: [],
		// Random variables
		rvs: [],
		// Calculated variables
		xvs: [],
		pipOps: [], // Pipetting operations
		absorbanceMeasurements: []
	};
}

function addRv(model, rv) {
	rv.i = model.rvs.length;
	rv.idx = model.rvs.length; // FIXME: change this to i + 1
	model.rvs.push(rv);
	return RefRV(rv.i);
}

function addLiquid(model, k, spec) {
	const liquidData = getLiquidData(model, k);
	liquidData.spec = spec;
}

function getLabwareData(model, l) {
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
		const ref = addRv(model, {type: "al", l});
		labwareData.ref_al = ref;
	}
	return lookup(model, labwareData.ref_al);
}

function getRv_a0(model, well) {
	const wellData = getWellData(model, well);
	if (!wellData.hasOwnProperty("ref_a0")) {
		const rv = {type: "a0", well};
		const ref = addRv(model, rv);
		wellData.ref_a0 = ref;
	}
	return lookup(model, wellData.ref_a0);
}

function getRv_av(model, well) {
	const wellData = getWellData(model, well);
	if (!wellData.hasOwnProperty("ref_av")) {
		const rv = {type: "av", well};
		const ref = addRv(model, rv);
		wellData.ref_av = ref;
	}
	return lookup(model, wellData.ref_av);
}


function absorbance_A0(context, model, wells) {
	_.forEach(wells, well => {
		const {labware: l, wellId: wellPos} = wellsParser.parseOne(well);
		const rv_al = getRv_al(model, l);
		const rv_a0 = getRv_a0(model, well);
	});
}

function absorbance_AV(context, model, wells) {
	_.forEach(wells, well => {
		const {labware: l, wellId: wellPos} = wellsParser.parseOne(well);
		const rv_al = getRv_al(model, l);
		const rv_a0 = getRv_a0(model, well);
		const rv_av = getRv_av(model, well);
	});
}

function measureAbsorbance(context, model, wells) {
	_.forEach(wells, well => {
		const {labware: l, wellId: wellPos} = wellsParser.parseOne(well);

		const wellData = getWellData(model, well);
		const rv_al = getRv_al(model, l);
		const rv_a0 = getRv_a0(model, well);

		if (wellData.ref_a) {
			model.absorbanceMeasurements.push({ref_a: wellData.ref_a});
		}
		else if (wellData.ref_vWell) {
			const rv_av = getRv_av(model, well);
			const rv_a = {type: "a", ref_av: RefRV(rv_av.idx), ref_vWell: wellData.ref_vWell, ref_cWell: wellData.ref_cWell};
			const ref_a = addRv(model, rv_a);
			model.absorbanceMeasurements.push({ref_a});
			wellData.ref_a = ref_a;
		}
		else {
			model.absorbanceMeasurements.push({ref_a: RefRV(rv_a0.idx)});
		}
	});
}

function assignLiquid(context, model, well, k) {
	const liquidData = getLiquidData(model, k);
	const wellData = getWellData(model, well);
	wellData.k = k;
	wellData.idx_k = liquidData.idx;
}

function aspirate(context, model, {p, t, d, well}) {
	// create: RV for volume aspirated into tip
	// create: RV for concentration in tip
	// create: RV for new volume in src
	// input: previous volume of src
	// input: variable for k's concentration - we'll need a random variable for the original sources and a calculated variable for what we pipette together

	const wellData = getWellData(model, well);
	const tipData = getTipData(model, t);
	const ref_vWell0 = wellData.ref_vWell; // volume of well before aspirating

	// Check for concentration of source
	if (wellData.hasOwnProperty("k") && !wellData.hasOwnProperty("ref_cWell")) {
		// We have several possibilities:
		// - user can specify concentration exactly
		// - user can specify concentration with spread
		// - concentration should be estimated
		// For now, we'll assume we want to estimate the concentration.
		// Create a new alpha_k variable.
		const liquidData = getLiquidData(model, wellData.k);
		// PROBLEM: we probably do not want to define alpha_k as a RV, because RV_raw ~ normal(0, 1)
		// PROBLEM: some liquids, especially water, will have alpha_k = 0, with no variance. How to specify that?
		wellData.ref_cWell = Ref(`alpha_k_${liquidData.k}`, undefined);
	}
	const ref_cWell0 = wellData.ref_cWell; // concentration of well before aspirating

	let idx_majorD;
	if (_.includes(model.majorDValues, d)) {
		const pd = p+d;
		if (!model.majorDs.hasOwnProperty(pd)) {
			model.majorDs[pd] = {idx: _.size(model.majorDs), p, d};
		}
		idx_majorD = model.majorDs[pd].idx;
	}

	const idx_pip = model.pipOps.length;
	const idx_vTipAsp = model.rvs.length;
	const rv_vTipAsp = {idx: idx_vTipAsp, type: "vTipAsp", idx_pip, idx_majorD};
	// TODO: this is currently just calculated, so it'd be better not to have a
	// RV_raw entry for this, because that adds a superfluous parameter to the
	// model. We should differentiate between RV's that are calculated and RV's
	// that require their own parameter.
	model.rvs.push(rv_vTipAsp);
	tipData.ref_vTipAsp = RefRV(idx_vTipAsp);

	// If we have information about the source concentration,
	// then track the concentration in the tip too.
	if (!_.isUndefined(ref_cWell0)) {
		const idx_cTipAsp = model.rvs.length;
		const rv_cTipAsp = {idx: idx_cTipAsp, type: "cTipAsp", ref_cWell0, idx_majorD};
		model.rvs.push(rv_cTipAsp);
		tipData.ref_cTipAsp = RefRV(idx_cTipAsp);
	}

	// If we already have information about well volume,
	// then update it by removing the aliquot.
	if (ref_vWell0) {
		const idx_vWellAsp = model.rvs.length;
		const rv_vWellAsp = {idx: idx_vWellAsp, type: "vWellAsp", ref_vWell0, ref_vTipAsp};
		model.rvs.push(rv_vWellAsp);
		wellData.ref_vWell = ref_vWellAsp;
	}

	wellData.ref_a = undefined;

	const asp = {
		d
		// p, t, d, well, k,
		// idx_volTot0, idx_conc0,
		// idx_v, idx_c, idx_volTot,
	};
	model.pipOps.push(asp);
}

function dispense(context, model, {p, t, d, well}) {
	// input: RV for volume aspirated into tip
	// input: RV for concentration in tip
	// input: previous volume of dst
	// input: previous conc of dst
	// create: RV for new volume of dst
	// create: RV for new conc of dst

	const wellData = getWellData(model, well);
	const tipData = getTipData(model, t);
	const ref_vWell0 = wellData.ref_vWell; // volume of well before aspirating
	const ref_cWell0 = wellData.ref_cWell; // concentration of well before aspirating
	const ref_vTipAsp = tipData.ref_vTipAsp; // volume in tip
	const ref_cTipAsp = tipData.ref_cTipAsp; // concentration in tip

	const idx_vWellDis = model.rvs.length;
	// const idx_pip = model.pipOps.length;
	const rv_vWellDis = {idx: idx_vWellDis, type: "vWellDis", ref_vTipAsp};
	model.rvs.push(rv_vWellDis);
	tipData.ref_vTipAsp = undefined;
	wellData.ref_vWell = RefRV(idx_vWellDis);

	// If we have information about the source concentration,
	// then track the concentration in the tip too.
	if (ref_cTipAsp) {
		const idx_cWellDis = model.rvs.length;
		const rv_cWellDis = {idx: idx_cWellDis, type: "cWellDis", ref_vWell0, ref_cWell0, ref_vTipAsp, ref_cTipAsp};
		model.rvs.push(rv_cWellDis);
		tipData.ref_cTipAsp = undefined;
		wellData.ref_cWell = RefRV(idx_cWellDis);
	}

	wellData.ref_a = undefined;

	const asp = {
		d
		// p, t, d, well, k,
		// idx_volTot0, idx_conc0,
		// idx_v, idx_c, idx_volTot,
	};
	model.pipOps.push(asp);
}

function printModel(model) {
	console.log();
	console.log("data {");
	if (!_.isEmpty(model.majorDs)) {
		console.log("  real beta_scale;");
		console.log("  real sigma_v_scale;");
		console.log("  real gamma_scale;");
	}
	console.log("  real sigma_a_scale;");
	_.forEach(model.liquids, liquidData => {
		if (_.get(liquidData.spec, "type") === "user") {
			console.log(`  real alpha_k_${liquidData.k}; // concentration of liquid ${liquidData.k}`);
		}
	});
	if (model.absorbanceMeasurements.length > 0) {
		console.log();
		console.log(`  vector<lower=0>[${model.absorbanceMeasurements.length}] A; // Absorbance measurements`);
	}
	console.log("}");

	console.log();
	console.log("transformed data {");
	console.log(`  int NM = 1; // number of labware models`);
	console.log(`  int NL = ${_.size(model.labwares)}; // number of labwares`);
	console.log(`  int NI = ${_.size(model.wells)}; // number of wells`);
	console.log(`  int NT = ${_.size(model.tips)}; // number of tips`);
	console.log(`  int NRV = ${_.size(model.rvs)}; // number of latent random variables`);
	console.log(`  int NJ = ${model.pipOps.length}; // number of pipetting operations`);
	// console.log(`  int NDIS = ${model.dispenses.length}; // number of dispenses`);
	console.log(`  int NPD = ${_.size(model.majorDs)}; // number of liquidClass+majorD combinations`);
	_.forEach(model.liquids, liquidData => {
		if (_.get(liquidData.spec, "type") === "fixed") {
			console.log(`  real alpha_k_${liquidData.k} = ${liquidData.spec.value}; // concentration of liquid ${liquidData.k}`);
		}
	});
	if (!_.isEmpty(model.pipOps)) {
		console.log();
		console.log("  // desired volumes")
		console.log(`  real d[NJ] = {${model.pipOps.map(x => x.d.toFixed(1))}};`);
	}
	console.log("}");

	console.log();
	console.log("parameters {");
	console.log("  vector<lower=0,upper=1>[NM] alpha_l;");
	console.log("  vector<lower=0,upper=1>[NM] sigma_alpha_l;");
	console.log("  vector<lower=0,upper=1>[NM] sigma_alpha_i;");
	if (_.some(model.rvs, rv => rv.type === "av")) {
		console.log("  vector<lower=-0.5,upper=0.5>[NM] alpha_v;");
		console.log("  vector<lower=0,upper=1>[NM] sigma_alpha_v;");
	}
	console.log();
	console.log("  vector[NRV] RV_raw;");
	_.forEach(model.liquids, liquidData => {
		if (_.get(liquidData.spec, "type") === "estimate") {
			console.log(`  real<lower=${liquidData.spec.lower || 0}, upper=${liquidData.spec.upper}> alpha_k_${liquidData.k}; // concentration of liquid ${liquidData.k}`);
		}
	});
	if (model.absorbanceMeasurements.length > 0) {
		console.log("  real sigma_a_raw;");
	}
	if (!_.isEmpty(model.majorDs)) {
		console.log("  vector[NPD] beta_raw;");
		console.log("  vector[NPD] gamma_raw;");
		console.log("  vector[NPD] sigma_v_raw;");
		console.log("  vector[NPD] sigma_gamma_raw;");
	}
	console.log("}");

	const output = {
		transformedParameters: {
			definitions: [],
			statements: []
		}
	};
	print_transformed_parameters(model, output);
	console.log();
	console.log("transformed parameters {");
	output.transformedParameters.definitions.forEach(s => console.log(s));
	console.log();
	output.transformedParameters.statements.forEach(s => console.log(s));
	console.log("}");

	console.log();
	console.log("model {");
	console.log("  RV_raw ~ normal(0, 1);");
	if (!_.isEmpty(model.majorDs)) {
		console.log("  beta_raw ~ normal(0, 1);");
		console.log("  sigma_v_raw ~ normal(0, 1);")
		console.log("  gamma_raw ~ normal(0, 1);");
		console.log("  sigma_gamma_raw ~ exponential(1);");
	}
	if (model.absorbanceMeasurements.length > 0) {
		console.log("  sigma_a_raw ~ exponential(1);");
		console.log();
		const idxsRv = model.absorbanceMeasurements.map(x => x.ref_a.idx);
		console.log(`  A ~ normal(RV[{${idxsRv}}], RV[{${idxsRv}}] * sigma_a);`);
	}
	console.log("}");
}

function print_transformed_parameters(model, output) {
	if (!_.isEmpty(model.majorDs)) {
		output.transformedParameters.definitions.push("");
		output.transformedParameters.definitions.push("  vector[NPD] beta = beta_raw * beta_scale;");
		output.transformedParameters.definitions.push("  vector[NPD] sigma_v = sigma_v_raw * sigma_v_scale;");
		output.transformedParameters.definitions.push("  vector[NPD] gamma = 1 - gamma_raw * gamma_scale;");
		output.transformedParameters.statements.push("");
		output.transformedParameters.statements.push("  for (i in 1:NPD) gamma[i] = max({1, 1 - gamma_raw[i] * gamma_scale});");
	}
	output.transformedParameters.definitions.push("  real sigma_a = sigma_a_raw * sigma_a_scale;");
	output.transformedParameters.definitions.push(`  vector[NRV] RV = RV_raw;`);
	print_transformed_parameters_RV_al(model, output);
	print_transformed_parameters_RV_a0(model, output);
	print_transformed_parameters_RV_av(model, output);
	output.transformedParameters.statements.push("");
	print_transformed_parameters_RVs(model, output);
}
function print_transformed_parameters_RV_al(model, output) {
	const rvs = _.filter(model.rvs, rv => rv.type === "al");
	const idxs = Array(rvs.length);
	const idxs_m = Array(rvs.length);
	for (let i = 0; i < rvs.length; i++) {
		const rv = rvs[i];
		idxs[i] = rv.idx + 1;
		idxs_m[i] = 0 + 1;
	}
	output.transformedParameters.statements.push("");
	output.transformedParameters.statements.push("  // AL[m] ~ normal(alpha_l[m], sigmal_alpha_l[m])")
	output.transformedParameters.statements.push(`  RV[{${idxs}}] = alpha_l[{${idxs_m}}] + RV_raw[{${idxs}}] .* sigma_alpha_l[{${idxs_m}}];`);
}
function print_transformed_parameters_RV_a0(model, output) {
	const rvs = _.filter(model.rvs, rv => rv.type === "a0");
	const idxs = Array(rvs.length);
	const idxs_al = Array(rvs.length);
	const idxs_m = Array(rvs.length);
	for (let i = 0; i < rvs.length; i++) {
		const rv = rvs[i];
		idxs[i] = rv.idx + 1;
		const wellData = model.wells[rv.well];
		const labwareData = model.labwares[wellData.l];
		idxs_al[i] = labwareData.ref_al.i + 1;
		idxs_m[i] = 0 + 1;
	}

	output.transformedParameters.statements.push("");
	output.transformedParameters.statements.push("  // A0[i] ~ normal(AL[m[i]], sigma_alpha_i[m[i]])");
	output.transformedParameters.statements.push(`  RV[{${idxs}}] = RV[{${idxs_al}}] + RV_raw[{${idxs}}] .* sigma_alpha_i[{${idxs_m}}];`);
}
function print_transformed_parameters_RV_av(model, output) {
	const rvs = _.filter(model.rvs, rv => rv.type === "av");
	if (rvs.length == 0) return;

	const idxs = Array(rvs.length);
	const idxs_a0 = Array(rvs.length);
	// const idxs_l = Array(rvs.length);
	const idxs_m = Array(rvs.length);
	for (let i = 0; i < rvs.length; i++) {
		const rv = rvs[i];
		idxs[i] = rv.idx + 1;
		const wellData = model.wells[rv.well];
		idxs_a0[i] = wellData.ref_a0.i + 1;
		// const labwareData = model.labwares[wellData.l];
		// // console.log({rv, wellData, labwareData})
		// idxs_l[i] = labwareData.idx_al;
		idxs_m[i] = 0 + 1;
	}

	output.transformedParameters.statements.push("");
	output.transformedParameters.statements.push("  // AV[i] ~ normal(A0[i] + alpha_v[m[i]], sigma_alpha_v[m[i]])");
	output.transformedParameters.statements.push(`  RV[{${idxs}}] = RV[{${idxs_a0}}] + alpha_v[{${idxs_m}}] + RV_raw[{${idxs}}] .* sigma_alpha_v[{${idxs_m}}];`);
}

const rvHandlers = {
	"al": handle_nop,
	"a0": handle_nop,
	"av": handle_nop,
	"a": handle_a,
	"vTipAsp": handle_vTipAsp,
	"cTipAsp": handle_cTipAsp,
	"vWellAsp": handle_vWellAsp,
	"vWellDis": handle_vWellDis,
	"cWellDis": handle_cWellDis
}

function print_transformed_parameters_RVs(model, output) {
	for (let i = 0; i < model.rvs.length; i++) {
		const rv = model.rvs[i];
		if (rvHandlers.hasOwnProperty(rv.type))
			rvHandlers[rv.type](model, output, rv, i);
		else
			console.log("unknown rv type "+rv.type+": "+JSON.stringify(rv));
	}
}
function handle_nop() {}
function handle_vTipAsp(model, output, rv, idx) {
	// V_t[j] ~ normal(d[j] * (1 + beta[subd[j]]), sigma_v[subd[j]])
	output.transformedParameters.statements.push(`  RV[${idx + 1}] = d[${rv.idx_pip + 1}] * (1 + beta[${rv.idx_majorD + 1}]) + RV_raw[${idx + 1}] .* sigma_v[${rv.idx_majorD + 1}]; // aspirate volume into tip`);
}
function handle_cTipAsp(model, output, rv, idx) {
	// C_t[j] ~ normal(ak[src] * gamma[subd[j]], sigma_gamma)
	const cWell0 = _.isNumber(rv.ref_cWell0.idx)
		? `RV[${rv.ref_cWell0.idx}]`
		: rv.ref_cWell0.name;
	output.transformedParameters.statements.push(`  RV[${idx + 1}] = ${cWell0} * (gamma[${rv.idx_majorD + 1}] + RV_raw[${idx + 1}] * sigma_gamma[${rv.idx_majorD + 1}]); // aspirated concentration in tip`);
}
function handle_vWellAsp(model, output, rv, idx) {
	if (_.isNumber(rv.ref_vWell0.idx)) {
		// VolTot_t[j] = sum of volumes
		output.transformedParameters.statements.push(`  RV[${idx + 1}] = RV[${rv.ref_vWell0.idx}] - RV[${rv.ref_vTipAsp.idx}]; // volume aspirated from well`);
	}
	else {
		// VolTot_t[j] = sum of volumes
		output.transformedParameters.statements.push(`  RV[${idx + 1}] = -RV[${rv.ref_vTipAsp.idx}]; // volume aspirated from well`);
	}
}
function handle_vWellDis(model, output, rv, idx) {
	// Add volume to well
	if (rv.ref_vWell0) {
		// VolTot_t[j] = sum of volumes
		output.transformedParameters.statements.push(`  RV[${idx + 1}] = RV[${rv.ref_vWell0.idx}] + RV[${rv.ref_vTipAsp.idx}]; // volume dispensed into well`);
	}
	else {
		// VolTot_t[j] = sum of volumes
		output.transformedParameters.statements.push(`  RV[${idx + 1}] = RV[${rv.ref_vTipAsp.idx}]; // volume dispensed into well`);
	}
}
function handle_cWellDis(model, output, rv, idx) {
	// C_t[j] = (c[i,j-1] * v[i,j-1] + cTip * vTip) / (v[i,j-1] + vTip)
	if (rv.ref_vWell0 && _.isNumber(rv.ref_vWell0.idx)) {
		const cWell0 = (_.isNumber(rv.ref_cWell0.idx))
			? `RV[${rv.ref_cWell0.idx}]`
			: rv.ref_cWell0.name;
		output.transformedParameters.statements.push(`  RV[${idx + 1}] = (${cWell0} * RV[${rv.ref_vWell0.idx}] + RV[${rv.ref_cTipAsp.idx}] * RV[${rv.ref_vTipAsp.idx}]) / (RV[${rv.ref_vWell0.idx}] + RV[${rv.ref_vTipAsp.idx}]); // new concentration in well`);
	}
	else {
		output.transformedParameters.statements.push(`  RV[${idx + 1}] = RV[${rv.ref_cTipAsp.idx}]; // concentration of dispense in well`);
	}
}
function handle_a(model, output, rv, idx) {
	// A ~ normal(Av + vWell * cWell, (Av + vWell * cWell) * sigma_a)
	if (rv.ref_cWell) {
		output.transformedParameters.statements.push(`  RV[${idx + 1}] = RV[${rv.ref_av.idx}] + RV[${rv.ref_vWell.idx}] * RV[${rv.ref_cWell.idx}]; // absorbance in well`);
	}
	else {
		output.transformedParameters.statements.push(`  RV[${idx + 1}] = RV[${rv.ref_av.idx}]; // absorbance in well`);
	}
}

module.exports = {
	createEmptyModel, addLiquid, assignLiquid, measureAbsorbance, aspirate, dispense,
	printModel,
};
