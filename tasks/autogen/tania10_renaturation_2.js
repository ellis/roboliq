function template(sourceWell, destinationWells) {
	return [
		{ thermocyclePlate: { object: "mixPlate", program: "?" } },
		{ transportLabware: { object: "mixPlate", site: "HOLDER" } },
		{ distribute: { source: "mixPlate("+sourceWell+")", destination: "renaturationPlate("+destinationWells+")", amount: "4.5ul" } },
		{ measureAbsorbance: { object: "renaturationPlate", programFile: "tania10_renaturation.mdfx" } }
	];
}

var protocolContents = {
	labware: {
	  mixPlate: { model: "plateModel_96_pcr", location: "HOLDER" },
	  renaturationPlate:  { model: "plateModel_384_square", location: "P2" }
	},
	protocol: []
};

protocolContents.protocol = protocolContents.protocol.concat(template("A01", "A01,B01,C01"))
console.log(JSON.stringify(protocolContents, null, '\t'))
