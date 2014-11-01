var _ = require('underscore'),
    fs = require('fs'),
    mdfxTemplate;

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

mdfxTemplate = _.template(fs.readFileSync("tania14_renaturation.mdfx.template").toString());

function template(row, col, bufferSource, gfpSource) {
	var mixWell = "mixPlate("+getWellName0(row, col)+")",
		renaturationCol = col + 12,
	    renaturationWell = "mixPlate("+getWellName0(row, renaturationCol)+")",
	    programData = mdfxTemplate({wells: getMdfxWells([{row: row, col: renaturationCol}])});
	return [
		{ pipette: {
			steps: [
				{ s: bufferSource, a: "68ul", clean: "thorough", pipettePolicy: "Roboliq_Water_Dry_1000" },
				{ s: gfpSource, a: "7ul", clean: "thorough", cleanBefore: "none", pipettePolicy: "Roboliq_Water_Wet_1000_mix3x50ul" }
			],
			destination: mixWell
		} },
		{ "evoware.timer.sleep": { agent: "mario", "id": 1, "duration": 300 } },
		{ pipette: { source: mixWell, destination: renaturationWell, amount: "4.5ul", pipettePolicy: "Roboliq_Water_Dry_1000" } },
		{ transportLabware: { device: "mario__transporter2", object: "mixPlate", site: "REGRIP" } },
		{ measureAbsorbance: {
			object: "mixPlate",
			programData: programData,
			outputFile: 'C:\\Users\\localadmin\\Desktop\\Ellis\\tania14_renaturation--<YYYMMDD_HHmmss>.xml'
		} },
		{ transportLabware: { device: "mario__transporter2", object: "mixPlate", site: "P2" } }
	];
}

var protocolContents = {
	labware: {
		buffer1Labware: { model: "troughModel_100ml", location: "R5" },
		buffer2Labware: { model: "troughModel_100ml", location: "R6" },
		gfpLabware: { model: "tubeHolderModel_1500ul", location: "T3" },
		mixPlate:  { model: "plateModel_384_square", location: "P2" }
	},
	source: [
		{ name: "buffer1", well: "buffer1Labware(C01|F01)" },
		{ name: "buffer2", well: "buffer2Labware(C01|F01)" },
		{ name: "sfGFP", well: "gfpLabware(A01)" },
		{ name: "Q204H_N149Y", well: "gfpLabware(A02)" },
		{ name: "tdGFP", well: "gfpLabware(A03)" },
		{ name: "N149Y", well: "gfpLabware(A04)" },
		{ name: "Q204H", well: "gfpLabware(A05)" }
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
	protocolContents.protocol = l;

	// Indicate that the mixWells will be used as sources
	protocolContents.source = protocolContents.source.concat([{
		name: "mix{{WELL}}",
		well: "mixPlate("+getWellNames0(rowCol_l)+")"
	}]);
}

var bufferSource = process.argv[2];
var col = parseInt(process.argv[3]);
var gfpIndex = parseInt(process.argv[4]);
generateProtocol(bufferSource, col, gfpIndex);
console.log(JSON.stringify(protocolContents, null, '\t'))
