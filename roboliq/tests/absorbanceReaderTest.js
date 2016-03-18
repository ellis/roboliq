var _ = require('lodash');
var should = require('should');
var roboliq = require('../src/roboliq.js')

describe('commands/absorbanceReader', function() {
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

	describe('absorbanceReader.measurePlate', function () {
		it('should measure specific wells', function () {
			var protocol = _.merge({}, protocol0, {
				objects: {
					plate1: {
						type: "Plate",
						model: "ourlab.model.plateModel_384_square",
						location: "ourlab.mario.site.READER"
					}
				},
				steps: {
					1: {
						command: "absorbanceReader.measurePlate",
						object: "plate1",
						program: {
							excitationWavelength: 6000,
							wells: "plate1(A1 right A12, B1, B3 right B12, C3)"
						},
						outputFile: "C:\\Users\\localadmin\\Desktop\\Ellis\\tania13_ph--<YYYYMMDD_HHmmss>.xml"
					}
				}
			});
			var result = roboliq.run(["-o", "", "-T"], protocol);
			//console.log("result:\n"+JSON.stringify(result, null, '\t'))
			should.deepEqual(result.protocol.errors, {});
			should.deepEqual(result.protocol.warnings, {});
			// console.log("result:\n"+JSON.stringify(result.output.steps[1], null, '\t'))
			should.deepEqual(result.output.steps[1],
				{
					"1": {
						"1": {
							"command": "evoware._facts",
							"agent": "ourlab.mario.evoware",
							"factsEquipment": "ReaderNETwork",
							"factsVariable": "ReaderNETwork_Measure",
							"factsValue": "C:\\Users\\localadmin\\Desktop\\Ellis\\tania13_ph--<YYYYMMDD_HHmmss>.xml|<TecanFile xmlns:xsi&equal;&quote;http://www.w3.org/2001/XMLSchema-instance&quote; xsi:schemaLocation&equal;&quote;tecan.at.schema.documents Main.xsd&quote; fileformat&equal;&quote;Tecan.At.Measurement&quote; fileversion&equal;&quote;2.0&quote; xmlns&equal;&quote;tecan.at.schema.documents&quote;><FileInfo type&equal;&quote;&quote; instrument&equal;&quote;infinite 200Pro&quote; version&equal;&quote;&quote; createdFrom&equal;&quote;localadmin&quote; createdAt&equal;&quote;2015-08-20T07:22:39.4678927Z&quote; createdWith&equal;&quote;Tecan.At.XFluor.ReaderEditor.XFluorReaderEditor&quote; description&equal;&quote;&quote; /><TecanMeasurement id&equal;&quote;1&quote; class&equal;&quote;Measurement&quote;><MeasurementManualCycle id&equal;&quote;2&quote; number&equal;&quote;1&quote; type&equal;&quote;Standard&quote;><CyclePlate id&equal;&quote;3&quote; file&equal;&quote;Which One?&quote; plateWithCover&equal;&quote;False&quote;><PlateRange id&equal;&quote;4&quote; range&equal;&quote;A1:A1|A2:A2|A3:A3|A4:A4|A5:A5|A6:A6|A7:A7|A8:A8|A9:A9|A10:A10|A11:A11|A12:A12|B1:B1|B3:B3|B4:B4|B5:B5|B6:B6|B7:B7|B8:B8|B9:B9|B10:B10|B11:B11|B12:B12|C3:C3&quote; auto&equal;&quote;false&quote;><MeasurementAbsorbance id&equal;&quote;5&quote; mode&equal;&quote;Normal&quote; type&equal;&quote;&quote; name&equal;&quote;ABS&quote; longname&equal;&quote;&quote; description&equal;&quote;&quote;><Well id&equal;&quote;6&quote; auto&equal;&quote;true&quote;><MeasurementReading id&equal;&quote;7&quote; name&equal;&quote;&quote; beamDiameter&equal;&quote;500&quote; beamGridType&equal;&quote;Single&quote; beamGridSize&equal;&quote;1&quote; beamEdgeDistance&equal;&quote;auto&quote;><ReadingLabel id&equal;&quote;8&quote; name&equal;&quote;Label1&quote; scanType&equal;&quote;ScanFixed&quote; refID&equal;&quote;0&quote;><ReadingSettings number&equal;&quote;25&quote; rate&equal;&quote;25000&quote; /><ReadingTime integrationTime&equal;&quote;0&quote; lagTime&equal;&quote;0&quote; readDelay&equal;&quote;10000&quote; flash&equal;&quote;0&quote; dark&equal;&quote;0&quote; excitationTime&equal;&quote;0&quote; /><ReadingFilter id&equal;&quote;0&quote; type&equal;&quote;Ex&quote; wavelength&equal;&quote;6000&quote; bandwidth&equal;&quote;90&quote; attenuation&equal;&quote;0&quote; usage&equal;&quote;ABS&quote; /></ReadingLabel></MeasurementReading></Well><CustomData id&equal;&quote;9&quote; /></MeasurementAbsorbance></PlateRange></CyclePlate></MeasurementManualCycle><MeasurementInfo id&equal;&quote;0&quote; description&equal;&quote;&quote;><ScriptTemplateSettings id&equal;&quote;0&quote;><ScriptTemplateGeneralSettings id&equal;&quote;0&quote; Title&equal;&quote;&quote; Group&equal;&quote;&quote; Info&equal;&quote;&quote; Image&equal;&quote;&quote; /><ScriptTemplateDescriptionSettings id&equal;&quote;0&quote; Internal&equal;&quote;&quote; External&equal;&quote;&quote; IsExternal&equal;&quote;False&quote; /></ScriptTemplateSettings></MeasurementInfo></TecanMeasurement></TecanFile>"
						},
						"command": "equipment.run|ourlab.mario.evoware|ourlab.mario.reader",
						"agent": "ourlab.mario.evoware",
						"equipment": "ourlab.mario.reader",
						"measurementType": "absorbance",
						"object": "plate1",
						"program": {"excitationWavelength": 6000, wells: "plate1(A1 right A12, B1, B3 right B12, C3)"},
						"outputFile": "C:\\Users\\localadmin\\Desktop\\Ellis\\tania13_ph--<YYYYMMDD_HHmmss>.xml"
					},
					"command": "absorbanceReader.measurePlate",
					"object": "plate1",
					"program": {"excitationWavelength": 6000, wells: "plate1(A1 right A12, B1, B3 right B12, C3)"},
					"outputFile": "C:\\Users\\localadmin\\Desktop\\Ellis\\tania13_ph--<YYYYMMDD_HHmmss>.xml"
				}
			);
		});

		it('should move plate to reader, measure, then move plate back to original location', function () {
			var protocol = _.merge({}, protocol0, {
				steps: {
					1: {
						command: "absorbanceReader.measurePlate",
						object: "plate1",
						program: {
							excitationWavelength: 6000
						},
						outputFile: "C:\\Users\\localadmin\\Desktop\\Ellis\\tania13_ph--<YYYYMMDD_HHmmss>.xml"
					}
				}
			});
			var result = roboliq.run(["-o", "", "-T"], protocol);
			//console.log("result:\n"+JSON.stringify(result, null, '\t'))
			should.deepEqual(result.protocol.errors, {});
			should.deepEqual(result.protocol.warnings, {});
			// console.log("result:\n"+JSON.stringify(result.output.steps[1], null, '\t'))
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
							"factsValue": "C:\\Users\\localadmin\\Desktop\\Ellis\\tania13_ph--<YYYYMMDD_HHmmss>.xml|<TecanFile xmlns:xsi&equal;&quote;http://www.w3.org/2001/XMLSchema-instance&quote; xsi:schemaLocation&equal;&quote;tecan.at.schema.documents Main.xsd&quote; fileformat&equal;&quote;Tecan.At.Measurement&quote; fileversion&equal;&quote;2.0&quote; xmlns&equal;&quote;tecan.at.schema.documents&quote;><FileInfo type&equal;&quote;&quote; instrument&equal;&quote;infinite 200Pro&quote; version&equal;&quote;&quote; createdFrom&equal;&quote;localadmin&quote; createdAt&equal;&quote;2015-08-20T07:22:39.4678927Z&quote; createdWith&equal;&quote;Tecan.At.XFluor.ReaderEditor.XFluorReaderEditor&quote; description&equal;&quote;&quote; /><TecanMeasurement id&equal;&quote;1&quote; class&equal;&quote;Measurement&quote;><MeasurementManualCycle id&equal;&quote;2&quote; number&equal;&quote;1&quote; type&equal;&quote;Standard&quote;><CyclePlate id&equal;&quote;3&quote; file&equal;&quote;Which One?&quote; plateWithCover&equal;&quote;False&quote;><PlateRange id&equal;&quote;4&quote; range&equal;&quote;A1:P24&quote; auto&equal;&quote;false&quote;><MeasurementAbsorbance id&equal;&quote;5&quote; mode&equal;&quote;Normal&quote; type&equal;&quote;&quote; name&equal;&quote;ABS&quote; longname&equal;&quote;&quote; description&equal;&quote;&quote;><Well id&equal;&quote;6&quote; auto&equal;&quote;true&quote;><MeasurementReading id&equal;&quote;7&quote; name&equal;&quote;&quote; beamDiameter&equal;&quote;500&quote; beamGridType&equal;&quote;Single&quote; beamGridSize&equal;&quote;1&quote; beamEdgeDistance&equal;&quote;auto&quote;><ReadingLabel id&equal;&quote;8&quote; name&equal;&quote;Label1&quote; scanType&equal;&quote;ScanFixed&quote; refID&equal;&quote;0&quote;><ReadingSettings number&equal;&quote;25&quote; rate&equal;&quote;25000&quote; /><ReadingTime integrationTime&equal;&quote;0&quote; lagTime&equal;&quote;0&quote; readDelay&equal;&quote;10000&quote; flash&equal;&quote;0&quote; dark&equal;&quote;0&quote; excitationTime&equal;&quote;0&quote; /><ReadingFilter id&equal;&quote;0&quote; type&equal;&quote;Ex&quote; wavelength&equal;&quote;6000&quote; bandwidth&equal;&quote;90&quote; attenuation&equal;&quote;0&quote; usage&equal;&quote;ABS&quote; /></ReadingLabel></MeasurementReading></Well><CustomData id&equal;&quote;9&quote; /></MeasurementAbsorbance></PlateRange></CyclePlate></MeasurementManualCycle><MeasurementInfo id&equal;&quote;0&quote; description&equal;&quote;&quote;><ScriptTemplateSettings id&equal;&quote;0&quote;><ScriptTemplateGeneralSettings id&equal;&quote;0&quote; Title&equal;&quote;&quote; Group&equal;&quote;&quote; Info&equal;&quote;&quote; Image&equal;&quote;&quote; /><ScriptTemplateDescriptionSettings id&equal;&quote;0&quote; Internal&equal;&quote;&quote; External&equal;&quote;&quote; IsExternal&equal;&quote;False&quote; /></ScriptTemplateSettings></MeasurementInfo></TecanMeasurement></TecanFile>"
						},
						"command": "equipment.run|ourlab.mario.evoware|ourlab.mario.reader",
						"agent": "ourlab.mario.evoware",
						"equipment": "ourlab.mario.reader",
						"measurementType": "absorbance",
						"object": "plate1",
						"program": {"excitationWavelength": 6000},
						"outputFile": "C:\\Users\\localadmin\\Desktop\\Ellis\\tania13_ph--<YYYYMMDD_HHmmss>.xml"
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
					"command": "absorbanceReader.measurePlate",
					"object": "plate1",
					"program": {"excitationWavelength": 6000},
					"outputFile": "C:\\Users\\localadmin\\Desktop\\Ellis\\tania13_ph--<YYYYMMDD_HHmmss>.xml"
				}
			);
		});
	});
});
