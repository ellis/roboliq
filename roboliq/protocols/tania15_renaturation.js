var _ = require('lodash');
var fs = require('fs');

var mdfxTemplate = undefined;

function getWellName0(row, col) {
	return String.fromCharCode("A".charCodeAt(0)-1+row) + ('0'+col).slice(-2);
}

function getWellNames0(rowCol_l) {
	return _.map(rowCol_l, function(rowCol) { return getWellName0(rowCol.row, rowCol.col) }).join("+");
}

function getWellName(row, col) {
	return String.fromCharCode("A".charCodeAt(0)-1+row) + col;
}

function getMdfxWells(rowCol_l) {
	return _.map(rowCol_l, function(rowCol) {
		var s = getWellName(rowCol.row, rowCol.col);
		return s+":"+s;
	}).join("|");
}

mdfxTemplate = _.template(fs.readFileSync("tania15_renaturation.mdfx.template").toString());

function template(row, col, bufferSource, gfpSource) {
	var mixWell = "mixPlate("+getWellName0(row, col)+")",
		renaturationCol = col + 12,
	    renaturationWell = "mixPlate("+getWellName0(row, renaturationCol)+")",
	    programData = mdfxTemplate({wells: getMdfxWells([{row: row, col: renaturationCol}])});
	return [
		{ pipette: {
			steps: [
				{ s: bufferSource, a: "85.5ul", clean: "thorough", pipettePolicy: "Roboliq_Water_Dry_1000" },
				{ s: gfpSource, a: "4.5ul", clean: "thorough", cleanBefore: "none", pipettePolicy: "Roboliq_Water_Wet_1000_mix3x50ul" }
			],
			destination: mixWell
		} },
		{ "evoware.timer.sleep": { agent: "mario", "id": 1, "duration": 420 } },
		{ pipette: { source: mixWell, destination: renaturationWell, amount: "7ul", pipettePolicy: "Roboliq_Water_Dry_1000" } },
		{ transportLabware: { device: "mario__transporter2", object: "mixPlate", site: "REGRIP" } },
		{ measureAbsorbance: {
			object: "mixPlate",
			programData: programData,
			outputFile: 'C:\\Users\\localadmin\\Desktop\\Ellis\\tania15_renaturation--<YYYMMDD_HHmmss>.xml'
		} },
		{ transportLabware: { device: "mario__transporter2", object: "mixPlate", site: "P2" } }
	];
}

var protocolContents = {
	labware: {
		buffer1Labware: { model: "troughModel_100ml", location: "R5" },
		mixPlate:  { model: "plateModel_384_square", location: "P2" }
	},
	source: [
		{ name: "buffer1", well: "buffer1Labware(C01|F01)" },
		{ name: "sfGFP", well: "mixPlate(P01)" },
		{ name: "Q204H_N149Y", well: "mixPlate(P02)" },
		{ name: "tdGFP", well: "mixPlate(P03)" },
		{ name: "N149Y", well: "mixPlate(P04)" },
		{ name: "Q204H", well: "mixPlate(P05)" }
	],
	protocol: []
};

function generateProtocol(bufferSource, col, gfpIndex) {
	var gfpSource_l = ["sfGFP", "Q204H_N149Y", "tdGFP", "N149Y", "Q204H"];

	// Generate the command list
	var ll = [];
	var rowCol_l = [];
	//for (var i = 0; i < gfpSource_l.length; i++) {
	var i = gfpIndex - 1;
		var row = 1 + i;
		var gfpSource = gfpSource_l[i];
		ll.push(template(row, col, bufferSource, gfpSource));
		rowCol_l.push({row: row, col: col});
	//}
	var l = _.flatten(ll);
	protocolContents.protocol = protocolContents.protocol.concat(l);

	// Indicate that the mixWells will be used as sources
	protocolContents.source = protocolContents.source.concat([{
		name: "mix{{WELL}}",
		well: "mixPlate("+getWellNames0(rowCol_l)+")"
	}]);
}

for (var col = 1; col <= 5; col++) {
	for (var gfp = 1; gfp <= 5; gfp++) {
		generateProtocol("buffer1", col, gfp);
		console.log(JSON.stringify(protocolContents, null, '\t'));
	}
}
