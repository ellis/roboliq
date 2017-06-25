// qc42-general-sim
const _ = require('lodash');
const stanModel = require('./src/stanModel.js');
const wellsParser = require('./src/parsers/wellsParser.js');

const {createEmptyModel, addLiquid, assignLiquid, measureAbsorbance, aspirate, dispense} = stanModel;

const context = {};
const majorDValues = [3, 7, 15, 16, 150, 500, 501, 750, 1000];
const model = createEmptyModel(majorDValues);
addLiquid(model, "water", {type: "fixed", value: 0});
addLiquid(model, "dye0015", {type: "estimate", lower: 0, upper: 2});
addLiquid(model, "dye0150", {type: "estimate", lower: 0, upper: 2});
assignLiquid(context, model, "waterLabware", "water");
assignLiquid(context, model, "dyeLabware", "dye");

const plates = ["plate1", "plate2", "plate3"];
_.forEach(plates, plate => {
	const wells = _.range(96).map(i => {
		const row = 1 + i % 8;
		const col = 1 + Math.floor(i / 8);
		const wellPos = wellsParser.locationRowColToText(row, col);
		return `${plate}(${wellPos})`;
	});
	// console.log({wells})
	measureAbsorbance(context, model, wells);
	// aspirate(context, model, {p: "Roboliq_Water_Air_1000", t: 1, d: 150, well: "troughLabware1(A01)", 	aspirate(context, model, {p: "Roboliq_Water_Air_1000", t: 1, d: 150, well: "waterLabware1(A01)", k: "water"});
	// dispense(context, model, {p: "Roboliq_Water_Air_1000", t: 1, d: 150, well: "plate1(A01)"});
});
// console.log(JSON.stringify(model, null, '\t'));

stanModel.printModel(model);
