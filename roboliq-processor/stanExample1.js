/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017 Ellis Whitehead
 * @license GPL-3.0
 */

const stanModel = require('./src/stanModel.js');

const {createEmptyModel, addLiquid, assignLiquid, measureAbsorbance, aspirate, dispense} = stanModel;

const context = {};
const majorDValues = [3, 7, 15, 16, 150, 500, 501, 750, 1000];
const model = createEmptyModel(majorDValues);
addLiquid(model, "water", {type: "fixed", value: 0});
addLiquid(model, "dye", {type: "estimate", lower: 0, upper: 1});
assignLiquid(context, model, "waterLabware1(A01)", "water");
assignLiquid(context, model, "troughLabware1(A01)", "dye");
measureAbsorbance(context, model, ["plate1(A01)", "plate1(A02)"]);
aspirate(context, model, {p: "Roboliq_Water_Air_1000", t: 1, d: 150, well: "troughLabware1(A01)", k: "dye0150"});
dispense(context, model, {p: "Roboliq_Water_Air_1000", t: 1, d: 150, well: "plate1(A01)"});
aspirate(context, model, {p: "Roboliq_Water_Air_1000", t: 1, d: 150, well: "waterLabware1(A01)", k: "water"});
dispense(context, model, {p: "Roboliq_Water_Air_1000", t: 1, d: 150, well: "plate1(A01)"});
// absorbance_A0(context, model, ["plate1(A01)", "plate1(A02)"]);
// absorbance_AV(context, model, ["plate1(A01)"]);
measureAbsorbance(context, model, ["plate1(A01)", "plate1(A02)"]);
measureAbsorbance(context, model, ["plate1(A01)", "plate1(A02)"]);
// console.log(JSON.stringify(model, null, '\t'));

stanModel.printModel(model);
