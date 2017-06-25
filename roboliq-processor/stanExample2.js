// qc42-general-sim
const _ = require('lodash');
const fs = require('fs');
const stanModel = require('./src/stanModel.js');
const wellsParser = require('./src/parsers/wellsParser.js');

const {createEmptyModel, addLiquid, assignLiquid, measureAbsorbance, aspirate, dispense} = stanModel;

const wellData = fs.readFileSync("../protocols/qc42-general-wellData.jsonl", "utf8").split("\n").map(s => s.trim()).filter(s => s != "").map(s => JSON.parse(s));
// console.log(wellData)

const context = {};
const majorDValues = [3, 7, 15, 16, 150, 500, 501, 750, 1000];
const model = createEmptyModel(majorDValues);
addLiquid(model, "water", {type: "fixed", value: 0});
addLiquid(model, "dye0015", {type: "estimate", lower: 0, upper: 2});
addLiquid(model, "dye0150", {type: "estimate", lower: 0, upper: 2});
assignLiquid(context, model, "waterLabware", "water");
assignLiquid(context, model, "dye0015Labware(A01)", "dye0015");
assignLiquid(context, model, "dye0150Labware(A01)", "dye0150");

const plates = ["plate1"];//, "plate2", "plate3"];
_.forEach(plates, plate => {
	// const wells = _.range(96).map(i => {
	// 	const row = 1 + i % 8;
	// 	const col = 1 + Math.floor(i / 8);
	// 	const wellPos = wellsParser.locationRowColToText(row, col);
	// 	return `${plate}(${wellPos})`;
	// });
	const wells = [`plate1(A01)`, "plate1(B01)", "plate1(C01)"];
	// console.log({wells})
	measureAbsorbance(context, model, wells);

	_.forEach(wellData, row => {
		if (row.l === plate && row.d > 0 && _.includes(["A01", "B01", "C01"], row.well)) {
			aspirate(context, model, {p: row.liquidClass, t: row.t, d: row.d, well: `${row.k}Labware(A01)`});
			dispense(context, model, {p: row.liquidClass, t: row.t, d: row.d, well: `${row.l}(${row.well})`});
		}
		// TODO: fill to 300ul with water
		// aspirate(context, model, {p: row.liquidClass, t: row.t, d: row.d, well: `${row.k}Labware(A01)`, k: row.k});
	})

	measureAbsorbance(context, model, wells);
	measureAbsorbance(context, model, wells);
	measureAbsorbance(context, model, wells);
	measureAbsorbance(context, model, wells);
});

// console.log(JSON.stringify(model, null, '\t'));

stanModel.printModel(model);
