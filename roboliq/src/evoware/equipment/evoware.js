import _ from 'lodash';
import commandHelper from '../../commandHelper.js';
import expect from '../../expect.js';

function makeEvowareFacts(parsed, data, variable, value) {
	const equipmentId = commandHelper.getParsedValue(parsed, data, "equipment", "evowareId");
	const result2 = {
		command: "evoware._facts",
		agent: parsed.objectName.agent,
		factsEquipment: equipmentId,
		factsVariable: equipmentId+"_"+variable
	};
	const value2 = (_.isFunction(value))
		? value(parsed, data)
		: value;
	return _.merge(result2, {factsValue: value2});
}

/**
 * Expect spec of this form:
 * ``{siteModel: string, sites: [string], labwareModels: [string]}``
 */
function makeSiteModelPredicates(spec) {
	return _.flatten([
		{isSiteModel: {model: spec.siteModel}},
		_.map(spec.sites, site => ({siteModel: {site, siteModel: spec.siteModel}})),
		_.map(spec.labwareModels, labwareModel => ({stackable: {below: spec.siteModel, above: labwareModel}}))
	]);
}

/**
 * Expect specs of this form:
 * ``{<transporter>: {<program>: [site names]}}``
 */
function makeTransporterPredicates(namespaceName, agentName, specs) {
	let siteCliqueId = 1;
	const l = [];
	_.forEach(specs, (programs, equipment) => {
		_.forEach(programs, (cliques, program) => {
			_.forEach(cliques, (sites) => {
				const siteClique = `${namespaceName}.siteClique${siteCliqueId}`;
				siteCliqueId++;
				_.forEach(sites, site => {
					l.push({"siteCliqueSite": {siteClique, site}});
				});
				l.push({
					"transporter.canAgentEquipmentProgramSites": {
						"agent": agentName,
						equipment,
						program,
						siteClique
					}
				});
			});
		});
	});
	return l;
}



const templateAbsorbance = `<TecanFile xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="tecan.at.schema.documents Main.xsd" fileformat="Tecan.At.Measurement" fileversion="2.0" xmlns="tecan.at.schema.documents">
    <FileInfo type="" instrument="infinite 200Pro" version="" createdFrom="localadmin" createdAt="2015-08-20T07:22:39.4678927Z" createdWith="Tecan.At.XFluor.ReaderEditor.XFluorReaderEditor" description="" />
    <TecanMeasurement id="1" class="Measurement">
        <MeasurementManualCycle id="2" number="1" type="Standard">
            <CyclePlate id="3" file="{{plateFile}}" plateWithCover="False">
                <PlateRange id="4" range="{{wells}}" auto="false">
                    <MeasurementAbsorbance id="5" mode="Normal" type="" name="ABS" longname="" description="">
                        <Well id="6" auto="true">
                            <MeasurementReading id="7" name="" beamDiameter="500" beamGridType="Single" beamGridSize="1" beamEdgeDistance="auto">
                                <ReadingLabel id="8" name="Label1" scanType="ScanFixed" refID="0">
                                    <ReadingSettings number="25" rate="25000" />
                                    <ReadingTime integrationTime="0" lagTime="0" readDelay="10000" flash="0" dark="0" excitationTime="0" />
                                    <ReadingFilter id="0" type="Ex" wavelength="{{excitationWavelength}}" bandwidth="{{excitationBandwidth}}" attenuation="0" usage="ABS" />
                                </ReadingLabel>
                            </MeasurementReading>
                        </Well>
                        <CustomData id="9" />
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
				program: {description: "Program definition"},
				programFile: {description: "Program filename", type: "File"},
				programData: {description: "Program data"},
				object: {description: "The labware being measured", type: "Plate"},
				outputFile: {description: "Filename for measured output", type: "string"}
			},
			required: ["measurementType", "outputFile"]
		},
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
			console.log("reader-InfiniteM200Pro-equipment.run: "+JSON.stringify(parsed, null, '\t'));

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
				// const labwareModelName = parsed.objectName["object.model"];
				const labwareModelName = parsed.value.object.model;
				console.log({labwareModelName})
				const labwareModel = _.get(data.objects, labwareModelName);
				console.log({labwareModel})
				const plateFile = {"ourlab.model.plateModel_96_round_transparent_nunc": "COR96fc UV transparent"}[labwareModelName];
				assert(plateFile);
				const wells = "A1:"+locationRowColToText(labwareModel.rows, labwareModel.columns);
				const params = {
					plateFile,
					wells,
					excitationWavelength: parsed.value.program.excitationWavelength,
					excitationBandwidth: 90
				};
				console.log({params});
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
			// Token
			var value = parsed.value.outputFile + "|" + programData;
			return {expansion: [makeEvowareFacts(parsed, data, "Measure", value)]};
		}
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


module.exports = {
	makeEvowareFacts,
	makeSiteModelPredicates,
	makeTransporterPredicates,

	objectToPredicateConverters: {
		"EvowareRobot": function(name) {
			return {
				value: [{
					"isAgent": {
						"agent": name
					}
				}]
			};
		}
	},
	getSchemas: () => ({
		"EvowareRobot": {
			properties: {
				type: {enum: ["EvowareRobot"]},
				config: {description: "configuration options for evoware", type: "object"}
			},
			required: ["type"]
		},
		"EvowareWashProgram": {
			properties: {
				type: {enum: ["EvowareWashProgram"]}
			},
			required: ["type"]
		},
	}),
	getCommandHandlers: () => ({
		"evoware._facts": function(params, parsed, data) {},
	}),
};
