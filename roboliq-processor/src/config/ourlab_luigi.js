/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017, ETH Zurich, Ellis Whitehead
 * @license GPL-3.0
 */

const _ = require('lodash');
const assert = require('assert');
const math = require('mathjs');
const commandHelper = require('../commandHelper.js');
const expect = require('../expect.js');
const {makeEvowareFacts, makeSiteModelPredicates, makeTransporterPredicates} = require('../evoware/equipment/evoware.js');

const Equipment = {
	evoware: require('../evoware/equipment/evoware.js'),
	reader: require('../evoware/equipment/reader-InfiniteM200Pro.js'),
	sealer: require('../evoware/equipment/sealer-Tecan.js'),
};

module.exports = {
	roboliq: "v1",
	imports: ["roboliq.js"],
	objects: {
		"ourlab": {
			"type": "Namespace",
			"luigi": {
				"type": "Namespace",
				"culturebox": {
					"type": "Incubator",
					"sitesInternal": ["ourlab.luigi.site.BOX_1", "ourlab.luigi.site.BOX_2", "ourlab.luigi.site.BOX_3", "ourlab.luigi.site.BOX_4", "ourlab.luigi.site.BOX_5", "ourlab.luigi.site.BOX_6", "ourlab.luigi.site.BOX_7", "ourlab.luigi.site.BOX_8"],
					"evowareId": "Kuehner"
				},
				"evoware": {
					"type": "EvowareRobot",
					config: {
						TEMPDIR: "C:\\Users\\localadmin\\Desktop\\Ellis\\temp",
						ROBOLIQ: "wscript C:\\Users\\localadmin\\Documents\\Ellis\\roboliq\\roboliq-runtime-cli\\roboliq-runtime-cli.vbs"
					}
				},
				"reader": {
					"type": "Reader",
					"sitesInternal": ["ourlab.luigi.site.READER"],
					"evowareId": "ReaderNETwork",
					"modelToPlateFile": {
						"ourlab.model.plateModel_96_round_transparent_nunc": "COR96fc UV transparent"
					}
				},
				"roma1": {
					"type": "Transporter",
					"evowareRoma": 0
				},
				"roma2": {
					"type": "Transporter",
					"evowareRoma": 1
				},
				"liha": {
					"type": "Pipetter",
					"syringe": {
						"1": {
							"type": "Syringe",
							"tipModel": "ourlab.luigi.liha.tipModel.tipModel2500",
							"tipModelPermanent": "ourlab.luigi.liha.tipModel.tipModel2500",
							row: 1
						},
						"2": {
							"type": "Syringe",
							"tipModel": "ourlab.luigi.liha.tipModel.tipModel2500",
							"tipModelPermanent": "ourlab.luigi.liha.tipModel.tipModel2500",
							row: 2
						},
						"3": {
							"type": "Syringe",
							"tipModel": "ourlab.luigi.liha.tipModel.tipModel2500",
							"tipModelPermanent": "ourlab.luigi.liha.tipModel.tipModel2500",
							row: 3
						},
						"4": {
							"type": "Syringe",
							"tipModel": "ourlab.luigi.liha.tipModel.tipModel2500",
							"tipModelPermanent": "ourlab.luigi.liha.tipModel.tipModel2500",
							row: 4
						},
						"5": {
							"type": "Syringe",
							"tipModel": "ourlab.luigi.liha.tipModel.tipModel2500",
							"tipModelPermanent": "ourlab.luigi.liha.tipModel.tipModel2500",
							row: 5
						},
						"6": {
							"type": "Syringe",
							"tipModel": "ourlab.luigi.liha.tipModel.tipModel2500",
							"tipModelPermanent": "ourlab.luigi.liha.tipModel.tipModel2500",
							row: 6
						},
						"7": {
							"type": "Syringe",
							"tipModel": "ourlab.luigi.liha.tipModel.tipModel2500",
							"tipModelPermanent": "ourlab.luigi.liha.tipModel.tipModel2500",
							row: 7
						},
						"8": {
							"type": "Syringe",
							"tipModel": "ourlab.luigi.liha.tipModel.tipModel2500",
							"tipModelPermanent": "ourlab.luigi.liha.tipModel.tipModel2500",
							row: 8
						}
					},
					"tipModel": {
						"tipModel2500": {"type": "TipModel", "programCode": "2500", "min": "10ul", "max": "2250ul", "canHandleSeal": true, "canHandleCells": true},
					},
					"tipModelToSyringes": {
						"ourlab.luigi.liha.tipModel.tipModel2500": ["ourlab.luigi.liha.syringe.1", "ourlab.luigi.liha.syringe.2", "ourlab.luigi.liha.syringe.3", "ourlab.luigi.liha.syringe.4", "ourlab.luigi.liha.syringe.5", "ourlab.luigi.liha.syringe.6", "ourlab.luigi.liha.syringe.7", "ourlab.luigi.liha.syringe.8"]
					}
				},
				"sealer": {
					"type": "Sealer",
					"evowareId": "RoboSeal"
				},
				"shaker": {
					"type": "Shaker",
					"evowareId": "Shaker"
				},
				"site": {
					"type": "Namespace",
					BOX_1: { type: "Site", evowareCarrier: "Kuhner Shaker ES-X  2x4x2Pos", evowareGrid: 50, evowareSite: 2, closed: true },
					BOX_2: { type: "Site", evowareCarrier: "Kuhner Shaker ES-X  2x4x2Pos", evowareGrid: 50, evowareSite: 4, closed: true },
					BOX_3: { type: "Site", evowareCarrier: "Kuhner Shaker ES-X  2x4x2Pos", evowareGrid: 50, evowareSite: 6, closed: true },
					BOX_4: { type: "Site", evowareCarrier: "Kuhner Shaker ES-X  2x4x2Pos", evowareGrid: 50, evowareSite: 8, closed: true },
					BOX_5: { type: "Site", evowareCarrier: "Kuhner Shaker ES-X  2x4x2Pos", evowareGrid: 50, evowareSite: 10, closed: true },
					BOX_6: { type: "Site", evowareCarrier: "Kuhner Shaker ES-X  2x4x2Pos", evowareGrid: 50, evowareSite: 12, closed: true },
					BOX_7: { type: "Site", evowareCarrier: "Kuhner Shaker ES-X  2x4x2Pos", evowareGrid: 50, evowareSite: 14, closed: true },
					BOX_8: { type: "Site", evowareCarrier: "Kuhner Shaker ES-X  2x4x2Pos", evowareGrid: 50, evowareSite: 16, closed: true },
					HOTEL12_1: { type: "Site", evowareCarrier: "Hotel 3x4Pos Grid25to39", evowareGrid: 25, evowareSite: 1 },
					HOTEL12_2: { type: "Site", evowareCarrier: "Hotel 3x4Pos Grid25to39", evowareGrid: 25, evowareSite: 2 },
					HOTEL12_3: { type: "Site", evowareCarrier: "Hotel 3x4Pos Grid25to39", evowareGrid: 25, evowareSite: 3 },
					HOTEL12_4: { type: "Site", evowareCarrier: "Hotel 3x4Pos Grid25to39", evowareGrid: 25, evowareSite: 4 },
					HOTEL12_5: { type: "Site", evowareCarrier: "Hotel 3x4Pos Grid25to39", evowareGrid: 25, evowareSite: 5 },
					HOTEL12_6: { type: "Site", evowareCarrier: "Hotel 3x4Pos Grid25to39", evowareGrid: 25, evowareSite: 6 },
					HOTEL12_7: { type: "Site", evowareCarrier: "Hotel 3x4Pos Grid25to39", evowareGrid: 25, evowareSite: 7 },
					HOTEL12_8: { type: "Site", evowareCarrier: "Hotel 3x4Pos Grid25to39", evowareGrid: 25, evowareSite: 8 },
					HOTEL12_9: { type: "Site", evowareCarrier: "Hotel 3x4Pos Grid25to39", evowareGrid: 25, evowareSite: 9 },
					HOTEL12_10: { type: "Site", evowareCarrier: "Hotel 3x4Pos Grid25to39", evowareGrid: 25, evowareSite: 10 },
					HOTEL12_11: { type: "Site", evowareCarrier: "Hotel 3x4Pos Grid25to39", evowareGrid: 25, evowareSite: 11 },
					//HOTEL12_12: { type: "Site", evowareCarrier: "Hotel 3x4Pos Grid25to39", evowareGrid: 25, evowareSite: 12 },
					INCUBATOR1_1: { type: "Site", evowareCarrier: "Incubator1", evowareGrid: 1, evowareSite: 4, closed: true },
					INCUBATOR1_2: { type: "Site", evowareCarrier: "Incubator1", evowareGrid: 1, evowareSite: 3, closed: true },
					INCUBATOR1_3: { type: "Site", evowareCarrier: "Incubator1", evowareGrid: 1, evowareSite: 2, closed: true },
					INCUBATOR1_4: { type: "Site", evowareCarrier: "Incubator1", evowareGrid: 1, evowareSite: 1, closed: true },
					LIGHT: { type: "Site", evowareCarrier: "Pickolo-Light-Table", evowareGrid: 29, evowareSite: 1 },
					P1: { type: "Site", evowareCarrier: "MP 2Pos Flat with Downholder", evowareGrid: 13, evowareSite: 1 },
					P2: { type: "Site", evowareCarrier: "MP 2Pos Flat with Downholder", evowareGrid: 13, evowareSite: 2 },
					P3: { type: "Site", evowareCarrier: "Downholder DWB", evowareGrid: 12, evowareSite: 1 },
					P4: { type: "Site", evowareCarrier: "MP 3Pos Flat Grid19", evowareGrid: 19, evowareSite: 1 },
					P5: { type: "Site", evowareCarrier: "MP 3Pos Flat Grid19", evowareGrid: 19, evowareSite: 2 },
					P6: { type: "Site", evowareCarrier: "MP 3Pos Flat Grid19", evowareGrid: 19, evowareSite: 3 },
					R1: { type: "Site", evowareCarrier: "Trough 1500ml StainlessSteel", evowareGrid: 37, evowareSite: 1 },
					R2: { type: "Site", evowareCarrier: "Trough 3Pos 25+100ml", evowareGrid: 40, evowareSite: 1 },
					R3: { type: "Site", evowareCarrier: "Trough 3Pos 25+100ml", evowareGrid: 40, evowareSite: 2 },
					R4: { type: "Site", evowareCarrier: "Trough 3Pos 25+100ml", evowareGrid: 40, evowareSite: 3 },
					READER: { type: "Site", evowareCarrier: "Infinite 200", evowareGrid: 6, evowareSite: 1, closed: true },
					REGRIP_ABOVE: { type: "Site", evowareCarrier: "ReGrip above", evowareGrid: 8, evowareSite: 1, siteIdUnique: "ourlab.luigi.REGRIP" },
					REGRIP_BELOW: { type: "Site", evowareCarrier: "ReGrip below", evowareGrid: 9, evowareSite: 1, siteIdUnique: "ourlab.luigi.REGRIP" },
					ROBOSEAL: { "type": "Site", "evowareCarrier": "RoboSeal", "evowareGrid": 45, "evowareSite": 1 },
					SHAKER: { type: "Site", evowareCarrier: "Te-Shake 1Pos Front", evowareGrid: 44, evowareSite: 1 },
					TRANSFER_1: { type: "Site", evowareCarrier: "Hotel 4Pos Transfer Grid 0", evowareGrid: 7, evowareSite: 1 },
					TRANSFER_2: { type: "Site", evowareCarrier: "Hotel 4Pos Transfer Grid 0", evowareGrid: 7, evowareSite: 2 },
					TRANSFER_3: { type: "Site", evowareCarrier: "Hotel 4Pos Transfer Grid 0", evowareGrid: 7, evowareSite: 3 },
					TRANSFER_4: { type: "Site", evowareCarrier: "Hotel 4Pos Transfer Grid 0", evowareGrid: 7, evowareSite: 4 },
					"SYSTEM": {
						"type": "Site",
						"evowareCarrier": "System",
						"evowareGrid": -1,
						"evowareSite": 0
					},
				},
				"systemLiquidLabwareModel": {
					"type": "PlateModel",
					"description": "dummy labware model representing the system liquid source",
					"rows": 8,
					"columns": 1,
					"evowareName": "SystemLiquid"
				},
				"systemLiquid": {
					"type": "Liquid",
					"wells": [
						"ourlab.luigi.systemLiquidLabware(A01)",
						"ourlab.luigi.systemLiquidLabware(B01)",
						"ourlab.luigi.systemLiquidLabware(C01)",
						"ourlab.luigi.systemLiquidLabware(D01)",
						"ourlab.luigi.systemLiquidLabware(E01)",
						"ourlab.luigi.systemLiquidLabware(F01)",
						"ourlab.luigi.systemLiquidLabware(G01)",
						"ourlab.luigi.systemLiquidLabware(H01)"
					]
				},
				"systemLiquidLabware": {
					"type": "Plate",
					"description": "dummy labware representing the system liquid source",
					"model": "ourlab.luigi.systemLiquidLabwareModel",
					"location": "ourlab.luigi.site.SYSTEM",
					"contents": ["Infinity l", "systemLiquid"]
				},
				"timer1": { "type": "Timer", "evowareId": 1 },
				"timer2": { "type": "Timer", "evowareId": 2 },
				"timer3": { "type": "Timer", "evowareId": 3 },
				"timer4": { "type": "Timer", "evowareId": 4 },
				"timer5": { "type": "Timer", "evowareId": 5 },
				"timer6": { "type": "Timer", "evowareId": 6 },
				"timer7": { "type": "Timer", "evowareId": 7 },
				"timer8": { "type": "Timer", "evowareId": 8 },
				"timer9": { "type": "Timer", "evowareId": 9 },
				"washProgram": {
					// FIXME: figure out proper wash programs
					"type": "Namespace",
					"flush_2500": {
						"type": "EvowareWashProgram",
						"wasteGrid": 42,
						"wasteSite": 2,
						"cleanerGrid": 42,
						"cleanerSite": 3,
						"wasteVolume": 0.15,
						"wasteDelay": 500,
						"cleanerVolume": 0.15,
						"cleanerDelay": 500,
						"airgapVolume": 10,
						"airgapSpeed": 70,
						"retractSpeed": 30,
						"fastWash": true
					},
					"light_2500": { // FIXME: IS THIS REALLY LIGHT?
						"type": "EvowareWashProgram",
						"wasteGrid": 42,
						"wasteSite": 2,
						"cleanerGrid": 42,
						"cleanerSite": 3,
						"wasteVolume": 1,
						"wasteDelay": 500,
						"cleanerVolume": 2,
						"cleanerDelay": 500,
						"airgapVolume": 10,
						"airgapSpeed": 70,
						"retractSpeed": 30,
						"fastWash": true
					},
					"thorough_2500": { // FIXME: IS THIS REALLY THOROUGH?
						"type": "EvowareWashProgram",
						"wasteGrid": 42,
						"wasteSite": 2,
						"cleanerGrid": 42,
						"cleanerSite": 3,
						"wasteVolume": 6,
						"wasteDelay": 500,
						"cleanerVolume": 8,
						"cleanerDelay": 500,
						"airgapVolume": 10,
						"airgapSpeed": 70,
						"retractSpeed": 30,
						"fastWash": true
					},
					"decontaminate_2500": {
						"type": "EvowareWashProgram",
						"script": "C:\\ProgramData\\Tecan\\EVOware\\database\\scripts\\Roboliq_Clean_Decontaminate.esc"
					},
				}
			},
			"model": {
				"type": "Namespace",
				"plateModel_96_dwp": {
					"type": "PlateModel",
					"label": "96 well deep-well plate",
					"rows": 8,
					"columns": 12,
					"evowareName": "DM 96-DWB GripUp",
					"evowareName_sealed": "DM 96 DWB GripUp sealed"
				},
				"plateModel_96_dwp_sealed": {
					"type": "PlateModel",
					"label": "96 well deep-well plate (sealed)",
					"rows": 8,
					"columns": 12,
					"evowareName": "DM 96 DWB GripUp sealed"
				},
				"plateModel_96_round_transparent_nunc": {
					"type": "PlateModel",
					"label": "96 round-well transparent Nunc plate",
					"rows": 8,
					"columns": 12,
					"evowareName": "Ellis Nunc F96 MicroWell"
				},
				"troughModel_100ml": {
					"type": "PlateModel",
					"label": "Trough 100ml",
					"rows": 8,
					"columns": 1,
					"evowareName": "Trough 100ml"
				},
				"troughModel_1500ml": {
					"type": "PlateModel",
					"label": "Trough 1500ml Stainless",
					"rows": 8,
					"columns": 1,
					"evowareName": "Trough 1500ml Stainless"
				},
			}
		}
	},

	objectToPredicateConverters: _.merge({},
		Equipment.evoware.objectToPredicateConverters
	),

	predicates: _.flatten([
		// Deepwell plates only
		makeSiteModelPredicates({
			siteModel: "ourlab.luigi.siteModel_DWP",
			sites: [
				"ourlab.luigi.site.P3",
				"ourlab.luigi.site.BOX_1", "ourlab.luigi.site.BOX_2", "ourlab.luigi.site.BOX_3", "ourlab.luigi.site.BOX_4", "ourlab.luigi.site.BOX_5", "ourlab.luigi.site.BOX_6", "ourlab.luigi.site.BOX_7", "ourlab.luigi.site.BOX_8",
			],
			labwareModels: ["ourlab.model.plateModel_96_dwp", "ourlab.model.plateModel_96_dwp_sealed"]
		}),
		// Shallow plates only
		makeSiteModelPredicates({
			siteModel: "ourlab.luigi.siteModel_shallow",
			sites: [
				"ourlab.luigi.site.HOTEL12_1", "ourlab.luigi.site.HOTEL12_5", "ourlab.luigi.site.HOTEL12_9",
				"ourlab.luigi.site.READER"
			],
			labwareModels: ["ourlab.model.plateModel_96_round_transparent_nunc"]
		}),
		// Deepwell blocks + microwell
		makeSiteModelPredicates({
			siteModel: "ourlab.luigi.siteModel1",
			sites: [
				"ourlab.luigi.site.HOTEL12_2", "ourlab.luigi.site.HOTEL12_3", "ourlab.luigi.site.HOTEL12_4", "ourlab.luigi.site.HOTEL12_6", "ourlab.luigi.site.HOTEL12_7", "ourlab.luigi.site.HOTEL12_8", "ourlab.luigi.site.HOTEL12_9", "ourlab.luigi.site.HOTEL12_10", "ourlab.luigi.site.HOTEL12_11",
				"ourlab.luigi.site.P1", "ourlab.luigi.site.P2", "ourlab.luigi.site.P4", "ourlab.luigi.site.P5", "ourlab.luigi.site.P6",
				"ourlab.luigi.site.LIGHT",
				"ourlab.luigi.site.REGRIP_ABOVE", "ourlab.luigi.site.REGRIP_BELOW",
				"ourlab.luigi.site.ROBOSEAL",
				"ourlab.luigi.site.SHAKER",
				"ourlab.luigi.site.TRANSFER_1", "ourlab.luigi.site.TRANSFER_2", "ourlab.luigi.site.TRANSFER_3", "ourlab.luigi.site.TRANSFER_4",
			],
			labwareModels: ["ourlab.model.plateModel_96_dwp", "ourlab.model.plateModel_96_dwp_sealed", "ourlab.model.plateModel_96_round_transparent_nunc"]
		}),
		makeTransporterPredicates("ourlab.luigi", "ourlab.luigi.evoware", {
			"ourlab.luigi.roma1": {
				Narrow: [
					[
						"ourlab.luigi.site.REGRIP_ABOVE",
						"ourlab.luigi.site.P1", "ourlab.luigi.site.P2", "ourlab.luigi.site.P3",
						"ourlab.luigi.site.LIGHT"
					],
					[
						"ourlab.luigi.site.HOTEL12_1", "ourlab.luigi.site.HOTEL12_2", "ourlab.luigi.site.HOTEL12_3", "ourlab.luigi.site.HOTEL12_4",
						"ourlab.luigi.site.HOTEL12_5", "ourlab.luigi.site.HOTEL12_6", "ourlab.luigi.site.HOTEL12_7", "ourlab.luigi.site.HOTEL12_8",
						"ourlab.luigi.site.HOTEL12_9", "ourlab.luigi.site.HOTEL12_10", "ourlab.luigi.site.HOTEL12_11", /*"ourlab.luigi.site.HOTEL12_12"*/,
						"ourlab.luigi.site.P4", "ourlab.luigi.site.P5", "ourlab.luigi.site.P6",
						"ourlab.luigi.site.REGRIP_BELOW",
						"ourlab.luigi.site.TRANSFER_1", "ourlab.luigi.site.TRANSFER_2", "ourlab.luigi.site.TRANSFER_3", "ourlab.luigi.site.TRANSFER_4",
					],
					["ourlab.luigi.site.REGRIP_ABOVE", "ourlab.luigi.site.REGRIP_BELOW"],
					["ourlab.luigi.site.P4", "ourlab.luigi.site.P5", "ourlab.luigi.site.P6", "ourlab.luigi.site.READER", "ourlab.luigi.site.REGRIP_BELOW"],
				],
			},
			"ourlab.luigi.roma2": {
				Narrow: [
					[
						"ourlab.luigi.site.BOX_1", "ourlab.luigi.site.BOX_2", "ourlab.luigi.site.BOX_3", "ourlab.luigi.site.BOX_4", "ourlab.luigi.site.BOX_5", "ourlab.luigi.site.BOX_6", "ourlab.luigi.site.BOX_7", "ourlab.luigi.site.BOX_8",
						"ourlab.luigi.site.LIGHT"
					],
					[
						"ourlab.luigi.site.P1", "ourlab.luigi.site.P2", "ourlab.luigi.site.P4", "ourlab.luigi.site.P5", "ourlab.luigi.site.P6",
						"ourlab.luigi.site.LIGHT",
						"ourlab.luigi.site.ROBOSEAL",
						"ourlab.luigi.site.SHAKER",
					],
				]
			}
		}),
		{"#for": {
			factors: {model: ["plateModel_96_round_transparent_nunc"]},
			output: {
				"absorbanceReader.canAgentEquipmentModelSite": {
					"agent": "ourlab.luigi.evoware",
					"equipment": "ourlab.luigi.reader",
					"model": "ourlab.model.{{model}}",
					"site": "ourlab.luigi.site.READER"
				}
			}
		}},
		{"#for": {
			factors: {model: ["plateModel_96_round_transparent_nunc"]},
			output: {
				"fluorescenceReader.canAgentEquipmentModelSite": {
					"agent": "ourlab.luigi.evoware",
					"equipment": "ourlab.luigi.reader",
					"model": "ourlab.model.{{model}}",
					"site": "ourlab.luigi.site.READER"
				}
			}
		}},
		{
			"sealer.canAgentEquipmentProgramModelSite": {
				"agent": "ourlab.luigi.evoware",
				"equipment": "ourlab.luigi.sealer",
				"program": "C:\\INSTALLFOLDER_NOVEMBER2014_30096901_CH_ETHBS\\PLATTENPROGRAMM FUER ROBOSEAL\\MTP Riplate SW 2ml.bcf",
				"model": "ourlab.model.plateModel_96_dwp",
				"site": "ourlab.luigi.site.ROBOSEAL"
			}
		},
		{
			"sealer.canAgentEquipmentProgramModelSite": {
				"agent": "ourlab.luigi.evoware",
				"equipment": "ourlab.luigi.sealer",
				"program": "C:\\INSTALLFOLDER_NOVEMBER2014_30096901_CH_ETHBS\\PLATTENPROGRAMM FUER ROBOSEAL\\MTP Riplate SW 2ml.bcf",
				"model": "ourlab.model.plateModel_96_dwp_sealed",
				"site": "ourlab.luigi.site.ROBOSEAL"
			}
		},
		{
			"shaker.canAgentEquipment": {
				"agent": "ourlab.luigi.evoware",
				"equipment": "ourlab.luigi.culturebox"
			}
		},
		{
			"shaker.canAgentEquipment": {
				"agent": "ourlab.luigi.evoware",
				"equipment": "ourlab.luigi.shaker"
			}
		},
		{
			"shaker.canAgentEquipmentSite": {
				"agent": "ourlab.luigi.evoware",
				"equipment": "ourlab.luigi.shaker",
				"site": "ourlab.luigi.site.SHAKER"
			}
		},
		{
			"pipetter.canAgentEquipment": {
				"agent": "ourlab.luigi.evoware",
				"equipment": "ourlab.luigi.liha"
			}
		},
		{"#for": {
			factors: {site: ["P1", "P2", "P3", "P4", "P5", "P6", "LIGHT", "SYSTEM", "R1", "R2", "R3", "R4"]},
			output: {
				"pipetter.canAgentEquipmentSite": {
					"agent": "ourlab.luigi.evoware",
					"equipment": "ourlab.luigi.liha",
					"site": "ourlab.luigi.site.{{site}}"
				}
			}
		}},
		{"#for": {
			factors: {i: [1, 2, 3, 4, 5, 6, 7, 8]},
			output: {
				"pipetter.canAgentEquipmentSyringe": {
					"agent": "ourlab.luigi.evoware",
					"equipment": "ourlab.luigi.liha",
					"syringe": "ourlab.luigi.liha.syringe.{{i}}"
				}
			}
		}},
		_.map(_.range(9), i => ({"timer.canAgentEquipment": {
			"agent": "ourlab.luigi.evoware",
			"equipment": "ourlab.luigi.timer"+(i+1),
		}})),
		_.map([1,2,3,4,5,6,7,8], function(n) {
			return {"method": {"description": "generic.closeSite-BOX_"+n,
				"task": {"generic.closeSite": {"site": "?site"}},
				"preconditions": [
					{"same": {"thing1": "?site", "thing2": "ourlab.luigi.site.BOX_"+n}}
				],
				"subtasks": {"ordered": [
					{"ourlab.luigi.culturebox.close": {"agent": "ourlab.luigi.evoware", "equipment": "ourlab.luigi.culturebox"}}
				]}
			}}
		}),
		{"action": {"description": "ourlab.luigi.culturebox.close: close the culturebox",
			"task": {"ourlab.luigi.culturebox.close": {"agent": "?agent", "equipment": "?equipment"}},
			"preconditions": [],
			"deletions": [],
			"additions": [
				{"siteIsClosed": {"site": "ourlab.luigi.site.BOX_1"}},
				{"siteIsClosed": {"site": "ourlab.luigi.site.BOX_2"}},
				{"siteIsClosed": {"site": "ourlab.luigi.site.BOX_3"}},
				{"siteIsClosed": {"site": "ourlab.luigi.site.BOX_4"}},
				{"siteIsClosed": {"site": "ourlab.luigi.site.BOX_5"}},
				{"siteIsClosed": {"site": "ourlab.luigi.site.BOX_6"}},
				{"siteIsClosed": {"site": "ourlab.luigi.site.BOX_7"}},
				{"siteIsClosed": {"site": "ourlab.luigi.site.BOX_8"}}
			]
		}},
		_.map([1,2,3,4,5,6,7,8], function(n) {
			return {"method": {"description": "generic.openSite-BOX_"+n,
				"task": {"generic.openSite": {"site": "?site"}},
				"preconditions": [{"same": {"thing1": "?site", "thing2": "ourlab.luigi.site.BOX_"+n}}],
				"subtasks": {"ordered": [{"ourlab.luigi.culturebox.stopAndOpen": {}}]}
			}};
		}),
		{"action": {"description": "ourlab.luigi.culturebox.open: open the culturebox",
			"task": {"ourlab.luigi.culturebox.stopAndOpen": {}},
			"preconditions": [],
			"deletions": [
				{"siteIsClosed": {"site": "ourlab.luigi.site.BOX_1"}},
				{"siteIsClosed": {"site": "ourlab.luigi.site.BOX_2"}},
				{"siteIsClosed": {"site": "ourlab.luigi.site.BOX_3"}},
				{"siteIsClosed": {"site": "ourlab.luigi.site.BOX_4"}},
				{"siteIsClosed": {"site": "ourlab.luigi.site.BOX_5"}},
				{"siteIsClosed": {"site": "ourlab.luigi.site.BOX_6"}},
				{"siteIsClosed": {"site": "ourlab.luigi.site.BOX_7"}},
				{"siteIsClosed": {"site": "ourlab.luigi.site.BOX_8"}}
			],
			"additions": []
		}},
		Equipment.reader.getPredicates("ourlab.luigi.evoware", "ourlab.luigi.reader", "ourlab.luigi.site.READER")
	]),

	schemas: _.merge(
		{
			"equipment.close|ourlab.luigi.evoware|ourlab.luigi.culturebox": {
				properties: {
					agent: {description: "Agent identifier", type: "Agent"},
					equipment: {description: "Equipment identifier", type: "Equipment"},
				},
				required: ["agent", "equipment"]
			},
			"equipment.open|ourlab.luigi.evoware|ourlab.luigi.culturebox": {
				properties: {
					agent: {description: "Agent identifier", type: "Agent"},
					equipment: {description: "Equipment identifier", type: "Equipment"},
				},
				required: ["agent", "equipment"]
			},
			"equipment.run|ourlab.luigi.evoware|ourlab.luigi.culturebox": {
				properties: {
					agent: {description: "Agent identifier", type: "Agent"},
					equipment: {description: "Equipment identifier", type: "Equipment"},
					program: {
						description: "Program for shaking and incubating",
						type: "object",
						properties: {
							rpm: {type: "number", default: 300},
							duration: {type: "Duration"},
							temperature: {type: "Temperature"}
						}
					}
				},
				required: ["program"]
			},
			"equipment.start|ourlab.luigi.evoware|ourlab.luigi.culturebox": {
				properties: {
					agent: {description: "Agent identifier", type: "Agent"},
					equipment: {description: "Equipment identifier", type: "Equipment"},
					program: {
						description: "Program for shaking and incubating",
						type: "object",
						properties: {
							rpm: {type: "number", default: 300},
							temperature: {type: "Temperature"}
						}
					}
				},
				required: ["program"]
			},
			"equipment.stop|ourlab.luigi.evoware|ourlab.luigi.culturebox": {
				properties: {
					agent: {description: "Agent identifier", type: "Agent"},
					equipment: {description: "Equipment identifier", type: "Equipment"},
				}
			},
			"shaker.run|ourlab.luigi.evoware|ourlab.luigi.shaker": {
				properties: {
					agent: {description: "Agent identifier", type: "Agent"},
					equipment: {description: "Equipment identifier", type: "Equipment"},
					program: {description: "Program identifier for shaking", type: "string"}
				}
			},
			"pipetter.cleanTips|ourlab.luigi.evoware|ourlab.luigi.liha": {
				description: "Clean the pipetter tips.",
				properties: {
					agent: {description: "Agent identifier", type: "Agent"},
					equipment: {description: "Equipment identifier", type: "Equipment"},
					program: {description: "Program identifier", type: "string"},
					items: {
						description: "List of which syringes to clean at which intensity",
						type: "array",
						items: {
							type: "object",
							properties: {
								syringe: {description: "Syringe identifier", type: "Syringe"},
								intensity: {description: "Intensity of the cleaning", type: "pipetter.CleaningIntensity"}
							},
							required: ["syringe", "intensity"]
						}
					}
				},
				required: ["agent", "equipment", "items"]
			}
		},
		Equipment.evoware.getSchemas(),
		Equipment.reader.getSchemas("ourlab.luigi.evoware", "ourlab.luigi.reader"),
		Equipment.sealer.getSchemas("ourlab.luigi.evoware", "ourlab.luigi.sealer")
	),

	commandHandlers: _.merge(
		{
			"equipment.close|ourlab.luigi.evoware|ourlab.luigi.culturebox": function(params, parsed, data) {
				const expansion = [
					{
						command: "evoware._facts",
						agent: parsed.objectName.agent,
						factsEquipment: "Cover",
						factsVariable: "Cover_Close"
					}
				];
				return {expansion};
			},
			"equipment.open|ourlab.luigi.evoware|ourlab.luigi.culturebox": function(params, parsed, data) {
				var effects = _.merge(
					// Indicate the the equipment is open and not running
					{
						[`${parsed.objectName.equipment}.open`]: true,
						[`${parsed.objectName.equipment}.running`]: false
					},
					// Indicate that the internal sites are open
					_.fromPairs(_.map(parsed.value.equipment.sitesInternal, site => [`${site}.closed`, false]))
				);

				// Stop and open
				const expansion = [
					{
						command: "evoware._facts",
						agent: parsed.objectName.agent,
						factsEquipment: "Kuehner",
						factsVariable: "Kuehner_stop"
					},
					{
						command: "evoware._facts",
						agent: parsed.objectName.agent,
						factsEquipment: "Cover",
						factsVariable: "Cover_Open"
					}
				];

				return {expansion, effects};
			},
			"equipment.start|ourlab.luigi.evoware|ourlab.luigi.culturebox": function(params, parsed, data) {
				// console.log("equipment.run|ourlab.luigi.evoware|ourlab.luigi.culturebox:"); console.log({parsed, params})
				const equipmentId = commandHelper.getParsedValue(parsed, data, "equipment", "evowareId");
				const parsedProgram = parsed.value.program;
				//console.log({parsedProgram});
				const expansion = [
					{
						command: "evoware._facts",
						agent: parsed.objectName.agent,
						factsEquipment: equipmentId,
						factsVariable: equipmentId+"_start",
						factsValue: parsedProgram.rpm
					},
				];
				return {expansion};
			},
			"equipment.stop|ourlab.luigi.evoware|ourlab.luigi.culturebox": function(params, parsed, data) {
				// console.log("equipment.run|ourlab.luigi.evoware|ourlab.luigi.culturebox:"); console.log({parsed, params})
				const equipmentId = commandHelper.getParsedValue(parsed, data, "equipment", "evowareId");
				const expansion = [
					{
						command: "evoware._facts",
						agent: parsed.objectName.agent,
						factsEquipment: equipmentId,
						factsVariable: equipmentId+"_stop"
					},
				];
				return {expansion};
			},
			"equipment.run|ourlab.luigi.evoware|ourlab.luigi.culturebox": function(params, parsed, data) {
				// console.log("shaker.run|ourlab.luigi.evoware|ourlab.luigi.culturebox:"); console.log({parsed, params})
				const equipmentId = commandHelper.getParsedValue(parsed, data, "equipment", "evowareId");
				const parsedProgram = parsed.value.program;
				//console.log({parsedProgram});
				const expansion = [
					{
						command: "evoware._facts",
						agent: parsed.objectName.agent,
						factsEquipment: equipmentId,
						factsVariable: equipmentId+"_start",
						factsValue: parsedProgram.rpm
					},
					{
						command: "timer.sleep",
						agent: parsed.objectName.agent,
						duration: parsed.orig.program.duration
					},
					{
						command: "evoware._facts",
						agent: parsed.objectName.agent,
						factsEquipment: equipmentId,
						factsVariable: equipmentId+"_stop"
					},
				];
				return {expansion};
			},
			// Shaker
			"equipment.run|ourlab.luigi.evoware|ourlab.luigi.shaker": function(params, parsed, data) {
				//console.log("equipment.run|ourlab.luigi.evoware|ourlab.luigi.shaker: "+JSON.stringify(parsed, null, '\t'))
				const equipmentId = commandHelper.getParsedValue(parsed, data, "equipment", "evowareId");
				const rpm = parsed.value.program.rpm || 750;

				return {
					expansion: [
						{
							command: "evoware._facts",
							agent: parsed.objectName.agent,
							factsEquipment: equipmentId,
							factsVariable: equipmentId+"_Init"
						},
						{
							command: "evoware._facts",
							agent: parsed.objectName.agent,
							factsEquipment: equipmentId,
							factsVariable: equipmentId+"_SetFrequency",
							factsValue: rpm
						},
						{
							command: "evoware._facts",
							agent: parsed.objectName.agent,
							factsEquipment: equipmentId,
							factsVariable: equipmentId+"_Start",
							factsValue: "1"
						},
						{
							command: "timer.sleep",
							agent: parsed.objectName.agent,
							duration: parsed.orig.program.duration
						},
						{
							command: "evoware._facts",
							agent: parsed.objectName.agent,
							factsEquipment: equipmentId,
							factsVariable: equipmentId+"_Stop"
						},
					]
				};
			},
			// Clean tips
			"pipetter.cleanTips|ourlab.luigi.evoware|ourlab.luigi.liha": function(params, parsed, data) {
				// console.log("pipetter.cleanTips|ourlab.luigi.evoware|ourlab.luigi.liha")
				// console.log(JSON.stringify(parsed, null, '  '))

				const cleaningIntensities = data.schemas["pipetter.CleaningIntensity"].enum;
				const syringeNameToItems = _.map(parsed.value.items, (item, index) => [parsed.objectName[`items.${index}.syringe`], item]);
				//console.log(syringeNameToItems);

				const expansionList = [];
				const sub = function(syringeNames, volume) {
					const syringeNameToItems2 = _.filter(syringeNameToItems, ([syringeName, ]) =>
						_.includes(syringeNames, syringeName)
					);
					//console.log({syringeNameToItems2})
					if (!_.isEmpty(syringeNameToItems2)) {
						const value = _.max(_.map(syringeNameToItems2, ([, item]) => cleaningIntensities.indexOf(item.intensity)));
						if (value >= 0) {
							const intensity = cleaningIntensities[value];
							const syringes = _.map(syringeNameToItems2, ([syringeName, ]) => syringeName);
							expansionList.push({
								command: "pipetter._washTips",
								agent: parsed.objectName.agent,
								equipment: parsed.objectName.equipment,
								program: `ourlab.luigi.washProgram.${intensity}_${volume}`,
								intensity: intensity,
								syringes: syringeNames
							});
						}
					}
				}
				sub(_.map([1, 2, 3, 4, 5, 6, 7, 8], n => `ourlab.luigi.liha.syringe.${n}`), "2500");
				return {expansion: expansionList};
			}
		},
		Equipment.evoware.getCommandHandlers(),
		Equipment.reader.getCommandHandlers("ourlab.luigi.evoware", "ourlab.luigi.reader"),
		Equipment.sealer.getCommandHandlers("ourlab.luigi.evoware", "ourlab.luigi.sealer")
	),

	planHandlers: _.merge(
		{
			"ourlab.luigi.culturebox.close": function(params, parentParams, data) {
				return [{
					command: "equipment.close",
					agent: "ourlab.luigi.evoware",
					equipment: "ourlab.luigi.culturebox"
				}];
			},
			"ourlab.luigi.culturebox.stopAndOpen": function(params, parentParams, data) {
				return [{
					command: "equipment.open",
					agent: "ourlab.luigi.evoware",
					equipment: "ourlab.luigi.culturebox"
				}];
			},
		},
		Equipment.reader.getPlanHandlers("ourlab.luigi.evoware", "ourlab.luigi.reader", "ourlab.luigi.site.READER")
	)
}
