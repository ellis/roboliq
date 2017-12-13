/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017, ETH Zurich, Ellis Whitehead
 * @license GPL-3.0
 */

import _ from 'lodash';
import should from 'should';
import * as EvowareConfigSpec from '../src/EvowareConfigSpec.js';
// const Validator = require('jsonschema').Validator;
// const YAML = require('yamljs');

const evowareConfigSpec = {
	namespace: "ourlab",
	name: "mario",
	config: {
		TEMPDIR: "C:\\Users\\localadmin\\Desktop\\Ellis\\temp",
		ROBOLIQ: "wscript C:\\Users\\localadmin\\Desktop\\Ellis\\roboliq\\roboliq-runtime-cli\\roboliq-runtime-cli.vbs",
		BROWSER: "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe",
	},
	sites: {
		"HOTEL4_1": { evowareCarrier: "Hotel 4Pos Transfer Grid 69", "evowareGrid": 69, "evowareSite": 1 },
		"HOTEL4_2": { evowareCarrier: "Hotel 4Pos Transfer Grid 69", "evowareGrid": 69, "evowareSite": 2 },
		"HOTEL4_3": { evowareCarrier: "Hotel 4Pos Transfer Grid 69", "evowareGrid": 69, "evowareSite": 3 },
		"HOTEL4_4": { evowareCarrier: "Hotel 4Pos Transfer Grid 69", "evowareGrid": 69, "evowareSite": 4 },
		"HOTEL32_A1": { evowareCarrier: "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 4 },
		"HOTEL32_B1": { evowareCarrier: "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 3 },
		"HOTEL32_C1": { evowareCarrier: "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 2 },
		"HOTEL32_D1": { evowareCarrier: "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 1 },
		"HOTEL32_A2": { evowareCarrier: "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 8 },
		"HOTEL32_B2": { evowareCarrier: "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 7 },
		"HOTEL32_C2": { evowareCarrier: "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 6 },
		"HOTEL32_D2": { evowareCarrier: "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 5 },
		"HOTEL32_A3": { evowareCarrier: "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 12 },
		"HOTEL32_B3": { evowareCarrier: "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 11 },
		"HOTEL32_C3": { evowareCarrier: "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 10 },
		"HOTEL32_D3": { evowareCarrier: "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 9 },
		"HOTEL32_A4": { evowareCarrier: "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 16 },
		"HOTEL32_B4": { evowareCarrier: "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 15 },
		"HOTEL32_C4": { evowareCarrier: "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 14 },
		"HOTEL32_D4": { evowareCarrier: "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 13 },
		"HOTEL32_A5": { evowareCarrier: "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 20 },
		"HOTEL32_B5": { evowareCarrier: "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 19 },
		"HOTEL32_C5": { evowareCarrier: "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 18 },
		"HOTEL32_D5": { evowareCarrier: "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 17 },
		"HOTEL32_A6": { evowareCarrier: "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 24 },
		"HOTEL32_B6": { evowareCarrier: "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 23 },
		"HOTEL32_C6": { evowareCarrier: "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 22 },
		"HOTEL32_D6": { evowareCarrier: "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 21 },
		"HOTEL32_A7": { evowareCarrier: "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 28 },
		"HOTEL32_B7": { evowareCarrier: "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 27 },
		"HOTEL32_C7": { evowareCarrier: "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 26 },
		"HOTEL32_D7": { evowareCarrier: "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 25 },
		"HOTEL32_A8": { evowareCarrier: "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 32 },
		"HOTEL32_B8": { evowareCarrier: "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 31 },
		"HOTEL32_C8": { evowareCarrier: "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 30 },
		"HOTEL32_D8": { evowareCarrier: "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 29 },
		"P1DOWNHOLDER": { evowareCarrier: "Downholder", "evowareGrid": 9, "evowareSite": 3 },
		P2: { evowareCarrier: "MP 2Pos H+P Shake", "evowareGrid": 10, "evowareSite": 2 },
		P3: { evowareCarrier: "MP 2Pos H+P Shake", "evowareGrid": 10, "evowareSite": 4 },
		P4: { evowareCarrier: "MP 3Pos Cooled 1 PCR", evowareGrid: 17, evowareSite: 2 },
		P5: { evowareCarrier: "MP 3Pos Cooled 1 PCR", evowareGrid: 17, evowareSite: 4 },
		P6: { evowareCarrier: "MP 3Pos Cooled 2 PCR", evowareGrid: 24, evowareSite: 2 },
		P7: { evowareCarrier: "MP 3Pos Cooled 2 PCR", evowareGrid: 24, evowareSite: 4 },
		P8: { evowareCarrier: "MP 3Pos Cooled 2 PCR", evowareGrid: 24, evowareSite: 6 },
		R1: { evowareCarrier: "LI - Trough 3Pos 100ml", evowareGrid: 3, evowareSite: 1 },
		R2: { evowareCarrier: "LI - Trough 3Pos 100ml", evowareGrid: 3, evowareSite: 2 },
		R3: { evowareCarrier: "LI - Trough 3Pos 100ml", evowareGrid: 3, evowareSite: 3 },
		R4: { evowareCarrier: "LI - Trough 3Pos 100mlOffset", evowareGrid: 8, evowareSite: 1 },
		R5: { evowareCarrier: "LI - Trough 3Pos 100mlOffset", evowareGrid: 8, evowareSite: 2 },
		R6: { evowareCarrier: "LI - Trough 3Pos 100mlOffset", evowareGrid: 8, evowareSite: 3 },
		REGRIP: { evowareCarrier: "ReGrip Station", "evowareGrid": 59, "evowareSite": 1 },
		ROBOPEEL: { evowareCarrier: "RoboPeel", "evowareGrid": 12, "evowareSite": 1 },
		SYSTEM: { evowareCarrier: "System", "evowareGrid": -1, "evowareSite": 0 },
		T1: { evowareCarrier: "Cooled 8Pos*15ml 8Pos*50ml", evowareGrid: 4, evowareSite: 1 },
		T2: { evowareCarrier: "Cooled 8Pos*15ml 8Pos*50ml", evowareGrid: 4, evowareSite: 2 },
		T3: { evowareCarrier: "Block 20Pos", evowareGrid: 16, evowareSite: 1 },
	},

	// Models
	models: {
		"EK_96_well_Greiner_Black": {
			type: "PlateModel",
			rows: 8,
			columns: 12,
			evowareName: "EK 96 well Greiner Black"
		},
		"EK_384_greiner_flat_bottom": {
			type: "PlateModel",
			rows: 16,
			columns: 24,
			evowareName: "EK 384 greiner flat bottom"
		},
		"plateModel_48_flower": {
			"type": "PlateModel",
			"description": "48 flower-well plate",
			"rows": 6,
			"columns": 8,
			"evowareName": "Ellis 48 Flower Plate"
		},
		"plateModel_96_pcr": {
			"type": "PlateModel",
			"description": "96 well PCR plate",
			"rows": 8,
			"columns": 12,
			"evowareName": "D-BSSE 96 Well PCR Plate"
		},
		"plateModel_96_dwp": {
			"type": "PlateModel",
			"description": "96 well deep-well plate",
			"rows": 8,
			"columns": 12,
			"evowareName": "D-BSSE 96 Well DWP"
		},
		"plateModel_96_round_transparent_nunc": {
			"type": "PlateModel",
			"description": "96 round-well transparent Nunc plate",
			"rows": 8,
			"columns": 12,
			"evowareName": "Ellis Nunc F96 MicroWell"
		},
		"plateModel_96_square_transparent_nunc": {
			"type": "PlateModel",
			"description": "96 square-well transparent Nunc plate",
			"rows": 8,
			"columns": 12,
			"evowareName": "Ellis Nunc F96 MicroWell"
		},
		"plateModel_384_round": {
			"type": "PlateModel",
			"description": "384 round-well plate",
			"rows": 16,
			"columns": 24,
			"evowareName": "D-BSSE 384 Well Plate White"
		},
		"plateModel_384_square": {
			"type": "PlateModel",
			"description": "384 square-flat-well white plate",
			"rows": 16,
			"columns": 24,
			"evowareName": "D-BSSE 384 Well Plate White"
		},
		"plateModel_384_square_transparent_greiner": {
			"type": "PlateModel",
			"description": "384 square-flat-well transparent Greiner",
			"rows": 16,
			"columns": 24,
			"evowareName": "384 Sqr Flat Trans Greiner"
		},
		"plateModel_96_round_filter_OV": {
			"type": "PlateModel",
			"description": "96 round-well white filter plate",
			"rows": 8,
			"columns": 12,
			"evowareName": "OV 96 Well Filter Plate wLid"
		},
		"plateModel_96_round_deep_OV": {
			"type": "PlateModel",
			"description": "96 round-well white filter plate",
			"rows": 8,
			"columns": 12,
			"evowareName": "OV 96 DW Lid Ritter" // ToDo: Verify this
		},
		"troughModel_100ml": {
			"type": "PlateModel",
			"description": "Trough 100ml",
			"rows": 8,
			"columns": 1,
			"evowareName": "Trough 100ml"
		},
		"troughModel_25ml": {
			"type": "PlateModel",
			"description": "Trough 25ml",
			"rows": 8,
			"columns": 1,
			"evowareName": "Trough 25ml"
		},
		"tubeHolderModel_1500ul": {
			"type": "PlateModel",
			"description": "20 tube block 1.5ml",
			"rows": 4,
			"columns": 5,
			"evowareName": "Block 20Pos 1.5 ml Eppendorf"
		},
		"tubeHolderModel_15ml": {
			"type": "PlateModel",
			"description": "8 tube block 15ml",
			"rows": 8,
			"columns": 1,
			"evowareName": "Reagent Cooled 8*15ml"
		},
		"tubeHolderModel_50ml": {
			"type": "PlateModel",
			"description": "8 tube block 50ml",
			"rows": 8,
			"columns": 1,
			"evowareName": "Reagent Cooled 8*50ml"
		}
	},
	siteModelCompatibilities: [
		{
			description: "short-format plate sites (non-tall labware, not for deep well plates)",
			sites: ["P1DOWNHOLDER", "HOTEL4_1", "HOTEL4_2", "HOTEL4_3", "HOTEL4_4", "HOTEL32_A1", "HOTEL32_B1", "HOTEL32_C1", "HOTEL32_D1", "HOTEL32_A2", "HOTEL32_B2", "HOTEL32_C2", "HOTEL32_D2", "HOTEL32_A3", "HOTEL32_B3", "HOTEL32_C3", "HOTEL32_D3", "HOTEL32_A4", "HOTEL32_B4", "HOTEL32_C4", "HOTEL32_D4", "HOTEL32_A5", "HOTEL32_B5", "HOTEL32_C5", "HOTEL32_D5", "HOTEL32_A6", "HOTEL32_B6", "HOTEL32_C6", "HOTEL32_D6", "HOTEL32_A7", "HOTEL32_B7", "HOTEL32_C7", "HOTEL32_D7", "HOTEL32_A8", "HOTEL32_B8", "HOTEL32_C8", "HOTEL32_D8", "READER", "ROBOPEEL", "ROBOSEAL"],
			models: ["plateModel_96_round_transparent_nunc", "plateModel_96_square_transparent_nunc", "plateModel_384_square", "EK_96_well_Greiner_Black", "EK_384_greiner_flat_bottom", "lidModel_standard", "plateModel_96_round_filter_OV"]
		},
		{
			description: "PCR-format sites",
			sites: ["P4PCR", "HOTEL4_1", "HOTEL4_2", "HOTEL4_3", "HOTEL4_4", "HOTEL32_A1", "HOTEL32_B1", "HOTEL32_C1", "HOTEL32_D1", "HOTEL32_A2", "HOTEL32_B2", "HOTEL32_C2", "HOTEL32_D2", "HOTEL32_A3", "HOTEL32_B3", "HOTEL32_C3", "HOTEL32_D3", "HOTEL32_A4", "HOTEL32_B4", "HOTEL32_C4", "HOTEL32_D4", "HOTEL32_A5", "HOTEL32_B5", "HOTEL32_C5", "HOTEL32_D5", "HOTEL32_A6", "HOTEL32_B6", "HOTEL32_C6", "HOTEL32_D6", "HOTEL32_A7", "HOTEL32_B7", "HOTEL32_C7", "HOTEL32_D7", "HOTEL32_A8", "HOTEL32_B8", "HOTEL32_C8", "HOTEL32_D8", "ROBOPEEL", "ROBOSEAL"],
			models: ["plateModel_96_pcr"]
		},
		{
			description: "Bench sites that don't have any obstructions, so deep well plates can fit on them too",
			sites: ["P2", "P3", "P4", "P5", "P6", "P7", "P8", "REGRIP"],
			models: ["plateModel_96_round_transparent_nunc", "plateModel_96_square_transparent_nunc", "plateModel_384_square", "plateModel_96_dwp", "EK_96_well_Greiner_Black", "EK_384_greiner_flat_bottom", "lidModel_standard", "plateModel_96_round_filter_OV", "plateModel_96_round_deep_OV"]
		},
	],
	equipment: {
		centrifuge: {
			module: "centrifuge4.js",
			params: {
				evowareId: "Centrifuge",
				evowareCarrier: "Centrifuge",
				evowareGrid: 54,
				sites: {
					CENTRIFUGE_1: { evowareSite: 1 },
					CENTRIFUGE_2: { evowareSite: 2 },
					CENTRIFUGE_3: { evowareSite: 1 },
					CENTRIFUGE_4: { evowareSite: 2 }
				},
				siteModelCompatibilities: [
					{
						sites: ["CENTRIFUGE_1", "CENTRIFUGE_2", "CENTRIFUGE_3", "CENTRIFUGE_4"],
						models: ["plateModel_384_square", "plateModel_96_round_transparent_nunc"]
					},
					{
						sites: ["CENTRIFUGE_2", "CENTRIFUGE_4"],
						models: ["plateModel_96_dwp"]
					},
				]
			}
		},
		reader: {
			module: "reader-InfiniteM200Pro.js",
			params: {
				evowareId: "ReaderNETwork",
				evowareCarrier: "Infinite M200",
				evowareGrid: 61,
				evowareSite: 1,
				site: "READER",
				modelToPlateFile: {
					"plateModel_96_round_transparent_nunc": "NUN96ft",
					"plateModel_384_square": "GRE384fw",
					"EK_384_greiner_flat_bottom": "GRE384fw",
					"EK_96_well_Greiner_Black": "GRE96fb_chimney"
				}
			}
		},
		shaker: {
			module: "shaker-Tecan1.js",
			params: {
				evowareId: "HPShaker",
				site: "P3"
			}
		},
		sealer: {
			module: "sealer-Tecan.js",
			params: {
				evowareId: "RoboSeal",
				evowareCarrier: "RoboSeal",
				evowareGrid: 35,
				evowareSite: 1,
				site: "ROBOSEAL",
				modelToPlateFile: {
					plateModel_96_round_transparent_nunc: "C:\\HJBioanalytikGmbH\\RoboSeal3\\RoboSeal_PlateParameters\\PerkinElmer_weiss.bcf",
					plateModel_96_square_transparent_nunc: "C:\\HJBioanalytikGmbH\\RoboSeal3\\RoboSeal_PlateParameters\\PerkinElmer_weiss.bcf",
					plateModel_384_square: "C:\\HJBioanalytikGmbH\\RoboSeal3\\RoboSeal_PlateParameters\\Greiner_384_schwarz.bcf",
				}
			}
		}
	},
	lidStacking: [
		{
			lids: ["lidModel_standard"],
			models: [
				"plateModel_96_round_transparent_nunc",
				"plateModel_96_square_transparent_nunc",
				"plateModel_384_round",
				"plateModel_384_square",
				"plateModel_96_round_filter_OV",
				"plateModel_96_round_deep_OV"
			]
		},
		{
			lids: ["plateModel_96_round_filter_OV"],
			models: ["plateModel_96_round_transparent_nunc", "plateModel_96_round_deep_OV"]
		}
	],
	romas: [
		{
			description: "roma1",
			safeVectorCliques: [
				{ vector: "Narrow", clique: ["P2", "P3", "P4", "P5", "P6", "P7", "P8", "REGRIP"] },
				{ vector: "Narrow", clique: ["CENTRIFUGE_1", "REGRIP"] },
				{ vector: "Narrow", clique: ["CENTRIFUGE_2", "REGRIP"] },
				{ vector: "Narrow", clique: ["CENTRIFUGE_3", "REGRIP"] },
				{ vector: "Narrow", clique: ["CENTRIFUGE_4", "REGRIP"] },
			]
		},
		{
			description: "roma2",
			safeVectorCliques: [
				{
					vector: "Narrow",
					clique: [
						/*"P1DOWNHOLDER",*/ "P2", "P3", "P6", "P7", "P8", "ROBOPEEL", "ROBOSEAL", "REGRIP",
						"HOTEL4_1", "HOTEL4_2", "HOTEL4_3", "HOTEL4_4",
						"HOTEL32_A1", "HOTEL32_B1", "HOTEL32_C1", "HOTEL32_D1",
						"HOTEL32_A2", "HOTEL32_B2", "HOTEL32_C2", "HOTEL32_D2",
						"HOTEL32_A3", "HOTEL32_B3", "HOTEL32_C3", "HOTEL32_D3",
						"HOTEL32_A4", "HOTEL32_B4", "HOTEL32_C4", "HOTEL32_D4",
						"HOTEL32_A5", "HOTEL32_B5", "HOTEL32_C5", "HOTEL32_D5",
						"HOTEL32_A6", "HOTEL32_B6", "HOTEL32_C6", "HOTEL32_D6",
						"HOTEL32_A7", "HOTEL32_B7", "HOTEL32_C7", "HOTEL32_D7",
						"HOTEL32_A8", "HOTEL32_B8", "HOTEL32_C8", "HOTEL32_D8",
					]
				},
				{
					vector: "Narrow",
					clique: [
						"P4PCR",
						"ROBOPEEL", "ROBOSEAL",
						"HOTEL4_1", "HOTEL4_2", "HOTEL4_3", "HOTEL4_4",
						"HOTEL32_A1", "HOTEL32_B1", "HOTEL32_C1", "HOTEL32_D1",
						"HOTEL32_A2", "HOTEL32_B2", "HOTEL32_C2", "HOTEL32_D2",
						"HOTEL32_A3", "HOTEL32_B3", "HOTEL32_C3", "HOTEL32_D3",
						"HOTEL32_A4", "HOTEL32_B4", "HOTEL32_C4", "HOTEL32_D4",
						"HOTEL32_A5", "HOTEL32_B5", "HOTEL32_C5", "HOTEL32_D5",
						"HOTEL32_A6", "HOTEL32_B6", "HOTEL32_C6", "HOTEL32_D6",
						"HOTEL32_A7", "HOTEL32_B7", "HOTEL32_C7", "HOTEL32_D7",
						"HOTEL32_A8", "HOTEL32_B8", "HOTEL32_C8", "HOTEL32_D8",
					]
				},
				{ vector: "Wide", clique: ["READER", "REGRIP"] }
			]
		}
	],
	liha: {
		tipModels: {
			"tipModel1000": {"programCode": "1000", "min": "3ul", "max": "950ul", "canHandleSeal": false, "canHandleCells": true},
			"tipModel0050": {"programCode": "0050", "min": "0.25ul", "max": "45ul", "canHandleSeal": true, "canHandleCells": false}
		},
		syringes: [
			{ tipModelPermanent: "tipModel1000" },
			{ tipModelPermanent: "tipModel1000" },
			{ tipModelPermanent: "tipModel1000" },
			{ tipModelPermanent: "tipModel1000" },
			{ tipModelPermanent: "tipModel0050" },
			{ tipModelPermanent: "tipModel0050" },
			{ tipModelPermanent: "tipModel0050" },
			{ tipModelPermanent: "tipModel0050" }
		],
		sites: ["P1DOWNHOLDER", "P2", "P3", "P4", "P4PCR", "P5", "P6", "P7", "P8", "R1", "R2", "R3", "R4", "R5", "R6", "SYSTEM", "T1", "T2", "T3"],
		washPrograms: {
			"flush_1000": {
				"type": "EvowareWashProgram",
				"wasteGrid": 1,
				"wasteSite": 2,
				"cleanerGrid": 1,
				"cleanerSite": 1,
				"wasteVolume": 4,
				"wasteDelay": 500,
				"cleanerVolume": 3,
				"cleanerDelay": 500,
				"airgapVolume": 10,
				"airgapSpeed": 70,
				"retractSpeed": 30,
				"fastWash": true
			},
			"flush_0050": {
				"type": "EvowareWashProgram",
				"wasteGrid": 1,
				"wasteSite": 2,
				"cleanerGrid": 1,
				"cleanerSite": 1,
				"wasteVolume": 0.05,
				"wasteDelay": 500,
				"cleanerVolume": 0.05,
				"cleanerDelay": 500,
				"airgapVolume": 10,
				"airgapSpeed": 70,
				"retractSpeed": 30,
				"fastWash": false
			},
			"light_1000": {
				"type": "EvowareWashProgram",
				"wasteGrid": 1,
				"wasteSite": 2,
				"cleanerGrid": 1,
				"cleanerSite": 1,
				"wasteVolume": 4,
				"wasteDelay": 500,
				"cleanerVolume": 2,
				"cleanerDelay": 500,
				"airgapVolume": 10,
				"airgapSpeed": 70,
				"retractSpeed": 30,
				"fastWash": false
			},
			"thorough_1000": {
				"type": "EvowareWashProgram",
				"script": "C:\\ProgramData\\TECAN\\EVOware\\database\\scripts\\Roboliq\\Roboliq_Clean_Thorough_1000.esc"
			},
			"thorough_0050": {
				"type": "EvowareWashProgram",
				"script": "C:\\ProgramData\\TECAN\\EVOware\\database\\scripts\\Roboliq\\Roboliq_Clean_Thorough_0050.esc"
			},
			"decontaminate_1000": {
				"type": "EvowareWashProgram",
				"script": "C:\\ProgramData\\TECAN\\EVOware\\database\\scripts\\Roboliq\\Roboliq_Clean_Decontaminate_1000.esc"
			},
			"decontaminate_0050": {
				"type": "EvowareWashProgram",
				"script": "C:\\ProgramData\\TECAN\\EVOware\\database\\scripts\\Roboliq\\Roboliq_Clean_Decontaminate_0050.esc"
			}
		}
	},
	commandHandlers: {
		"scale.weigh": function(params, parsed, data) { }
	},
	planAlternativeChoosers: {
		"centrifuge.canAgentEquipmentModelSite1Site2": (alternatives) => { },
		"shaker.canAgentEquipmentSite": (alternatives) => { },
	},
};

describe('EvowareConfigSpec', function() {
	describe('validate', function () {
		it('should validate', function () {
			const result = EvowareConfigSpec.validate(evowareConfigSpec);
			// console.log(result);
			should.deepEqual(result.errors, []);
		});
	});
});
