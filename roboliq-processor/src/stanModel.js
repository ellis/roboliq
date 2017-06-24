const _ = require('lodash');
const wellsParser = require('./parsers/wellsParser');

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
		labwares: {},
		wells: {},
		tips: {},
		majorDs: {},
		rvs: [],
		aspirates: [],
		dispenses: []
	};
}

function getLabwareData(model, l) {
	const m = "FIXME";
	if (!model.labwares.hasOwnProperty(l)) {
		model.labwares[l] = { m };
	}
	return model.labwares[l];
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
	if (!labwareData.hasOwnProperty("idx_al")) {
		const rv = {type: "al", l, idx: model.rvs.length};
		model.rvs.push(rv);
		labwareData.idx_al = rv.idx;
	}
	return model.rvs[labwareData.idx_al];
}

function getRv_a0(model, well) {
	const wellData = getWellData(model, well);
	if (!wellData.hasOwnProperty("idx_a0")) {
		const rv = {type: "a0", well, idx: model.rvs.length};
		model.rvs.push(rv);
		wellData.idx_a0 = rv.idx;
	}
	return model.rvs[wellData.idx_a0];
}

function getRv_av(model, well) {
	const wellData = getWellData(model, well);
	if (!wellData.hasOwnProperty("idx_av")) {
		const rv = {type: "av", well, idx: model.rvs.length};
		model.rvs.push(rv);
		wellData.idx_av = rv.idx;
	}
	return model.rvs[wellData.idx_av];
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

function aspirate(context, model, {p, t, d, well, k}) {
	// create: RV for volume aspirated into tip
	// create: RV for concentration in tip
	// create: RV for new volume in src
	// input: previous volume of src
	// input: variable for k's concentration - we'll need a random variable for the original sources and a calculated variable for what we pipette together

	const wellData = getWellData(model, well);
	const tipData = getTipData(model, t);
	const idx_volTot0 = wellData.idx_volTot; // volume of well before aspirating
	const idx_conc0 = wellData.idx_conc0; // concentration of well before aspirating

	const idx_v = model.rvs.length;
	const idx_c = idx_v + 1;
	const idx_volTot = idx_c + 1;
	const rv_v = {type: "v", p, t, d, well, k, idx: idx_v};
	const rv_c = {type: "c", p, t, d, well, k, idx_conc0, idx: idx_c};
	const rv_volTot = {type: "volTot", p, t, d, well, idx_volTot0, idx_v, idx: idx_volTot};
	model.rvs.push(rv_v);
	model.rvs.push(rv_c);
	model.rvs.push(rv_volTot);

	wellData.idx_volTot = idx_volTot;
	tipData.idx_v = idx_v;
	tipData.idx_c = idx_c;

	const asp = {
		p, t, d, well, k,
		idx_volTot0, idx_conc0,
		idx_v, idx_c, idx_volTot,
	};
	if (_.includes(model.majorDValues, d)) {
		const pd = p+d;
		if (!model.majorDs.hasOwnProperty(pd)) {
			model.majorDs[pd] = {idx: _.size(model.majorDs), p, d};
		}
		asp.idx_majorD = model.majorDs[pd].idx;
	}
	model.aspirates.push(asp);
}







const context = {};
const majorDValues = [3, 7, 15, 16, 150, 500, 501, 750, 1000];
const model = createEmptyModel(majorDValues);
absorbance_A0(context, model, ["plate1(A01)", "plate1(A02)"]);
absorbance_AV(context, model, ["plate1(A01)", "plate1(A02)"]);
aspirate(context, model, {p: "Roboliq_Water_Air_1000", t: 1, d: 150, well: "trough1", k: "dye0150"});
console.log(JSON.stringify(model, null, '\t'));

console.log();
console.log("data {");
if (!_.isEmpty(model.majorDs)) {
	console.log("  real beta_scale;");
}
console.log("}");

console.log();
console.log("transformed data {");
console.log(`  int NM = 1; // number of labware models`);
console.log(`  int NL = ${_.size(model.labwares)}; // number of labwares`);
console.log(`  int NI = ${_.size(model.wells)}; // number of wells`);
console.log(`  int NT = ${_.size(model.tips)}; // number of tips`);
console.log(`  int NRV = ${_.size(model.rvs)}; // number of latent random variables`);
console.log(`  int NASP = ${model.aspirates.length}; // number of aspirations`);
// console.log(`  int NDIS = ${model.dispenses.length}; // number of dispenses`);
console.log(`  int NPD = ${_.size(model.majorDs)}; // number of liquidClass+majorD combinations`);
if (!_.isEmpty(model.aspirates)) {
	console.log();
	console.log("  // desired volumes")
	console.log(`  vector[NASP] d = {${model.aspirates.map(x => x.d)}};`);
}
console.log("}");

console.log();
console.log("parameters {");
console.log("  vector<lower=0,upper=1>[NM] alpha_l;");
console.log("  vector<lower=0,upper=1>[NM] sigma_alpha_l;");
console.log("  vector<lower=0,upper=1>[NM] sigma_alpha_i;");
console.log("  vector<lower=-0.5,upper=0.5>[NM] alpha_v;");
console.log("  vector<lower=0,upper=1>[NM] sigma_alpha_v;");
console.log();
console.log("  vector[NRV] RV_raw;");
if (!_.isEmpty(model.majorDs)) {
	console.log("  vector[NPD] beta_raw;");
}
console.log("}");

console.log();
console.log("transformed parameters {");
function print_transformed_parameters_RV(model) {
	console.log(`  vector[NRV] RV = RV_raw;`);
	print_transformed_parameters_RV_al(model);
	print_transformed_parameters_RV_a0(model);
	print_transformed_parameters_RV_av(model);
	print_transformed_parameters_RV_v(model);
}
function print_transformed_parameters_RV_al(model) {
	const rvs = _.filter(model.rvs, rv => rv.type === "al");
	const idxs = Array(rvs.length);
	const idxs_m = Array(rvs.length);
	for (let i = 0; i < rvs.length; i++) {
		const rv = rvs[i];
		idxs[i] = rv.idx + 1;
		idxs_m[i] = 0 + 1;
	}
	console.log();
	console.log("  // AL[m] ~ normal(alpha_l[m], sigmal_alpha_l[m])")
	console.log(`  RV[${idxs}] = alpha_l[${idxs_m}] + RV_raw[${idxs}] .* sigma_alpha_l[${idxs_m}]);`);
}
function print_transformed_parameters_RV_a0(model) {
	const rvs = _.filter(model.rvs, rv => rv.type === "a0");
	const idxs = Array(rvs.length);
	const idxs_al = Array(rvs.length);
	const idxs_m = Array(rvs.length);
	for (let i = 0; i < rvs.length; i++) {
		const rv = rvs[i];
		idxs[i] = rv.idx + 1;
		const wellData = model.wells[rv.well];
		const labwareData = model.labwares[wellData.l];
		idxs_al[i] = labwareData.idx_al + 1;
		idxs_m[i] = 0 + 1;
	}

	console.log();
	console.log("  // A0[i] ~ normal(AL[m[i]], sigma_alpha_i[m[i]])");
	console.log(`  RV[${idxs}] = RV[${idxs_al}] + RV_raw[${idxs}] .* sigma_alpha_i[${idxs_m}];`);
}
function print_transformed_parameters_RV_av(model) {
	const rvs = _.filter(model.rvs, rv => rv.type === "av");
	const idxs = Array(rvs.length);
	const idxs_a0 = Array(rvs.length);
	// const idxs_l = Array(rvs.length);
	const idxs_m = Array(rvs.length);
	for (let i = 0; i < rvs.length; i++) {
		const rv = rvs[i];
		idxs[i] = rv.idx + 1;
		const wellData = model.wells[rv.well];
		idxs_a0[i] = wellData.idx_a0 + 1;
		// const labwareData = model.labwares[wellData.l];
		// // console.log({rv, wellData, labwareData})
		// idxs_l[i] = labwareData.idx_al;
		idxs_m[i] = 0 + 1;
	}

	console.log();
	console.log("  // AV[i] ~ normal(A0[i] + alpha_v[m[i]], sigma_alpha_v[m[i]])");
	console.log(`  RV[${idxs}] = RV[${idxs_a0}] + alpha_v[${idxs_m}] + RV_raw[${idxs}] .* sigma_alpha_v[${idxs_m}];`);
}
function print_transformed_parameters_RV_v(model) {

	if (!_.isEmpty(model.majorDs)) {
		console.log();
		console.log("  vector[NPD] beta = beta_raw * beta_scale;");
	}

	const n = model.aspirates.length;
	// create: RV for volume aspirated into tip
	// create: RV for concentration in tip
	// create: RV for new volume in src
	// model.aspirates.push({
	// 	p, t, d, well, k,
	// 	idx_volTot0, idx_conc,
	// 	idx_v, idx_c, idx_volTot,
	// });
	const idxs = model.aspirates.map(x => x.idx_v + 1);
	const rvs = idxs.map(idx => model.rvs[idx - 1]);
	const iAsp = _.range(1, model.aspirates.length + 1);
	const iMajorD = model.aspirates.map(x => x.idx_majorD + 1);

	console.log();
	console.log("  // V_t[j] ~ normal(d[j] * (1 + beta[subd[j]]), sigma_v[subd[j]])");
	console.log(`  RV[${idxs}] = d[${iAsp}] * (1 + beta[${iMajorD}]) + RV_raw[${idxs}] .* sigma_v[${iMajorD}];`);
}


print_transformed_parameters_RV(model);
console.log("}");

console.log();
console.log("model {");
console.log("  RV_raw ~ normal(0, 1);");
if (!_.isEmpty(model.majorDs)) {
	console.log("  beta_raw ~ normal(0, 1);");
}
console.log("}");
