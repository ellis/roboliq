var _ = require('underscore'),
    fs = require('fs'),
    mdfxTemplate;

function getWellName0(row, col) {
	return String.fromCharCode("A".charCodeAt(0)-1+row) + ('0'+col).slice(-2);
}

function getWellName(row, col) {
	return String.fromCharCode("A".charCodeAt(0)-1+row) + col;
}

function getMdfxWells(rowCol_l) {
	return _.map(rowCol_l, function(rowCol) { return getWellName(rowCol.row, rowCol.col) }).join("|");
}

mdfxTemplate = _.template(fs.readFileSync("tania10_renaturation.mdfx.template").toString());

function template(row, col) {
	var row1 = row, col1 = col,
	    row2 = row, col2 = col + 12,
	    row3 = row + 8, col3 = col,
	    sourceWell = getWellName0(row, col),
	    destinationWells = getWellName0(row1, col1) + '+' + getWellName(row2, col2) + '+' + getWellName(row3, col3),
	    programData = mdfxTemplate({wells: getMdfxWells([{row: row1, col: col1}, {row: row2, col: col2}, {row: row3, col: col3}])});
	return [
		{ distribute: {
			source: "mixPlate("+sourceWell+")",
			destination: "renaturationPlate("+destinationWells+")",
			amount: "4.5ul",
			cleanBetween: "none",
			tip: 5,
			pipettePolicy: "Roboliq_Water_Dry_0050"
		} },
		{ transportLabware: {
			device: "mario__transporter2",
			object: "renaturationPlate",
			site: "REGRIP"
		} },
		{ measureAbsorbance: {
			object: "renaturationPlate",
			programData: programData,
			outputFile: 'C:\\Users\\localadmin\\Desktop\\Ellis\\tania10_renaturation_test--<YYYMMDD_HHmmss>.xml'
		} },
		{ transportLabware: {
			device: "mario__transporter2",
			object: "renaturationPlate",
			site: "P2"
		} }
	];
}

var protocolContents = {
	labware: {
	  mixPlate: { model: "plateModel_96_pcr", location: "DOWNHOLDER" },
	  renaturationPlate:  { model: "plateModel_384_square", location: "P2" }
	},
	protocol: []
};

function setProtocol(rowCol_l) {
	var ll = _.map(rowCol_l, function(rowCol) { return template(rowCol.row, rowCol.col) });
	_.each(ll, function(l) {
		protocolContents.protocol = protocolContents.protocol.concat(l);
	});
}

var sourceRowCol_l = [
	{ row: 2, col: 1 },
	{ row: 3, col: 1 }
];

setProtocol(sourceRowCol_l);
console.log(JSON.stringify(protocolContents, null, '\t'))
