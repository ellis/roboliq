// Execute in ~/src/roboliq/protocols:
// node stan-qc41-150ul.js ~/repo/bsse-lab/EvowareScripts/qc/qc41-150ul-roboliq | tee ~/repo/bsse-lab/EvowareScripts/qc/qc41-150ul-roboliq.stan

// qc41-150ul-sim
const _ = require('lodash');
const fs = require('fs');
const process = require('process');
const stanModel = require('../roboliq-processor/src/stanModel.js');
const wellsParser = require('../roboliq-processor/src/parsers/wellsParser.js');

const {createEmptyModel, addLiquid, assignLiquid, measureAbsorbance, measureWeight, aspirate, dispense} = stanModel;

const wellData = fs.readFileSync("../protocols/qc41-150ul-wellData.jsonl", "utf8").split("\n").map(s => s.trim()).filter(s => s != "").map(s => JSON.parse(s));
// console.log(wellData)

const subclassNodes = [3, 15, 500, 1000];
const betaDs = [3, 7, 15, 16, 150, 500, 501, 750, 950];
const gammaDs = [150];
const model = createEmptyModel(subclassNodes, betaDs, gammaDs);
addLiquid(model, "water", {type: "fixed", value: 0});
addLiquid(model, "dye0150", {type: "normal"});
assignLiquid(model, "waterLabware(A01)", "water");
assignLiquid(model, "dye0150Labware(A01)", "dye0150");

const wellsAll = wellData.map(x => `${x.l}(${x.well})`);
const wellDataSrc = wellData.filter(x => x.roleB === "src");
const wellsSrc = wellDataSrc.map(x => `${x.l}(${x.well})`);
const wellDataDst = wellData.filter(x => x.roleB === "dst");

// Measure absorbance of empty plate
measureAbsorbance(model, wellsAll);

// Measure weight of empty plate
measureWeight(model, "plate1");

// Dispense 150ul to src wells
wellData.forEach(row => {
	if (row.roleB === "src") {
		aspirate(model, {p: row.liquidClass, t: row.t, d: row.d, well: `${row.k}Labware(A01)`});
		dispense(model, {p: row.liquidClass, t: row.t, d: row.d, well: `${row.l}(${row.well})`});
	}
});

// Measure weight
measureWeight(model, "plate1");

// Dispense another 150ul to src wells
wellData.forEach(row => {
	if (row.roleB === "src") {
		aspirate(model, {p: row.liquidClass, t: row.t, d: row.d, well: `${row.k}Labware(A01)`});
		dispense(model, {p: row.liquidClass, t: row.t, d: row.d, well: `${row.l}(${row.well})`});
	}
});

// Measure weight
measureWeight(model, "plate1");

// Measure absorbance of 300ul dye wells
measureAbsorbance(model, wellsSrc);
measureAbsorbance(model, wellsSrc);
measureAbsorbance(model, wellsSrc);
measureAbsorbance(model, wellsSrc);

// Transfer 150ul from src to dst wells
for (let i = 0; i < wellsSrc.length; i++) {
	const src = wellDataSrc[i];
	const dst = wellDataDst[i];
	aspirate(model, {p: src.liquidClass, t: src.t, d: 150, well: `${src.l}(${src.well})`});
	dispense(model, {p: dst.liquidClass, t: dst.t, d: 150, well: `${dst.l}(${dst.well})`});
}

// Add water to fill dye wells to 300ul
// Skipped, because not relevant for analysis

// Add water to water wells
wellData.forEach(row => {
	if (row.roleA === "water") {
		aspirate(model, {p: row.liquidClass, t: row.t, d: 300, well: `waterLabware(A01)`});
		dispense(model, {p: row.liquidClass, t: row.t, d: 300, well: `${row.l}(${row.well})`});
	}
});

// Measure absorbance of 300ul dye wells
measureAbsorbance(model, wellsAll);
measureAbsorbance(model, wellsAll);
measureAbsorbance(model, wellsAll);
measureAbsorbance(model, wellsAll);

// console.log(JSON.stringify(model, null, '\t'));

stanModel.printModel(model, _.last(process.argv));
