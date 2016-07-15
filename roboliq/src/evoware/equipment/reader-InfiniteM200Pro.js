import _ from 'lodash';
import assert from 'assert';
import math from 'mathjs';
import Mustache from 'mustache';
import path from 'path';
import commandHelper from '../../commandHelper.js';
import expect from '../../expect.js';
import wellsParser from '../../parsers/wellsParser.js';

import {makeEvowareExecute, makeEvowareFacts} from './evoware.js';

const templateAbsorbance = `<TecanFile xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="tecan.at.schema.documents Main.xsd" fileformat="Tecan.At.Measurement" fileversion="2.0" xmlns="tecan.at.schema.documents">
		<FileInfo type="" instrument="infinite 200Pro" version="" createdFrom="localadmin" createdAt="{{createdAt}}" createdWith="Tecan.At.XFluor.ReaderEditor.XFluorReaderEditor" description="" />
		<TecanMeasurement id="1" class="Measurement">
				<MeasurementManualCycle id="2" number="1" type="Standard">
						<CyclePlate id="3" file="{{plateFile}}" plateWithCover="{{plateWithCover}}">
								<PlateRange id="4" range="{{wells}}" auto="false">
										<MeasurementAbsorbance id="5" mode="Normal" type="" name="ABS" longname="" description="">
												<Well id="6" auto="true">
														<MeasurementReading id="7" name="" beamDiameter="{{beamDiameter}}" beamGridType="{{beamGridType}}" beamGridSize="{{beamGridSize}}" beamEdgeDistance="{{beamEdgeDistance}}">
																<ReadingLabel id="8" name="Label1" scanType="{{scanType}}" refID="0">
																		<ReadingSettings number="25" rate="25000" />
																		<ReadingTime integrationTime="0" lagTime="0" readDelay="{{readDelay}}" flash="0" dark="0" excitationTime="0" />
																		<ReadingFilter id="0" type="Ex" wavelength="{{excitationWavelength}}" bandwidth="{{excitationBandwidth}}" attenuation="0" usage="ABS" />
																</ReadingLabel>
														</MeasurementReading>
												</Well>
										</MeasurementAbsorbance>
								</PlateRange>
						</CyclePlate>
				</MeasurementManualCycle>
				<MeasurementInfo id="0" description="">
						<ScriptTemplateSettings id="0">
								<ScriptTemplateGeneralSettings id="0" Title="" Group="" Info="" Image="" />
								<ScriptTemplateDescriptionSettings id="0" Internal="" External="" IsExternal="False" />
						</ScriptTemplateSettings>
				</MeasurementInfo>
		</TecanMeasurement>
</TecanFile>`;

const templateShake = `<TecanFile xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="tecan.at.schema.documents Main.xsd" fileformat="Tecan.At.Measurement" fileversion="2.0" xmlns="tecan.at.schema.documents">
		<FileInfo type="" instrument="infinite 200Pro" version="" createdFrom="localadmin" createdAt="{{createdAt}}" createdWith="Tecan.At.XFluor.ReaderEditor.XFluorReaderEditor" description="" />
		<TecanMeasurement id="1" class="Measurement">
				<MeasurementManualCycle id="2" number="1" type="Standard">
						<CyclePlate id="3" file="{{plateFile}}" plateWithCover="{{plateWithCover}}">
								<PlateRange id="4" range="{{wells}}" auto="false">
{{#items}}
										<Shaking id="{{id}}" mode="Orbital" time="{{time}}" frequency="0" amplitude="{{amplitude}}" maxDeviation="PT0S" settleTime="PT0S" />
{{/items}}
								</PlateRange>
						</CyclePlate>
				</MeasurementManualCycle>
				<MeasurementInfo id="0" description="">
						<ScriptTemplateSettings id="0">
								<ScriptTemplateGeneralSettings id="0" Title="" Group="" Info="" Image="" />
								<ScriptTemplateDescriptionSettings id="0" Internal="" External="" IsExternal="False" />
						</ScriptTemplateSettings>
				</MeasurementInfo>
		</TecanMeasurement>
</TecanFile>`;

module.exports = {
	getSchemas: (agentName, equipmentName) => ({
		[`equipment.close|${agentName}|${equipmentName}`]: {
			properties: {
				agent: {description: "Agent identifier", type: "Agent"},
				equipment: {description: "Equipment identifier", type: "Equipment"},
			},
			required: ["agent", "equipment"]
		},
		[`equipment.open|${agentName}|${equipmentName}`]: {
			properties: {
				agent: {description: "Agent identifier", type: "Agent"},
				equipment: {description: "Equipment identifier", type: "Equipment"},
			},
			required: ["agent", "equipment"]
		},
		[`equipment.openSite|${agentName}|${equipmentName}`]: {
			properties: {
				agent: {description: "Agent identifier", type: "Agent"},
				equipment: {description: "Equipment identifier", type: "Equipment"},
				site: {description: "Site identifier", type: "Site"}
			},
			required: ["agent", "equipment", "site"]
		},
		[`equipment.run|${agentName}|${equipmentName}`]: {
			description: "Run measurement on Infinite M200 Pro reader",
			properties: {
				agent: {description: "Agent identifier", type: "Agent"},
				equipment: {description: "Equipment identifier", type: "Equipment"},
				measurementType: {description: "Type of measurement, i.e fluorescence or absorbance", enum: ["fluorescence", "absorbance"]},
				program: {
					description: "Program definition",
					properties: {
						wells: {description: "Array of wells to read", type: "Wells"},
						excitationWavelength: {description: "Excitation wavelength", type: "Length"},
						excitationBandwidth: {description: "Excitation bandwidth", type: "Length"},
						excitationWavelengthMin: {description: "Minimum excitation wavelength for a scan", type: "Length"},
						excitationWavelengthMax: {description: "Maximum excitation wavelength for a scan", type: "Length"},
						excitationWavelengthStep: {description: "Size of steps for a scan", type: "Length"}
					}
				},
				programFile: {description: "Program filename", type: "File"},
				programData: {description: "Program data"},
				object: {description: "The labware being measured", type: "Plate"},
				output: {
					description: "Output definition for where and how to save the measurements",
					properties: {
						joinKey: {description: "The key used to left-join the measurement values with the current DATA", type: "string"},
						writeTo: {description: "Filename to write measured to as JSON", type: "string"},
						appendTo: {description: "Filename to append measured to as newline-delimited JSON", type: "string"},
						userValues: {description: "User-specificed values that should be included in the output table", type: "object"}
					}
				}
			},
			required: ["measurementType"]
		},
		// [`shaker.run|${agentName}|${equipmentName}`]: {
		// 	properties: {
		// 		agent: {description: "Agent identifier", type: "Agent"},
		// 		equipment: {description: "Equipment identifier", type: "Equipment"},
		// 		program: {
		// 			description: "Program for shaking",
		// 			properties: {
		// 				amplitude: {description: "Amplitude", enum: ["min", "low", "high", "max"]},
		// 				duration: {description: "Duration of shaking", type: "Duration"}
		// 			},
		// 			required: ["duration"]
		// 		}
		// 	},
		// 	required: ["agent", "equipment", "program"]
		// },
	}),
	getCommandHandlers: (agentName, equipmentName) => ({
		// Reader
		[`equipment.close|${agentName}|${equipmentName}`]: function(params, parsed, data) {
			return {expansion: [makeEvowareFacts(parsed, data, "Close")]};
		},
		[`equipment.open|${agentName}|${equipmentName}`]: function(params, parsed, data) {
			return {expansion: [makeEvowareFacts(parsed, data, "Open")]};
		},
		[`equipment.openSite|${agentName}|${equipmentName}`]: function(params, parsed, data) {
			var carrier = commandHelper.getParsedValue(parsed, data, "equipment", "evowareId");
			var sitesInternal = commandHelper.getParsedValue(parsed, data, "equipment", "sitesInternal");
			var siteIndex = sitesInternal.indexOf(parsed.objectName.site);
			expect.truthy({paramName: "site"}, siteIndex >= 0, "site must be one of the equipments internal sites: "+sitesInternal.join(", "));

			return {expansion: [makeEvowareFacts(parsed, data, "Open")]};
		},
		[`equipment.run|${agentName}|${equipmentName}`]: function(params, parsed, data) {
			// console.log("reader-InfiniteM200Pro-equipment.run: "+JSON.stringify(parsed, null, '\t'));

			const hasProgram = (parsed.value.program) ? 1 : 0;
			const hasProgramFile = (parsed.value.programFile) ? 1 : 0;
			const hasProgramData = (parsed.value.programData) ? 1 : 0;
			expect.truthy({}, hasProgram + hasProgramFile + hasProgramData >= 1, "either `program`, `programFile` or `programData` must be specified.");
			expect.truthy({}, hasProgram + hasProgramFile + hasProgramData <= 1, "only one of `program`, `programFile` or `programData` may be specified.");

			function locationRowColToText(row, col) {
				var colText = col.toString();
				return String.fromCharCode("A".charCodeAt(0) + row - 1) + colText;
			}

			function getTemplateAbsorbanceParams() {
				const program = parsed.value.program || {};
				const output = parsed.value.output || {};
				// const labwareModelName = parsed.objectName["object.model"];
				const labwareModelName = parsed.value.object.model;
				// console.log({labwareModelName})
				const labwareModel = _.get(data.objects, labwareModelName);
				// console.log({labwareModel})
				const modelToPlateFile = parsed.value.equipment.modelToPlateFile;
				assert(modelToPlateFile, `please define ${parsed.objectName.equipment}.modelToPlateFile`);
				const plateFile = modelToPlateFile[labwareModelName];
				assert(plateFile, `please define ${parsed.objectName.equipment}.plateFile."${labwareModelName}"`);

				let wells;
				const wells0 = (program.wells)
					? commandHelper.asArray(program.wells)
					: (output.joinKey)
						? commandHelper.getDesignFactor(output.joinKey, data.objects.DATA)
						: undefined;
				if (wells0) {
					// console.log({program})
					// Get well list
					const wells1 = _.flatMap(wells0, s => wellsParser.parse(s, data.objects));
					// console.log({wells0})
					const rx = /\(([^)]*)\)/;
					const wells2 = wells1.map(s => {
						const match = s.match(rx);
						return (match) ? match[1] : s;
					});
					// console.log({wells1})
					const rowcols = wells2.map(s => wellsParser.locationTextToRowCol(s));
					// console.log({rowcols})
					// rowcols.sort();
					rowcols.sort((a, b) => (a[0] == b[0]) ? a[1] - b[1] : a[0] - b[0]);
					// console.log({rowcols})

					if (_.isEmpty(rowcols)) {
						wells = "";
					}
					else {
						let prev = rowcols[0];
						let indexOnPlatePrev = (prev[0] - 1) * labwareModel.columns + prev[1];
						wells = locationRowColToText(prev[0], prev[1])+":";
						for (let i = 1; i < rowcols.length; i++) {
							const rowcol = rowcols[i];
							const indexOnPlate = (rowcol[0] - 1) * labwareModel.columns + rowcol[1];
							// If continuity is broken:
							if (indexOnPlate !== indexOnPlatePrev + 1) {
								wells += locationRowColToText(prev[0], prev[1])+"|"+locationRowColToText(rowcol[0], rowcol[1])+":";
							}
							prev = rowcol;
							indexOnPlatePrev = indexOnPlate;
						}
						wells += locationRowColToText(prev[0], prev[1]);
					}
				}
				// If not specified, read all wells on plate
				else {
					wells = "A1:"+locationRowColToText(labwareModel.rows, labwareModel.columns);
				}
				// console.log({wells})

				let excitationWavelength;
				const isScan = (program.excitationWavelengthMin && program.excitationWavelengthMax);
				if (isScan) {
					const excitationWavelengthMin = program.excitationWavelengthMin.toNumber("nm");
					const excitationWavelengthStep0 = program.excitationWavelengthStep || math.unit(2, "nm");
					const excitationWavelengthStep = excitationWavelengthStep0.toNumber("nm");
					const excitationWavelengthMax0 = program.excitationWavelengthMax.toNumber("nm");
					const stepCount = Math.floor((excitationWavelengthMax0 - excitationWavelengthMin) / excitationWavelengthStep);
					const excitationWavelengthMax = excitationWavelengthMin + excitationWavelengthStep * stepCount;
					excitationWavelength = `${excitationWavelengthMin*10}~${excitationWavelengthMax*10}:${excitationWavelengthStep*10}`;
				}
				else {
					// console.log({program})
					excitationWavelength = program.excitationWavelength.toNumber("nm") * 10;
				}
				const excitationBandwidth0 = program.excitationBandwidth || math.unit(9, "nm");
				const excitationBandwidth = excitationBandwidth0.toNumber("nm") * 10;
				const params = {
					//createdAt:	moment().format("YYYY-MM-DDTHH:mm:ss.SSSSSSS")+"Z",
					createdAt: "2016-01-01T00:00:00.0000000Z",
					plateFile,
					plateWithCover: (parsed.value.object.isSealed || parsed.value.object.isCovered) ? "True" : "False",
					wells,
					beamDiameter: (isScan) ? 0 : 500,
					beamGridType: "Single",
					beamGridSize: (isScan) ? 0 : 1,
					beamEdgeDistance: (isScan) ? "" : "auto",
					scanType: (isScan) ? "ScanEX" : "ScanFixed",
					readDelay: (isScan) ? 0 : 10000,
					excitationWavelength,
					excitationBandwidth,
				};
				// console.log({params, excitationWavelength0, excitationWavelength, excitationBandwidth0, excitationBandwidth});
				return params;
			}

			const content
				= (hasProgramData) ? parsed.value.programData.toString('utf8')
				: (hasProgramFile) ? parsed.value.programFile.toString('utf8')
				: (true || measurementType === "absorbance") ? Mustache.render(templateAbsorbance, getTemplateAbsorbanceParams())
				: undefined;
			var start_i = content.indexOf("<TecanFile");
			if (start_i < 0)
				start_i = 0;
			var programData = content.substring(start_i).
				replace(/[\r\n]/g, "").
				replace(/&/g, "&amp;"). // "&amp;" is probably not needed, since I didn't find it in the XML files
				replace(/=/g, "&equal;").
				replace(/"/g, "&quote;").
				replace(/~/, "&tilde;").
				replace(/>[ \t]+</g, "><");

			// Save the file to the agent-configured TEMPDIR, if no absolute path is given
			const outputFile0 = _.get(parsed.value, "output.writeTo") || parsed.value.outputFile || parsed.value.measurementType+".xml";
			const outputFile = (path.win32.isAbsolute(outputFile0))
				? outputFile0
				: "${TEMPDIR}\\" + path.win32.basename(outputFile0);
			const value = outputFile + "|" + programData;
			const args = ["TecanInfinite", "${SCRIPTFILE}", data.path.join("."), outputFile];
			const expansion = [
				makeEvowareFacts(parsed, data, "Measure", value, parsed.objectName.object),
				makeEvowareExecute(parsed.objectName.agent, "${ROBOLIQ}", args, true)
			];
			return {
				expansion,
				reports: (_.isEmpty(data.objects.DATA)) ? undefined : {
					measurementFactors: data.objects.DATA
				}
			};
		},
		// [`shaker.run|${agentName}|${equipmentName}`]: function(params, parsed, data) {
	}),
	getPredicates: (agentName, equipmentName, siteName) => [
		// Open READER
		{"method": {"description": `generic.openSite-${siteName}`,
			"task": {"generic.openSite": {"site": "?site"}},
			"preconditions": [{"same": {"thing1": "?site", "thing2": siteName}}],
			"subtasks": {"ordered": [{[`${equipmentName}.open`]: {}}]}
		}},
		{"action": {"description": `${equipmentName}.open: open the reader`,
			"task": {[`${equipmentName}.open`]: {}},
			"preconditions": [],
			"deletions": [{"siteIsClosed": {"site": siteName}}],
			"additions": []
		}},
		// Close READER
		{"method": {"description": `generic.closeSite-${siteName}`,
			"task": {"generic.closeSite": {"site": "?site"}},
			"preconditions": [{"same": {"thing1": "?site", "thing2": siteName}}],
			"subtasks": {"ordered": [{[`${equipmentName}.close`]: {}}]}
		}},
		{"action": {"description": `${equipmentName}.close: close the reader`,
			"task": {[`${equipmentName}.close`]: {}},
			"preconditions": [],
			"deletions": [],
			"additions": [
				{"siteIsClosed": {"site": siteName}}
			]
		}},
	],
	getPlanHandlers: (agentName, equipmentName, siteName) => ({
		[`${equipmentName}.close`]: function(params, parentParams, data) {
			return [{
				command: "equipment.close",
				agent: agentName,
				equipment: equipmentName
			}];
		},
		[`${equipmentName}.open`]: function(params, parentParams, data) {
			return [{
				command: "equipment.openSite",
				agent: agentName,
				equipment: equipmentName,
				site: siteName
			}];
		},
	})
};
