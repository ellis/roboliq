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
							excitationWavelength: "600nm",
							wells: "plate1(A1 right A12, B1, B3 right B12, C3)"
						},
						output: {
							writeTo: "measurement.xml"
						}
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
							"factsValue": "${TEMPDIR}\\measurement.xml|<TecanFile xmlns:xsi&equal;&quote;http://www.w3.org/2001/XMLSchema-instance&quote; xsi:schemaLocation&equal;&quote;tecan.at.schema.documents Main.xsd&quote; fileformat&equal;&quote;Tecan.At.Measurement&quote; fileversion&equal;&quote;2.0&quote; xmlns&equal;&quote;tecan.at.schema.documents&quote;><FileInfo type&equal;&quote;&quote; instrument&equal;&quote;infinite 200Pro&quote; version&equal;&quote;&quote; createdFrom&equal;&quote;localadmin&quote; createdAt&equal;&quote;2016-01-01T00:00:00.0000000Z&quote; createdWith&equal;&quote;Tecan.At.XFluor.ReaderEditor.XFluorReaderEditor&quote; description&equal;&quote;&quote; /><TecanMeasurement id&equal;&quote;1&quote; class&equal;&quote;Measurement&quote;><MeasurementManualCycle id&equal;&quote;2&quote; number&equal;&quote;1&quote; type&equal;&quote;Standard&quote;><CyclePlate id&equal;&quote;3&quote; file&equal;&quote;Which One?&quote; plateWithCover&equal;&quote;False&quote;><PlateRange id&equal;&quote;4&quote; range&equal;&quote;A1:A12|B1:B1|B3:B12|C3:C3&quote; auto&equal;&quote;false&quote;><MeasurementAbsorbance id&equal;&quote;5&quote; mode&equal;&quote;Normal&quote; type&equal;&quote;&quote; name&equal;&quote;ABS&quote; longname&equal;&quote;&quote; description&equal;&quote;&quote;><Well id&equal;&quote;6&quote; auto&equal;&quote;true&quote;><MeasurementReading id&equal;&quote;7&quote; name&equal;&quote;&quote; beamDiameter&equal;&quote;500&quote; beamGridType&equal;&quote;Single&quote; beamGridSize&equal;&quote;1&quote; beamEdgeDistance&equal;&quote;auto&quote;><ReadingLabel id&equal;&quote;8&quote; name&equal;&quote;Label1&quote; scanType&equal;&quote;ScanFixed&quote; refID&equal;&quote;0&quote;><ReadingSettings number&equal;&quote;25&quote; rate&equal;&quote;25000&quote; /><ReadingTime integrationTime&equal;&quote;0&quote; lagTime&equal;&quote;0&quote; readDelay&equal;&quote;10000&quote; flash&equal;&quote;0&quote; dark&equal;&quote;0&quote; excitationTime&equal;&quote;0&quote; /><ReadingFilter id&equal;&quote;0&quote; type&equal;&quote;Ex&quote; wavelength&equal;&quote;6000&quote; bandwidth&equal;&quote;90&quote; attenuation&equal;&quote;0&quote; usage&equal;&quote;ABS&quote; /></ReadingLabel></MeasurementReading></Well></MeasurementAbsorbance></PlateRange></CyclePlate></MeasurementManualCycle><MeasurementInfo id&equal;&quote;0&quote; description&equal;&quote;&quote;><ScriptTemplateSettings id&equal;&quote;0&quote;><ScriptTemplateGeneralSettings id&equal;&quote;0&quote; Title&equal;&quote;&quote; Group&equal;&quote;&quote; Info&equal;&quote;&quote; Image&equal;&quote;&quote; /><ScriptTemplateDescriptionSettings id&equal;&quote;0&quote; Internal&equal;&quote;&quote; External&equal;&quote;&quote; IsExternal&equal;&quote;False&quote; /></ScriptTemplateSettings></MeasurementInfo></TecanMeasurement></TecanFile>",
							"labware": "plate1"
						},
						"2": {
							command: "evoware._execute",
							agent: "ourlab.mario.evoware",
							path: "${ROBOLIQ}",
							args: ["TecanInfinite", "${SCRIPTFILE}", "1.1", "${TEMPDIR}\\measurement.xml"],
							wait: true
						},
						"command": "equipment.run|ourlab.mario.evoware|ourlab.mario.reader",
						"agent": "ourlab.mario.evoware",
						"equipment": "ourlab.mario.reader",
						"measurementType": "absorbance",
						"object": "plate1",
						"program": {
							"excitationWavelength": "600nm",
							wells: [
								'plate1(A01)', 'plate1(A02)', 'plate1(A03)', 'plate1(A04)', 'plate1(A05)', 'plate1(A06)', 'plate1(A07)', 'plate1(A08)', 'plate1(A09)', 'plate1(A10)', 'plate1(A11)', 'plate1(A12)',
								'plate1(B01)',
								'plate1(B03)', 'plate1(B04)', 'plate1(B05)', 'plate1(B06)', 'plate1(B07)', 'plate1(B08)', 'plate1(B09)', 'plate1(B10)', 'plate1(B11)', 'plate1(B12)',
								'plate1(C03)'
							]
						},
						output: {
							writeTo: "measurement.xml"
						}
					},
					"command": "absorbanceReader.measurePlate",
					"object": "plate1",
					"program": {"excitationWavelength": "600nm", wells: "plate1(A1 right A12, B1, B3 right B12, C3)"},
					output: { writeTo: "measurement.xml" }
				}
			);
			should.not.exist(result.output.reports[1]);
		});

		it('should measure an absorbance spectrum', function () {
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
							excitationWavelengthMin: "300nm",
							excitationWavelengthMax: "900nm",
							excitationWavelengthStep: "100nm",
							wells: "plate1(A1)"
						}
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
							"factsValue": "${TEMPDIR}\\absorbance.xml|<TecanFile xmlns:xsi&equal;&quote;http://www.w3.org/2001/XMLSchema-instance&quote; xsi:schemaLocation&equal;&quote;tecan.at.schema.documents Main.xsd&quote; fileformat&equal;&quote;Tecan.At.Measurement&quote; fileversion&equal;&quote;2.0&quote; xmlns&equal;&quote;tecan.at.schema.documents&quote;><FileInfo type&equal;&quote;&quote; instrument&equal;&quote;infinite 200Pro&quote; version&equal;&quote;&quote; createdFrom&equal;&quote;localadmin&quote; createdAt&equal;&quote;2016-01-01T00:00:00.0000000Z&quote; createdWith&equal;&quote;Tecan.At.XFluor.ReaderEditor.XFluorReaderEditor&quote; description&equal;&quote;&quote; /><TecanMeasurement id&equal;&quote;1&quote; class&equal;&quote;Measurement&quote;><MeasurementManualCycle id&equal;&quote;2&quote; number&equal;&quote;1&quote; type&equal;&quote;Standard&quote;><CyclePlate id&equal;&quote;3&quote; file&equal;&quote;Which One?&quote; plateWithCover&equal;&quote;False&quote;><PlateRange id&equal;&quote;4&quote; range&equal;&quote;A1:A1&quote; auto&equal;&quote;false&quote;><MeasurementAbsorbance id&equal;&quote;5&quote; mode&equal;&quote;Normal&quote; type&equal;&quote;&quote; name&equal;&quote;ABS&quote; longname&equal;&quote;&quote; description&equal;&quote;&quote;><Well id&equal;&quote;6&quote; auto&equal;&quote;true&quote;><MeasurementReading id&equal;&quote;7&quote; name&equal;&quote;&quote; beamDiameter&equal;&quote;0&quote; beamGridType&equal;&quote;Single&quote; beamGridSize&equal;&quote;0&quote; beamEdgeDistance&equal;&quote;&quote;><ReadingLabel id&equal;&quote;8&quote; name&equal;&quote;Label1&quote; scanType&equal;&quote;ScanEX&quote; refID&equal;&quote;0&quote;><ReadingSettings number&equal;&quote;25&quote; rate&equal;&quote;25000&quote; /><ReadingTime integrationTime&equal;&quote;0&quote; lagTime&equal;&quote;0&quote; readDelay&equal;&quote;0&quote; flash&equal;&quote;0&quote; dark&equal;&quote;0&quote; excitationTime&equal;&quote;0&quote; /><ReadingFilter id&equal;&quote;0&quote; type&equal;&quote;Ex&quote; wavelength&equal;&quote;3000&tilde;9000:1000&quote; bandwidth&equal;&quote;90&quote; attenuation&equal;&quote;0&quote; usage&equal;&quote;ABS&quote; /></ReadingLabel></MeasurementReading></Well></MeasurementAbsorbance></PlateRange></CyclePlate></MeasurementManualCycle><MeasurementInfo id&equal;&quote;0&quote; description&equal;&quote;&quote;><ScriptTemplateSettings id&equal;&quote;0&quote;><ScriptTemplateGeneralSettings id&equal;&quote;0&quote; Title&equal;&quote;&quote; Group&equal;&quote;&quote; Info&equal;&quote;&quote; Image&equal;&quote;&quote; /><ScriptTemplateDescriptionSettings id&equal;&quote;0&quote; Internal&equal;&quote;&quote; External&equal;&quote;&quote; IsExternal&equal;&quote;False&quote; /></ScriptTemplateSettings></MeasurementInfo></TecanMeasurement></TecanFile>",
							"labware": "plate1"
						},
						"2": {
							command: "evoware._execute",
							agent: "ourlab.mario.evoware",
							path: "${ROBOLIQ}",
							args: ["TecanInfinite", "${SCRIPTFILE}", "1.1", "${TEMPDIR}\\absorbance.xml"],
							wait: true
						},
						"command": "equipment.run|ourlab.mario.evoware|ourlab.mario.reader",
						"agent": "ourlab.mario.evoware",
						"equipment": "ourlab.mario.reader",
						"measurementType": "absorbance",
						"object": "plate1",
						"program": {
							"excitationWavelengthMin": "300nm",
							"excitationWavelengthMax": "900nm",
							"excitationWavelengthStep": "100nm",
							wells: [
								'plate1(A01)'
							]
						}
					},
					"command": "absorbanceReader.measurePlate",
					"object": "plate1",
					"program": {"excitationWavelengthMin": "300nm", "excitationWavelengthMax": "900nm", "excitationWavelengthStep": "100nm", wells: "plate1(A1)"}
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
							excitationWavelength: "600 nm"
						}
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
							"factsValue": "${TEMPDIR}\\absorbance.xml|<TecanFile xmlns:xsi&equal;&quote;http://www.w3.org/2001/XMLSchema-instance&quote; xsi:schemaLocation&equal;&quote;tecan.at.schema.documents Main.xsd&quote; fileformat&equal;&quote;Tecan.At.Measurement&quote; fileversion&equal;&quote;2.0&quote; xmlns&equal;&quote;tecan.at.schema.documents&quote;><FileInfo type&equal;&quote;&quote; instrument&equal;&quote;infinite 200Pro&quote; version&equal;&quote;&quote; createdFrom&equal;&quote;localadmin&quote; createdAt&equal;&quote;2016-01-01T00:00:00.0000000Z&quote; createdWith&equal;&quote;Tecan.At.XFluor.ReaderEditor.XFluorReaderEditor&quote; description&equal;&quote;&quote; /><TecanMeasurement id&equal;&quote;1&quote; class&equal;&quote;Measurement&quote;><MeasurementManualCycle id&equal;&quote;2&quote; number&equal;&quote;1&quote; type&equal;&quote;Standard&quote;><CyclePlate id&equal;&quote;3&quote; file&equal;&quote;Which One?&quote; plateWithCover&equal;&quote;False&quote;><PlateRange id&equal;&quote;4&quote; range&equal;&quote;A1:P24&quote; auto&equal;&quote;false&quote;><MeasurementAbsorbance id&equal;&quote;5&quote; mode&equal;&quote;Normal&quote; type&equal;&quote;&quote; name&equal;&quote;ABS&quote; longname&equal;&quote;&quote; description&equal;&quote;&quote;><Well id&equal;&quote;6&quote; auto&equal;&quote;true&quote;><MeasurementReading id&equal;&quote;7&quote; name&equal;&quote;&quote; beamDiameter&equal;&quote;500&quote; beamGridType&equal;&quote;Single&quote; beamGridSize&equal;&quote;1&quote; beamEdgeDistance&equal;&quote;auto&quote;><ReadingLabel id&equal;&quote;8&quote; name&equal;&quote;Label1&quote; scanType&equal;&quote;ScanFixed&quote; refID&equal;&quote;0&quote;><ReadingSettings number&equal;&quote;25&quote; rate&equal;&quote;25000&quote; /><ReadingTime integrationTime&equal;&quote;0&quote; lagTime&equal;&quote;0&quote; readDelay&equal;&quote;10000&quote; flash&equal;&quote;0&quote; dark&equal;&quote;0&quote; excitationTime&equal;&quote;0&quote; /><ReadingFilter id&equal;&quote;0&quote; type&equal;&quote;Ex&quote; wavelength&equal;&quote;6000&quote; bandwidth&equal;&quote;90&quote; attenuation&equal;&quote;0&quote; usage&equal;&quote;ABS&quote; /></ReadingLabel></MeasurementReading></Well></MeasurementAbsorbance></PlateRange></CyclePlate></MeasurementManualCycle><MeasurementInfo id&equal;&quote;0&quote; description&equal;&quote;&quote;><ScriptTemplateSettings id&equal;&quote;0&quote;><ScriptTemplateGeneralSettings id&equal;&quote;0&quote; Title&equal;&quote;&quote; Group&equal;&quote;&quote; Info&equal;&quote;&quote; Image&equal;&quote;&quote; /><ScriptTemplateDescriptionSettings id&equal;&quote;0&quote; Internal&equal;&quote;&quote; External&equal;&quote;&quote; IsExternal&equal;&quote;False&quote; /></ScriptTemplateSettings></MeasurementInfo></TecanMeasurement></TecanFile>",
							"labware": "plate1"
						},
						"2": {
							command: "evoware._execute",
							agent: "ourlab.mario.evoware",
							path: "${ROBOLIQ}",
							args: ["TecanInfinite", "${SCRIPTFILE}", "1.2", "${TEMPDIR}\\absorbance.xml"],
							wait: true
						},
						"command": "equipment.run|ourlab.mario.evoware|ourlab.mario.reader",
						"agent": "ourlab.mario.evoware",
						"equipment": "ourlab.mario.reader",
						"measurementType": "absorbance",
						"object": "plate1",
						"program": {"excitationWavelength": "600 nm"},
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
					"program": {"excitationWavelength": "600 nm"}
				}
			);
		});

		it('should produce a report for measurement factors', function () {
			var protocol = _.merge({}, protocol0, {
				objects: {
					plate1: {
						type: "Plate",
						model: "ourlab.model.plateModel_384_square",
						location: "ourlab.mario.site.READER"
					},
					design1: {
						type: "Design",
						conditions: {a: 1, testWell: "A1"}
					}
				},
				steps: {
					data: {source: "design1"},
					1: {
						command: "absorbanceReader.measurePlate",
						object: "plate1",
						program: {
							excitationWavelength: "600nm"
						},
						output: {
							joinKey: "testWell",
							appendTo: "absorbance"
						}
					}
				}
			});
			var result = roboliq.run(["-o", "", "-T"], protocol);
			//console.log("result:\n"+JSON.stringify(result, null, '\t'))
			should.deepEqual(result.protocol.errors, {});
			should.deepEqual(result.protocol.warnings, {});
			// console.log("result:\n"+JSON.stringify(result.output.steps[1], null, '\t'))
			should.deepEqual(result.output.steps[1], {
				"1": {
					"1": {
						"command": "evoware._facts",
						"agent": "ourlab.mario.evoware",
						"factsEquipment": "ReaderNETwork",
						"factsVariable": "ReaderNETwork_Measure",
						"factsValue": "${TEMPDIR}\\absorbance.xml|<TecanFile xmlns:xsi&equal;&quote;http://www.w3.org/2001/XMLSchema-instance&quote; xsi:schemaLocation&equal;&quote;tecan.at.schema.documents Main.xsd&quote; fileformat&equal;&quote;Tecan.At.Measurement&quote; fileversion&equal;&quote;2.0&quote; xmlns&equal;&quote;tecan.at.schema.documents&quote;><FileInfo type&equal;&quote;&quote; instrument&equal;&quote;infinite 200Pro&quote; version&equal;&quote;&quote; createdFrom&equal;&quote;localadmin&quote; createdAt&equal;&quote;2016-01-01T00:00:00.0000000Z&quote; createdWith&equal;&quote;Tecan.At.XFluor.ReaderEditor.XFluorReaderEditor&quote; description&equal;&quote;&quote; /><TecanMeasurement id&equal;&quote;1&quote; class&equal;&quote;Measurement&quote;><MeasurementManualCycle id&equal;&quote;2&quote; number&equal;&quote;1&quote; type&equal;&quote;Standard&quote;><CyclePlate id&equal;&quote;3&quote; file&equal;&quote;Which One?&quote; plateWithCover&equal;&quote;False&quote;><PlateRange id&equal;&quote;4&quote; range&equal;&quote;A1:A1&quote; auto&equal;&quote;false&quote;><MeasurementAbsorbance id&equal;&quote;5&quote; mode&equal;&quote;Normal&quote; type&equal;&quote;&quote; name&equal;&quote;ABS&quote; longname&equal;&quote;&quote; description&equal;&quote;&quote;><Well id&equal;&quote;6&quote; auto&equal;&quote;true&quote;><MeasurementReading id&equal;&quote;7&quote; name&equal;&quote;&quote; beamDiameter&equal;&quote;500&quote; beamGridType&equal;&quote;Single&quote; beamGridSize&equal;&quote;1&quote; beamEdgeDistance&equal;&quote;auto&quote;><ReadingLabel id&equal;&quote;8&quote; name&equal;&quote;Label1&quote; scanType&equal;&quote;ScanFixed&quote; refID&equal;&quote;0&quote;><ReadingSettings number&equal;&quote;25&quote; rate&equal;&quote;25000&quote; /><ReadingTime integrationTime&equal;&quote;0&quote; lagTime&equal;&quote;0&quote; readDelay&equal;&quote;10000&quote; flash&equal;&quote;0&quote; dark&equal;&quote;0&quote; excitationTime&equal;&quote;0&quote; /><ReadingFilter id&equal;&quote;0&quote; type&equal;&quote;Ex&quote; wavelength&equal;&quote;6000&quote; bandwidth&equal;&quote;90&quote; attenuation&equal;&quote;0&quote; usage&equal;&quote;ABS&quote; /></ReadingLabel></MeasurementReading></Well></MeasurementAbsorbance></PlateRange></CyclePlate></MeasurementManualCycle><MeasurementInfo id&equal;&quote;0&quote; description&equal;&quote;&quote;><ScriptTemplateSettings id&equal;&quote;0&quote;><ScriptTemplateGeneralSettings id&equal;&quote;0&quote; Title&equal;&quote;&quote; Group&equal;&quote;&quote; Info&equal;&quote;&quote; Image&equal;&quote;&quote; /><ScriptTemplateDescriptionSettings id&equal;&quote;0&quote; Internal&equal;&quote;&quote; External&equal;&quote;&quote; IsExternal&equal;&quote;False&quote; /></ScriptTemplateSettings></MeasurementInfo></TecanMeasurement></TecanFile>",
						"labware": "plate1"
					},
					"2": {
						command: "evoware._execute",
						agent: "ourlab.mario.evoware",
						path: "${ROBOLIQ}",
						args: ["TecanInfinite", "${SCRIPTFILE}", "1.1", "${TEMPDIR}\\absorbance.xml"],
						wait: true
					},
					"command": "equipment.run|ourlab.mario.evoware|ourlab.mario.reader",
					"agent": "ourlab.mario.evoware",
					"equipment": "ourlab.mario.reader",
					"measurementType": "absorbance",
					"program": {
						"excitationWavelength": "600nm"
					},
					"output": {
						"joinKey": "testWell",
						"appendTo": "absorbance"
					},
					"object": "plate1"
				},
				"command": "absorbanceReader.measurePlate",
				"object": "plate1",
				"program": {
					"excitationWavelength": "600nm"
				},
				"output": {
					"joinKey": "testWell",
					"appendTo": "absorbance"
				}
			});
			// console.log("reports: "+JSON.stringify(result.output.reports, null, '\t'));
			should.deepEqual(result.output.reports["1.1"], {
				"measurementFactors": [
					{ "a": 1, "testWell": "A1" }
				]
			});
		});

	});
});
