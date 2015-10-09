var _ = require('lodash');
var fs = require('fs');
var path = require('path');

var mdfxTemplate = _.template(fs.readFileSync(path.join(__dirname, "tania15_renaturation.mdfx.template")).toString());

function getWellName0(row, col) {
	return String.fromCharCode("A".charCodeAt(0)-1+row) + ('0'+col).slice(-2);
}

function getWellNames0(rowCol_l) {
	return _.map(rowCol_l, function(rowCol) { return getWellName0(rowCol.row, rowCol.col) }).join(",");
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

function template(row, col, bufferSource, gfpSource) {
	var mixWell = "mixPlate("+getWellName0(row, col)+")",
		renaturationCol = col + 12,
	    renaturationWell = "mixPlate("+getWellName0(row, renaturationCol)+")",
	    programData = mdfxTemplate({wells: getMdfxWells([{row: row, col: renaturationCol}])});
	return [
		{
			command: 'pipetter.pipette',
			items: [
				{ source: bufferSource, volume: "85.5ul", clean: "thorough", pipettePolicy: "Roboliq_Water_Dry_1000" },
				{ source: gfpSource, volume: "4.5ul", clean: "thorough", cleanBefore: "none", pipettePolicy: "Roboliq_Water_Wet_1000_mix3x50ul" }
			],
			destinations: mixWell
		},
		{
			command: "timer.sleep",
			duration: 420
		},
		{
			command: 'pipetter.pipette',
			sources: mixWell,
			destinations: renaturationWell,
			volumes: "7ul",
			pipettePolicy: "Roboliq_Water_Dry_1000"
		},
		{
			command: 'transporter.movePlate',
			object: "mixPlate",
			site: "REGRIP"
		},
		{
			command: "fluorescenceReader.measurePlate",
			object: "mixPlate",
			program: { programData: programData },
			outputFile: 'C:\\Users\\localadmin\\Desktop\\Ellis\\tania15_renaturation--<YYYMMDD_HHmmss>.xml'
		},
		{ command: "transporter.movePlate", object: "mixPlate", destination: "ourlab.mario.site.P2" }
	];
}

var protocol = {
	roboliq: "v1",
	description: "renaturation experiments",
	objects: {
		buffer1Labware: { type: "Plate", model: "ourlab.model.troughModel_100ml", location: "ourlab.mario.site.R5" },
		mixPlate: { type: "Plate", model: "ourlab.model.plateModel_384_square", location: "ourlab.mario.site.P2" },
		buffer1: { type: "Liquid", wells: "buffer1Labware(C01 down to F01)" },
		sfGFP: { type: "Liquid", wells: "mixPlate(P01)" },
		Q204H_N149Y: { type: "Liquid", wells: "mixPlate(P02)" },
		tdGFP: { type: "Liquid", wells: "mixPlate(P03)" },
		N149Y: { type: "Liquid", wells: "mixPlate(P04)" },
		Q204H: { type: "Liquid", wells: "mixPlate(P05)" }
	},
	steps: {}
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
	l.forEach(function(step) {
		protocol.steps[_.keys(protocol.steps).length + 1] = step;
	});

	// Indicate that the mixWells will be used as sources
	// FIXME: re-add this? -- ellis 2015-10-09
	/*protocolContents.source = protocolContents.source.concat([{
		name: "mix{{WELL}}",
		wells: "mixPlate("+getWellNames0(rowCol_l)+")"
	}]);*/
}

for (var col = 1; col <= 5; col++) {
	for (var gfp = 1; gfp <= 5; gfp++) {
		generateProtocol("buffer1", col, gfp);
		console.log(JSON.stringify(protocol, null, '\t'));
	}
}
