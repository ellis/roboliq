const _ = require('lodash');
const wellsParser = require('./parsers/wellsParser');

// Standard Normal variate using Box-Muller transform.
function randn_bm(mean, sigma) {
	var u = 1 - Math.random(); // Subtraction to flip [0, 1) to (0, 1].
	var v = 1 - Math.random();
	return mean + sigma * Math.sqrt( -2.0 * Math.log( u ) ) * Math.cos( 2.0 * Math.PI * v );
}

function createEmptyModel() {
	return {
		models: {},
		labwares: {},
		wells: {},
		rvs: []
	};
}

function absorbance_A0(context, model, wells) {
	_.forEach(wells, well => {
		const {labware: l, wellId: wellPos} = wellsParser.parseOne(well);

		const m = "FIXME";

		let rv_al;
		if (!model.labwares.hasOwnProperty(l)) {
			rv_al = {type: "al", l, idx: model.rvs.length};
			model.rvs.push(rv_al);
			model.labwares[l] = {
				m,
				idx_al: rv_al.idx
			};
		}

		let rv_a0;
		if (!model.wells.hasOwnProperty(well)) {
			rv_a0 = {type: "a0", well, idx: model.rvs.length};
			model.rvs.push(rv_a0);

			rv_av = {type: "av", well, idx: model.rvs.length};
			model.rvs.push(rv_av);

			model.wells[well] = {
				l,
				pos: wellPos,
				idx_a0: rv_a0.idx,
				idx_av: rv_av.idx
			};
		}
	});
}

const context = {};
const model = createEmptyModel();
absorbance_A0(context, model, ["plate1(A01)", "plate1(A02)"]);
console.log(JSON.stringify(model, null, '\t'));

console.log();
console.log("transformed data {");
console.log(`  int NM = 1; // number of labware models`);
console.log(`  int NL = ${_.size(model.labwares)}; // number of labwares`);
console.log(`  int NI = ${_.size(model.wells)}; // number of wells`);
console.log(`  int NRV = ${_.size(model.rvs)}; // number of latent random variables`);
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
console.log("}");

console.log();
console.log("transformed parameters {");
function print_transformed_parameters_RV(model) {
	console.log(`  vector[NRV] RV = RV_raw;`);
	print_transformed_parameters_RV_al(model);
	print_transformed_parameters_RV_a0(model);
	print_transformed_parameters_RV_av(model);
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


print_transformed_parameters_RV(model);
console.log("}");

console.log();
console.log("model {");
console.log("  RV_raw ~ normal(0, 1);");
console.log("}");
