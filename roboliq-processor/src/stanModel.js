const _ = require('lodash');
const wellsParser = require('./parsers/wellsParser');

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

function createEmptyModel(majorDValues) {
	return {
		majorDValues,
		models: {},
		liquids: {},
		labwares: {},
		wells: {},
		tips: {},
		majorDs: {},
		// Random variables
		rvs: [],
		RV_AL: [],
		RV_A0: [],
		RV_AV: [],
		RV_VTIPASP: [],
		RV_V: [],
		RV_C: [],
		RV_A: [],
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

function addRv2(model, group, rv) {
	const list = model[group];
	rv.i = list.length;
	rv.idx = list.length + 1;
	list.push(rv);
	return Ref(group, rv.i);
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
				const rv_a = {type: "a", ref_av, ref_vWell: wellData.ref_vWell, ref_cWell: wellData.ref_cWell};
				const ref_a = addRv2(model, "RV_A", rv_a);
				model.absorbanceMeasurements.push({ref_a});
				wellData.ref_a = ref_a;
			}
			// Otherwise the liquid is clear:
			else {
				const rv_a = {type: "a", ref_av};
				const ref_a = addRv2(model, "RV_A", rv_a);
				model.absorbanceMeasurements.push({ref_a});
				wellData.ref_a = ref_a;
			}
		}
		// Otherwise, just measure A0
		else {
			const ref_a0 = Ref(rv_a0);
			const rv_a = {type: "a", ref_a0};
			const ref_a = addRv2(model, "RV_A", rv_a);
			wellData.ref_a = ref_a;
			model.absorbanceMeasurements.push({ref_a});
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

	let idx_majorD;
	if (_.includes(model.majorDValues, d)) {
		const pd = p+d;
		if (!model.majorDs.hasOwnProperty(pd)) {
			model.majorDs[pd] = {idx: _.size(model.majorDs), p, d};
		}
		idx_majorD = model.majorDs[pd].idx;
	}

	const idx_pip = model.pipOps.length;
	const rv_vTipAsp = {type: "vTipAsp", idx_pip, idx_majorD};
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
	// 	const rv_cTipAsp = {idx: idx_cTipAsp, type: "cTipAsp", ref_cWell0, idx_majorD};
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
	const ref_vWell0 = wellData.ref_vWell; // volume of well before dispensing
	const ref_cWell0 = wellData.ref_cWell; // concentration of well before dispensing
	const ref_vTipAsp = tipData.ref_vTipAsp; // volume in tip
	const ref_cTipAsp = tipData.ref_cTipAsp; // concentration in tip

	// const idx_pip = model.pipOps.length;
	const rv_vWellDis = {type: "vWellDis", well, ref_vTipAsp};
	const ref_vWellDis = addRv2(model, "RV_V", rv_vWellDis);
	tipData.ref_vTipAsp = undefined;
	wellData.ref_vWell = ref_vWellDis;

	// If we have information about the tip concentration,
	// then update the concentration in the destination well too.
	if (ref_cTipAsp) {
		const rv_cWellDis = {type: "cWellDis", ref_vWell0, ref_cWell0, ref_vTipAsp, ref_cTipAsp, well, of: well};
		const ref_cWellDis = addRv2(model, "RV_C", rv_cWellDis);
		wellData.ref_cWell = ref_cWellDis;
		tipData.ref_cTipAsp = undefined;
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
	const output = {
		transformedData: {
			definitions: [],
			statements: []
		},
		transformedParameters: {
			definitions: [],
			statements: []
		}
	};
	print_transformed_data(model, output);
	print_transformed_parameters(model, output);

	console.log();
	console.log("data {");
	if (!_.isEmpty(model.majorDs)) {
		console.log("  real beta_scale;");
		console.log("  real sigma_v_scale;");
		console.log("  real gamma_scale;");
		console.log("  real sigma_gamma_scale;");
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
	output.transformedData.definitions.forEach(s => console.log(s));
	console.log();
	output.transformedData.statements.forEach(s => console.log(s));
	console.log("}");

	console.log();
	console.log("parameters {");
	console.log("  vector<lower=0,upper=1>[NM] alpha_l;");
	console.log("  vector<lower=0,upper=1>[NM] sigma_alpha_l;");
	console.log("  vector<lower=0,upper=1>[NM] sigma_alpha_i;");
	if (model.RV_AV.length > 0) {
		console.log("  vector<lower=-0.5,upper=0.5>[NM] alpha_v;");
		console.log("  vector<lower=0,upper=1>[NM] sigma_alpha_v;");
	}
	console.log();
	if (model.RV_AL.length > 0) console.log(`  vector[${model.RV_AL.length}] RV_AL_raw;`);
	if (model.RV_A0.length > 0) console.log(`  vector[${model.RV_A0.length}] RV_A0_raw;`);
	if (model.RV_AV.length > 0) console.log(`  vector[${model.RV_AV.length}] RV_AV_raw;`);
	if (model.RV_VTIPASP.length > 0) console.log(`  vector[${model.RV_VTIPASP.length}] RV_VTIPASP_raw;`);
	// if (model.RV_C_raw.length > 0) console.log(`  vector[${model.RV_C_raw.length}] RV_C_raw;`)
	console.log("  vector[NRV] RV_raw;");
	_.forEach(model.liquids, liquidData => {
		if (_.get(liquidData.spec, "type") === "estimate") {
			console.log(`  real<lower=${liquidData.spec.lower || 0}, upper=${liquidData.spec.upper}> alpha_k_${liquidData.k}; // concentration of liquid ${liquidData.k}`);
		}
	});
	if (model.absorbanceMeasurements.length > 0) {
		console.log("  real<lower=0> sigma_a_raw;");
	}
	if (!_.isEmpty(model.majorDs)) {
		console.log("  vector[NPD] beta_raw;");
		console.log("  vector[NPD] gamma_raw;");
		console.log("  vector<lower=0>[NPD] sigma_v_raw;");
		console.log("  real<lower=0> sigma_gamma_raw;");
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
	if (model.RV_AL.length > 0) console.log("  RV_AL_raw ~ normal(0, 1);");
	if (model.RV_A0.length > 0) console.log("  RV_A0_raw ~ normal(0, 1);");
	if (model.RV_AV.length > 0) console.log("  RV_AV_raw ~ normal(0, 1);");
	if (model.RV_VTIPASP.length > 0) console.log("  RV_VTIPASP_raw ~ normal(0, 1);");
	// if (model.RV_C_raw.length > 0) console.log("  RV_C_raw ~ normal(0, 1);");
	console.log("  RV_raw ~ normal(0, 1);");
	if (!_.isEmpty(model.majorDs)) {
		console.log("  beta_raw ~ normal(0, 1);");
		console.log("  sigma_v_raw ~ exponential(1);")
		console.log("  gamma_raw ~ normal(0, 1);");
		console.log("  sigma_gamma_raw ~ exponential(1);");
	}
	if (model.absorbanceMeasurements.length > 0) {
		console.log("  sigma_a_raw ~ exponential(1);");
		console.log();
		const idxsRv = model.absorbanceMeasurements.map(x => x.ref_a.idx);
		// console.log(`  A ~ normal(RV_A[{${idxsRv}}], RV_A[{${idxsRv}}] * sigma_a);`);
		console.log(`  A ~ normal(RV_A[A_i_A], RV_A[A_i_A] * sigma_a);`);
	}
	console.log("}");
}

function print_transformed_data(model, output) {
	output.transformedData.definitions.push(`  int NM = 1; // number of labware models`);
	output.transformedData.definitions.push(`  int NL = ${_.size(model.labwares)}; // number of labwares`);
	output.transformedData.definitions.push(`  int NI = ${_.size(model.wells)}; // number of wells`);
	output.transformedData.definitions.push(`  int NT = ${_.size(model.tips)}; // number of tips`);
	output.transformedData.definitions.push(`  int NRV = ${_.size(model.rvs)}; // number of latent random variables`);
	output.transformedData.definitions.push(`  int NJ = ${model.pipOps.length}; // number of pipetting operations`);
	// console.log(`  int NDIS = ${model.dispenses.length}; // number of dispenses`);
	output.transformedData.definitions.push(`  int NPD = ${_.size(model.majorDs)}; // number of liquidClass+majorD combinations`);
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

function print_transformed_parameters(model, output) {
	if (!_.isEmpty(model.majorDs)) {
		output.transformedParameters.definitions.push("");
		output.transformedParameters.definitions.push("  vector[NPD] beta = beta_raw * beta_scale;");
		output.transformedParameters.definitions.push("  vector<lower=0>[NPD] sigma_v = sigma_v_raw * sigma_v_scale;");
		output.transformedParameters.definitions.push("  vector[NPD] gamma;");
		output.transformedParameters.definitions.push("  real sigma_gamma = sigma_gamma_raw * sigma_gamma_scale;");
		output.transformedParameters.statements.push("");
		output.transformedParameters.statements.push("  for (i in 1:NPD) gamma[i] = max({1, 1 - gamma_raw[i] * sigma_gamma});");
	}
	output.transformedParameters.definitions.push("  real<lower=0> sigma_a = sigma_a_raw * sigma_a_scale;");
	output.transformedParameters.definitions.push(`  vector[NRV] RV;`);
	print_transformed_parameters_RV_al(model, output);
	print_transformed_parameters_RV_a0(model, output);
	print_transformed_parameters_RV_av(model, output);
	print_transformed_parameters_RV_vTipAsp(model, output);
	print_transformed_parameters_RV_V(model, output);
	print_transformed_parameters_RV_C(model, output);
	print_transformed_parameters_RV_A(model, output);
	output.transformedParameters.statements.push("");
	print_transformed_parameters_RVs(model, output);
}
function print_transformed_parameters_RV_al(model, output) {
	const rvs = model.RV_AL;
	if (rvs.length == 0) return;

	const idxs_m = Array(rvs.length);
	for (let i = 0; i < rvs.length; i++) {
		const rv = rvs[i];
		idxs_m[i] = 0 + 1;
	}
	output.transformedParameters.definitions.push(`  vector<lower=0>[${rvs.length}] RV_AL; // average absorbance of labware`);
	output.transformedParameters.statements.push("");
	output.transformedParameters.statements.push("  // AL[m] ~ normal(alpha_l[m], sigmal_alpha_l[m])")
	output.transformedParameters.statements.push(`  RV_AL = alpha_l[{${idxs_m}}] + RV_AL_raw .* sigma_alpha_l[{${idxs_m}}];`);
}
function print_transformed_parameters_RV_a0(model, output) {
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
function print_transformed_parameters_RV_av(model, output) {
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
	output.transformedParameters.definitions.push(`  vector<lower=0>[${rvs.length}] RV_AV; // absorbance of water-filled wells`);
	output.transformedParameters.statements.push("");
	output.transformedParameters.statements.push("  // AV[i] ~ normal(A0[i] + alpha_v[m[i]], sigma_alpha_v[m[i]])");
	output.transformedParameters.statements.push(`  RV_AV = RV_A0[RV_AV_i_A0] + alpha_v[RV_AV_i_m] + RV_AV_raw .* sigma_alpha_v[RV_AV_i_m];`);
}
function print_transformed_parameters_RV_vTipAsp(model, output) {
	const rvs = model.RV_VTIPASP;
	if (rvs.length == 0) return;

	const idxs_pip = rvs.map(rv => rv.idx_pip + 1);
	const idxs_majorD = rvs.map(rv => rv.idx_majorD + 1);

	output.transformedParameters.definitions.push(`  vector<lower=0>[${rvs.length}] RV_VTIPASP; // volume aspirated into tip`);
	output.transformedParameters.statements.push("");
	output.transformedParameters.statements.push("  // V_t[j] ~ normal(d[j] * (1 + beta[subd[j]]), sigma_v[subd[j]])");
	output.transformedData.definitions.push(`  int<lower=1> RV_VTIPASP_i_d[${idxs_pip.length}] = {${idxs_pip}};`)
	output.transformedData.definitions.push(`  int<lower=1> RV_VTIPASP_i_majorD[${idxs_majorD.length}] = {${idxs_majorD}};`)
	output.transformedParameters.statements.push(`  RV_VTIPASP = d[RV_VTIPASP_i_d] .* (1 + beta[RV_VTIPASP_i_majorD]) + RV_VTIPASP_raw .* sigma_v[RV_VTIPASP_i_majorD]; // volume aspirated into tip`);
}
function print_transformed_parameters_RV_V(model, output) {
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
			// C_t[j] = (c[i,j-1] * v[i,j-1] + cTip * vTip) / (v[i,j-1] + vTip)
			// Add volume to well
			if (rv.ref_vWell0) {
				// VolTot_t[j] = sum of volumes
				output.transformedParameters.statements.push(`  RV_V[${rv.idx}] = RV_V[${rv.ref_vWell0.idx}] + RV_VTIPASP[${rv.ref_vTipAsp.idx}]; // volume in ${rv.well} after dispensing`);
			}
			else {
				// VolTot_t[j] = sum of volumes
				output.transformedParameters.statements.push(`  RV_V[${rv.idx}] = RV_VTIPASP[${rv.ref_vTipAsp.idx}]; // volume in ${rv.well} after dispensing`);
			}
		}
	});
}
function print_transformed_parameters_RV_C(model, output) {
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
			// C_t[j] = (c[i,j-1] * v[i,j-1] + cTip * vTip) / (v[i,j-1] + vTip)
			if (rv.ref_vWell0 && _.isNumber(rv.ref_vWell0.idx)) {
				const cWell0 = `RV_C[${rv.ref_cWell0.idx}]`;
				output.transformedParameters.statements.push(`  RV_C[${rv.idx}] = (${cWell0} * RV[${rv.ref_vWell0.idx}] + RV_C[${rv.ref_cTipAsp.idx}] * RV_VTIPASP[${rv.ref_vTipAsp.idx}]) / (RV[${rv.ref_vWell0.idx}] + RV_VTIPASP[${rv.ref_vTipAsp.idx}]); // concentration of ${rv.of}`);
			}
			else {
				output.transformedParameters.statements.push(`  RV_C[${rv.idx}] = RV_C[${rv.ref_cTipAsp.idx}]; // concentration of ${rv.of}`);
			}
		}
	});
}
function print_transformed_parameters_RV_A(model, output) {
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
	}

}

const rvHandlers = {
	"al": handle_nop,
	"a0": handle_nop,
	"av": handle_nop,
	// "a": handle_a,
	// "cTipAsp": handle_cTipAsp,
	// "vWellAsp": handle_vWellAsp,
	// "vWellDis": handle_vWellDis,
	// "cWellDis": handle_cWellDis
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
// function handle_cTipAsp(model, output, rv, idx) {
// 	// C_t[j] ~ ak[src] * normal(gamma[subd[j]], sigma_gamma)
// 	const cWell0 = _.isNumber(rv.ref_cWell0.idx)
// 		? `RV[${rv.ref_cWell0.idx}]`
// 		: rv.ref_cWell0.name;
// 	output.transformedParameters.statements.push(`  RV[${idx + 1}] = ${cWell0} * (gamma[${rv.idx_majorD + 1}] + RV_raw[${idx + 1}] * sigma_gamma); // aspirated concentration in tip`);
// }

module.exports = {
	createEmptyModel, addLiquid, assignLiquid, measureAbsorbance, aspirate, dispense,
	printModel,
};
