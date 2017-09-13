// qc42-general-sim
const _ = require('lodash');
const fs = require('fs');
const stanModel = require('./src/stanModel.js');
const wellsParser = require('./src/parsers/wellsParser.js');

const {createEmptyModel, addLiquid, assignLiquid, measureAbsorbance, aspirate, dispense} = stanModel;

const wellData = fs.readFileSync("../protocols/qc42-general-wellData.jsonl", "utf8").split("\n").map(s => s.trim()).filter(s => s != "").map(s => JSON.parse(s));
// console.log(wellData)

const subclassNodes = [3,15,500,1000];
const betaDs = [3, 7, 15, 16, 150, 500, 501, 750, 950];
const model = createEmptyModel(subclassNodes, betaDs);
addLiquid(model, "water", {type: "fixed", value: 0});
addLiquid(model, "dye0015", {type: "normal"});
addLiquid(model, "dye0150", {type: "normal"});
assignLiquid(model, "waterLabware", "water");
assignLiquid(model, "dye0015Labware(A01)", "dye0015");
assignLiquid(model, "dye0150Labware(A01)", "dye0150");

const plates = ["plate1", "plate2", "plate3"];
// const plates = ["plate1"];
_.forEach(plates, plate => {
	const wellDataPlate = wellData.filter(x => x.l == plate);
	// console.log({wellDataPlate})
	// const testWells = ["A01", "B01", "C01", "E08", "F08", "G08"];
	// const testWells = ["A01", "A04", "A07", "A11"];
	// const wells = testWells.map(s => `plate1(${s})`);
	const wells = _.map(_.sortBy(wellDataPlate, "i"), "well").map(s => `${plate}(${s})`);
	// console.log({wells})
	measureAbsorbance(model, wells);

	_.forEach(wellDataPlate, row => {
		// // FIXME: for debug only
		// if (row.l != plate) return;
		// if (!_.includes(testWells, row.well)) return;
		// // ENDFIX
		if (row.d > 0) {
			aspirate(model, {p: row.liquidClass, t: row.t, d: row.d, well: `${row.k}Labware(A01)`});
			dispense(model, {p: row.liquidClass, t: row.t, d: row.d, well: `${row.l}(${row.well})`});
		}
		// fill to 300ul with water
		aspirate(model, {p: row.liquidClass, t: row.t, d: 300 - row.d, well: `waterLabware`});
		dispense(model, {p: row.liquidClass, t: row.t, d: 300 - row.d, well: `${row.l}(${row.well})`});
	})

	measureAbsorbance(model, wells);
	measureAbsorbance(model, wells);
	measureAbsorbance(model, wells);
	measureAbsorbance(model, wells);
});

// console.log(JSON.stringify(model, null, '\t'));

stanModel.printModel(model);
