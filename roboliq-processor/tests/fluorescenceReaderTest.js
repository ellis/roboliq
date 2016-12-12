var _ = require('lodash');
var should = require('should');
var roboliq = require('../src/roboliq.js')

describe('commands/fluorescenceReader', function() {
	var protocol0 = {
		roboliq: "v1",
		objects: {
			plate1: {
				type: "Plate",
				model: "ourlab.model.plateModel_384_square",
				location: "ourlab.mario.site.P2"
			}
		}
	};

	describe('fluorescenceReader.measurePlate', function () {
		it('should move plate to reader, measure, then move plate back to original location', function () {
			var protocol = _.merge({}, protocol0, {
				steps: {
					1: {
						command: "fluorescenceReader.measurePlate",
						object: "plate1",
						programFile: "./protocols/tania13_ph-temp.mdfx"
					}
				}
			});
			var result = roboliq.run([__dirname+"/ourlab.js", "-o", "", "-T"], protocol, false);
			//console.log("result:\n"+JSON.stringify(result, null, '\t'))
			should.deepEqual(result.protocol.errors, {});
			should.deepEqual(result.protocol.warnings, {});
			//console.log("result:\n"+JSON.stringify(result.output.steps[1], null, '\t'))
			should.deepEqual(result.output.steps[1],
				{
					"1": {
						"1": {
							"command": "transporter._movePlate",
							"agent": "ourlab.mario.evoware",
							"equipment": "ourlab.mario.roma1",
							"program": "Narrow",
							"object": "plate1",
							"destination": "ourlab.mario.site.REGRIP"
						},
						"2": {
							"1": {
								"1": {
									"command": "evoware._facts",
									"agent": "ourlab.mario.evoware",
									"factsEquipment": "ReaderNETwork",
									"factsVariable": "ReaderNETwork_Open"
								},
								"command": "equipment.openSite|ourlab.mario.evoware|ourlab.mario.reader",
								"agent": "ourlab.mario.evoware",
								"equipment": "ourlab.mario.reader",
								"site": "ourlab.mario.site.READER"
							},
							"command": "equipment.openSite",
							"agent": "ourlab.mario.evoware",
							"equipment": "ourlab.mario.reader",
							"site": "ourlab.mario.site.READER"
						},
						"3": {
							"command": "transporter._movePlate",
							"agent": "ourlab.mario.evoware",
							"equipment": "ourlab.mario.roma2",
							"program": "Wide",
							"object": "plate1",
							"destination": "ourlab.mario.site.READER"
						},
						"4": {
							"1": {
								"1": {
									"command": "evoware._facts",
									"agent": "ourlab.mario.evoware",
									"factsEquipment": "ReaderNETwork",
									"factsVariable": "ReaderNETwork_Close"
								},
								"command": "equipment.close|ourlab.mario.evoware|ourlab.mario.reader",
								"agent": "ourlab.mario.evoware",
								"equipment": "ourlab.mario.reader"
							},
							"command": "equipment.close",
							"agent": "ourlab.mario.evoware",
							"equipment": "ourlab.mario.reader"
						},
						"command": "transporter.movePlate",
						"object": "plate1",
						"destination": "ourlab.mario.site.READER"
					},
					"2": {
						"1": {
							"command": "evoware._facts",
							"agent": "ourlab.mario.evoware",
							"factsEquipment": "ReaderNETwork",
							"factsVariable": "ReaderNETwork_Measure",
							"factsValue": "%{TEMPDIR}\\fluorescence.xml|<TecanFile xmlns:xsi&equal;&quote;http://www.w3.org/2001/XMLSchema-instance&quote; xsi:schemaLocation&equal;&quote;tecan.at.schema.documents Main.xsd&quote; fileformat&equal;&quote;Tecan.At.Measurement&quote; fileversion&equal;&quote;2.0&quote; xmlns&equal;&quote;tecan.at.schema.documents&quote;><FileInfo type&equal;&quote;&quote; instrument&equal;&quote;infinite 200Pro&quote; version&equal;&quote;&quote; createdFrom&equal;&quote;localadmin&quote; createdAt&equal;&quote;2014-12-21T17:51:32.0705908Z&quote; createdWith&equal;&quote;Tecan.At.XFluor.ReaderEditor.XFluorReaderEditor&quote; description&equal;&quote;&quote; /><TecanMeasurement id&equal;&quote;1&quote; class&equal;&quote;&quote;><MeasurementManualCycle id&equal;&quote;2&quote; number&equal;&quote;1&quote; type&equal;&quote;Standard&quote;><CyclePlate id&equal;&quote;3&quote; file&equal;&quote;GRE384fw&quote; plateWithCover&equal;&quote;False&quote;><PlateRange id&equal;&quote;4&quote; range&equal;&quote;A1:P24&quote; auto&equal;&quote;false&quote;><MeasurementFluoInt readingMode&equal;&quote;Top&quote; id&equal;&quote;5&quote; mode&equal;&quote;Normal&quote; type&equal;&quote;&quote; name&equal;&quote;FluoInt&quote; longname&equal;&quote;&quote; description&equal;&quote;&quote;><Well id&equal;&quote;6&quote; auto&equal;&quote;true&quote;><MeasurementReading id&equal;&quote;7&quote; name&equal;&quote;&quote; beamDiameter&equal;&quote;3000&quote; beamGridType&equal;&quote;Single&quote; beamGridSize&equal;&quote;1&quote; beamEdgeDistance&equal;&quote;auto&quote;><ReadingLabel id&equal;&quote;8&quote; name&equal;&quote;Label1&quote; scanType&equal;&quote;ScanFixed&quote; refID&equal;&quote;0&quote;><ReadingSettings number&equal;&quote;25&quote; rate&equal;&quote;25000&quote; /><ReadingGain type&equal;&quote;&quote; gain&equal;&quote;40&quote; optimalGainPercentage&equal;&quote;0&quote; automaticGain&equal;&quote;False&quote; mode&equal;&quote;Manual&quote; /><ReadingTime integrationTime&equal;&quote;20&quote; lagTime&equal;&quote;0&quote; readDelay&equal;&quote;0&quote; flash&equal;&quote;0&quote; dark&equal;&quote;0&quote; excitationTime&equal;&quote;0&quote; /><ReadingFilter id&equal;&quote;9&quote; type&equal;&quote;Ex&quote; wavelength&equal;&quote;4880&quote; bandwidth&equal;&quote;90&quote; attenuation&equal;&quote;0&quote; usage&equal;&quote;FI&quote; /><ReadingFilter id&equal;&quote;10&quote; type&equal;&quote;Em&quote; wavelength&equal;&quote;5100&quote; bandwidth&equal;&quote;200&quote; attenuation&equal;&quote;0&quote; usage&equal;&quote;FI&quote; /><ReadingZPosition mode&equal;&quote;Manual&quote; zPosition&equal;&quote;20000&quote; /></ReadingLabel></MeasurementReading></Well></MeasurementFluoInt></PlateRange></CyclePlate></MeasurementManualCycle><MeasurementInfo id&equal;&quote;0&quote; description&equal;&quote;&quote;><ScriptTemplateSettings id&equal;&quote;0&quote;><ScriptTemplateGeneralSettings id&equal;&quote;0&quote; Title&equal;&quote;&quote; Group&equal;&quote;&quote; Info&equal;&quote;&quote; Image&equal;&quote;&quote; /><ScriptTemplateDescriptionSettings id&equal;&quote;0&quote; Internal&equal;&quote;&quote; External&equal;&quote;&quote; IsExternal&equal;&quote;False&quote; /></ScriptTemplateSettings></MeasurementInfo></TecanMeasurement></TecanFile>",
							"labware": "plate1"
						},
						"2": {
							command: "evoware._execute",
							agent: "ourlab.mario.evoware",
							path: "%{ROBOLIQ}",
							args: ["TecanInfinite", "%{SCRIPTFILE}", "1.2", "%{TEMPDIR}\\fluorescence.xml"],
							wait: true
						},
						"command": "equipment.run|ourlab.mario.evoware|ourlab.mario.reader",
						"agent": "ourlab.mario.evoware",
						"equipment": "ourlab.mario.reader",
						"measurementType": "fluorescence",
						"object": "plate1",
						"programFile": "./protocols/tania13_ph-temp.mdfx",
						"outputFile": "1-fluorescence.xml"
					},
					"3": {
						"1": {
							"1": {
								"1": {
									"command": "evoware._facts",
									"agent": "ourlab.mario.evoware",
									"factsEquipment": "ReaderNETwork",
									"factsVariable": "ReaderNETwork_Open"
								},
								"command": "equipment.openSite|ourlab.mario.evoware|ourlab.mario.reader",
								"agent": "ourlab.mario.evoware",
								"equipment": "ourlab.mario.reader",
								"site": "ourlab.mario.site.READER"
							},
							"command": "equipment.openSite",
							"agent": "ourlab.mario.evoware",
							"equipment": "ourlab.mario.reader",
							"site": "ourlab.mario.site.READER"
						},
						"2": {
							"command": "transporter._movePlate",
							"agent": "ourlab.mario.evoware",
							"equipment": "ourlab.mario.roma2",
							"program": "Wide",
							"object": "plate1",
							"destination": "ourlab.mario.site.REGRIP"
						},
						"3": {
							"1": {
								"1": {
									"command": "evoware._facts",
									"agent": "ourlab.mario.evoware",
									"factsEquipment": "ReaderNETwork",
									"factsVariable": "ReaderNETwork_Close"
								},
								"command": "equipment.close|ourlab.mario.evoware|ourlab.mario.reader",
								"agent": "ourlab.mario.evoware",
								"equipment": "ourlab.mario.reader"
							},
							"command": "equipment.close",
							"agent": "ourlab.mario.evoware",
							"equipment": "ourlab.mario.reader"
						},
						"4": {
							"command": "transporter._movePlate",
							"agent": "ourlab.mario.evoware",
							"equipment": "ourlab.mario.roma1",
							"program": "Narrow",
							"object": "plate1",
							"destination": "ourlab.mario.site.P2"
						},
						"command": "transporter.movePlate",
						"object": "plate1",
						"destination": "ourlab.mario.site.P2"
					},
					"command": "fluorescenceReader.measurePlate",
					"object": "plate1",
					"programFile": "./protocols/tania13_ph-temp.mdfx"
				}
			);
		});
	});
});
