const _ = require('lodash');
const assert = require('assert');
const math = require('mathjs');
const commandHelper = require('roboliq-processor/dist/commandHelper.js');
const expect = require('roboliq-processor/dist/expect.js');
const Evoware = require('roboliq-evoware/dist/equipment/evoware.js');

const {makeEvowareFacts, makeSiteModelPredicates, makeTransporterPredicates} = Evoware;

const Equipment = {
	evoware: require('roboliq-evoware/dist/equipment/evoware.js'),
	reader: require('roboliq-evoware/dist/equipment/reader-InfiniteM200Pro.js'),
	sealer: require('roboliq-evoware/dist/equipment/sealer-Tecan.js'),
};

const evowareSpec = {
	namespace: "ourlab",
	name: "mario",
	config: {
		TEMPDIR: "C:\\Users\\localadmin\\Desktop\\Ellis\\temp",
		ROBOLIQ: "wscript C:\\Users\\localadmin\\Desktop\\Ellis\\roboliq\\roboliq-runtime-cli\\roboliq-runtime-cli.vbs",
		BROWSER: "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe"
	},
	sites: {
		"CENTRIFUGE_1": { "evowareCarrier": "Centrifuge", "evowareGrid": 54, "evowareSite": 1, "closed": true },
		"CENTRIFUGE_2": { "evowareCarrier": "Centrifuge", "evowareGrid": 54, "evowareSite": 2, "closed": true },
		"CENTRIFUGE_3": { "evowareCarrier": "Centrifuge", "evowareGrid": 54, "evowareSite": 1, "closed": true },
		"CENTRIFUGE_4": { "evowareCarrier": "Centrifuge", "evowareGrid": 54, "evowareSite": 2, "closed": true },
		"HOTEL4_1": { "evowareCarrier": "Hotel 4Pos Transfer Grid 69", "evowareGrid": 69, "evowareSite": 1 },
		"HOTEL4_2": { "evowareCarrier": "Hotel 4Pos Transfer Grid 69", "evowareGrid": 69, "evowareSite": 2 },
		"HOTEL4_3": { "evowareCarrier": "Hotel 4Pos Transfer Grid 69", "evowareGrid": 69, "evowareSite": 3 },
		"HOTEL4_4": { "evowareCarrier": "Hotel 4Pos Transfer Grid 69", "evowareGrid": 69, "evowareSite": 4 },
		"HOTEL32_A1": { "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 4 },
		"HOTEL32_B1": { "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 3 },
		"HOTEL32_C1": { "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 2 },
		"HOTEL32_D1": { "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 1 },
		"HOTEL32_A2": { "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 8 },
		"HOTEL32_B2": { "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 7 },
		"HOTEL32_C2": { "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 6 },
		"HOTEL32_D2": { "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 5 },
		"HOTEL32_A3": { "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 12 },
		"HOTEL32_B3": { "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 11 },
		"HOTEL32_C3": { "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 10 },
		"HOTEL32_D3": { "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 9 },
		"HOTEL32_A4": { "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 16 },
		"HOTEL32_B4": { "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 15 },
		"HOTEL32_C4": { "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 14 },
		"HOTEL32_D4": { "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 13 },
		"HOTEL32_A5": { "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 20 },
		"HOTEL32_B5": { "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 19 },
		"HOTEL32_C5": { "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 18 },
		"HOTEL32_D5": { "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 17 },
		"HOTEL32_A6": { "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 24 },
		"HOTEL32_B6": { "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 23 },
		"HOTEL32_C6": { "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 22 },
		"HOTEL32_D6": { "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 21 },
		"HOTEL32_A7": { "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 28 },
		"HOTEL32_B7": { "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 27 },
		"HOTEL32_C7": { "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 26 },
		"HOTEL32_D7": { "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 25 },
		"HOTEL32_A8": { "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 32 },
		"HOTEL32_B8": { "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 31 },
		"HOTEL32_C8": { "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 30 },
		"HOTEL32_D8": { "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 29 },
		"P1DOWNHOLDER": { "evowareCarrier": "Downholder", "evowareGrid": 9, "evowareSite": 3 },
		"P2": { "evowareCarrier": "MP 2Pos H+P Shake", "evowareGrid": 10, "evowareSite": 2 },
		"P3": { "evowareCarrier": "MP 2Pos H+P Shake", "evowareGrid": 10, "evowareSite": 4 },
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
		READER: { evowareCarrier: "Infinite M200", evowareGrid: 61, evowareSite: 1, closed: true },
		REGRIP: { "evowareCarrier": "ReGrip Station", "evowareGrid": 59, "evowareSite": 1 },
		ROBOPEEL: { "evowareCarrier": "RoboPeel", "evowareGrid": 12, "evowareSite": 1 },
		ROBOSEAL: { "evowareCarrier": "RoboSeal", "evowareGrid": 35, "evowareSite": 1 },
		SYSTEM: { "evowareCarrier": "System", "evowareGrid": -1, "evowareSite": 0 },
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
			"label": "48 flower-well plate",
			"rows": 6,
			"columns": 8,
			"evowareName": "Ellis 48 Flower Plate"
		},
		"plateModel_96_pcr": {
			"type": "PlateModel",
			"label": "96 well PCR plate",
			"rows": 8,
			"columns": 12,
			"evowareName": "D-BSSE 96 Well PCR Plate"
		},
		"plateModel_96_dwp": {
			"type": "PlateModel",
			"label": "96 well deep-well plate",
			"rows": 8,
			"columns": 12,
			"evowareName": "D-BSSE 96 Well DWP"
		},
		"plateModel_96_round_transparent_nunc": {
			"type": "PlateModel",
			"label": "96 round-well transparent Nunc plate",
			"rows": 8,
			"columns": 12,
			"evowareName": "Ellis Nunc F96 MicroWell"
		},
		"plateModel_96_square_transparent_nunc": {
			"type": "PlateModel",
			"label": "96 square-well transparent Nunc plate",
			"rows": 8,
			"columns": 12,
			"evowareName": "Ellis Nunc F96 MicroWell"
		},
		"plateModel_384_round": {
			"type": "PlateModel",
			"label": "384 round-well plate",
			"rows": 16,
			"columns": 24,
			"evowareName": "D-BSSE 384 Well Plate White"
		},
		"plateModel_384_square": {
			"type": "PlateModel",
			"label": "384 square-flat-well white plate",
			"rows": 16,
			"columns": 24,
			"evowareName": "D-BSSE 384 Well Plate White"
		},
		"plateModel_384_square_transparent_greiner": {
			"type": "PlateModel",
			"label": "384 square-flat-well transparent Greiner",
			"rows": 16,
			"columns": 24,
			"evowareName": "384 Sqr Flat Trans Greiner"
		},
		"plateModel_96_round_filter_OV": {
			"type": "PlateModel",
			"label": "96 round-well white filter plate",
			"rows": 8,
			"columns": 12,
			"evowareName": "OV 96 Well Filter Plate wLid"
		},
		"plateModel_96_round_deep_OV": {
			"type": "PlateModel",
			"label": "96 round-well white filter plate",
			"rows": 8,
			"columns": 12,
			"evowareName": "OV 96 DW Lid Ritter" // ToDo: Verify this
		},
		"troughModel_100ml": {
			"type": "PlateModel",
			"label": "Trough 100ml",
			"rows": 8,
			"columns": 1,
			"evowareName": "Trough 100ml"
		},
		"troughModel_25ml": {
			"type": "PlateModel",
			"label": "Trough 25ml",
			"rows": 8,
			"columns": 1,
			"evowareName": "Trough 25ml"
		},
		"tubeHolderModel_1500ul": {
			"type": "PlateModel",
			"label": "20 tube block 1.5ml",
			"rows": 4,
			"columns": 5,
			"evowareName": "Block 20Pos 1.5 ml Eppendorf"
		},
		"tubeHolderModel_15ml": {
			"type": "PlateModel",
			"label": "8 tube block 15ml",
			"rows": 8,
			"columns": 1,
			"evowareName": "Reagent Cooled 8*15ml"
		},
		"tubeHolderModel_50ml": {
			"type": "PlateModel",
			"label": "8 tube block 50ml",
			"rows": 8,
			"columns": 1,
			"evowareName": "Reagent Cooled 8*50ml"
		}
	},
	siteModelCompatibilities: [
		{
			description: "Centrifuge sites"
			sites: ["CENTRIFUGE_1", "CENTRIFUGE_2", "CENTRIFUGE_3", "CENTRIFUGE_4"],
			models: ["plateModel_96_round_transparent_nunc", "plateModel_96_square_transparent_nunc", "plateModel_384_square", "plateModel_96_dwp"]
		},
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
	safeLidStacking: [
		{
			lids: ["lidModel_standard"],
			models: [
				"ourlab.model.plateModel_96_round_transparent_nunc",
				"ourlab.model.plateModel_96_square_transparent_nunc",
				"ourlab.model.plateModel_384_round",
				"ourlab.model.plateModel_384_square",
				"ourlab.model.plateModel_96_round_filter_OV",
				"ourlab.model.plateModel_96_round_deep_OV"
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
			safeCliques: [
				{
					vector: "Narrow",
					cliques: [
						["P2", "P3", "P4", "P5", "P6", "P7", "P8", "REGRIP"],
						["CENTRIFUGE_1", "REGRIP"],
						["CENTRIFUGE_2", "REGRIP"],
						["CENTRIFUGE_3", "REGRIP"],
						["CENTRIFUGE_4", "REGRIP"],
					]
				}
			]
		},
		{
			description: "roma2",
			safeCliques: [
				{
					vector: "Narrow",
					cliques: [
						[
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
						],
						[
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
						],
					],
				},
				{
					vector: "Wide",
					cliques: [
						["READER", "REGRIP"]
					]
				}
			]
		}
	],
	tipModels: {
		"tipModel1000": {"programCode": "1000", "min": "3ul", "max": "950ul", "canHandleSeal": false, "canHandleCells": true},
		"tipModel0050": {"programCode": "0050", "min": "0.25ul", "max": "45ul", "canHandleSeal": true, "canHandleCells": false}
	},
	liha: {
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
	}
	equipment: {
		centrifuge: {
			module: "centrifuge-X.js",
			evowareId: "Centrifuge",
			sitesInternal: ["CENTRIFUGE_1", "CENTRIFUGE_2", "CENTRIFUGE_3", "CENTRIFUGE_4"]
		},
		reader: {
			module: "reader-InfiniteM200Pro.js",
			evowareId: "ReaderNETwork",
			sitesInternal: ["READER"],
			modelToPlateFile: {
				"plateModel_96_round_transparent_nunc": "NUN96ft",
				"plateModel_384_square": "GRE384fw",
				"EK_384_greiner_flat_bottom": "GRE384fw",
				"EK_96_well_Greiner_Black": "GRE96fb_chimney"
			}
		}
		shaker: {
			module: "shaker-Tecan1.js",
			evowareId: "HPShaker",
			site: "SHAKER"
		},
		sealer: {
			module: "sealer-Tecan.js",
			evowareId: "RoboSeal",
			site: "ROBOSEAL",
			modelToPlateFile: [
				plateModel_96_round_transparent_nunc: "C:\\HJBioanalytikGmbH\\RoboSeal3\\RoboSeal_PlateParameters\\PerkinElmer_weiss.bcf",
				plateModel_96_square_transparent_nunc: "C:\\HJBioanalytikGmbH\\RoboSeal3\\RoboSeal_PlateParameters\\PerkinElmer_weiss.bcf",
				plateModel_384_square: "C:\\HJBioanalytikGmbH\\RoboSeal3\\RoboSeal_PlateParameters\\Greiner_384_schwarz.bcf",
			]
		}
	}
});


const Equipment = {
	evoware: require('roboliq-evoware/dist/equipment/evoware.js'),
	reader: require('roboliq-evoware/dist/equipment/reader-InfiniteM200Pro.js'),
	sealer: require('roboliq-evoware/dist/equipment/sealer-Tecan.js'),
};


module.exports = {

	predicates: _.flatten([
		_.map(["ourlab.model.plateModel_384_square", "ourlab.model.plateModel_96_round_transparent_nunc", "ourlab.model.plateModel_96_dwp"], function(model) {
			return {"centrifuge.canAgentEquipmentModelSite1Site2": {
				"agent": "ourlab.mario.evoware",
				"equipment": "ourlab.mario.centrifuge",
				"model": model,
				"site1": "ourlab.mario.site.CENTRIFUGE_2",
				"site2": "ourlab.mario.site.CENTRIFUGE_4"
			}};
		}),
		{"#for": {
			factors: {model: ["plateModel_384_square", "plateModel_96_round_transparent_nunc", "EK_96_well_Greiner_Black", "EK_384_greiner_flat_bottom"]},
			output: {
				"absorbanceReader.canAgentEquipmentModelSite": {
					"agent": "ourlab.mario.evoware",
					"equipment": "ourlab.mario.reader",
					"model": "ourlab.model.{{model}}",
					"site": "ourlab.mario.site.READER"
				}
			}
		}},
		{"#for": {
			factors: {model: ["plateModel_384_square", "plateModel_96_round_transparent_nunc", "EK_96_well_Greiner_Black", "EK_384_greiner_flat_bottom"]},
			output: {
				"fluorescenceReader.canAgentEquipmentModelSite": {
					"agent": "ourlab.mario.evoware",
					"equipment": "ourlab.mario.reader",
					"model": "ourlab.model.{{model}}",
					"site": "ourlab.mario.site.READER"
				}
			}
		}},
		{"#for": {
			factors: {site: ["P1DOWNHOLDER", "P2", "P3", "P4", "P4PCR", "P5", "P6", "P7", "P8", "R1", "R2", "R3", "R4", "R5", "R6", "SYSTEM", "T1", "T2", "T3"]},
			output: {
				"pipetter.canAgentEquipmentSite": {
					"agent": "ourlab.mario.evoware",
					"equipment": "ourlab.mario.liha",
					"site": "ourlab.mario.site.{{site}}"
				}
			}
		}},
		{"#for": {
			factors: {i: [1, 2, 3, 4, 5, 6, 7, 8]},
			output: {
				"pipetter.canAgentEquipmentSyringe": {
					"agent": "ourlab.mario.evoware",
					"equipment": "ourlab.mario.liha",
					"syringe": "ourlab.mario.liha.syringe.{{i}}"
				}
			}
		}},
		_.map([1,2,3,4], function(n) {
			return {"method": {"description": "generic.closeSite-CENTRIFUGE_"+n,
				"task": {"generic.closeSite": {"site": "?site"}},
				"preconditions": [
					{"same": {"thing1": "?site", "thing2": "ourlab.mario.site.CENTRIFUGE_"+n}}
				],
				"subtasks": {"ordered": [
					{"ourlab.mario.centrifuge.close": {"agent": "ourlab.mario.evoware", "equipment": "ourlab.mario.centrifuge"}}
				]}
			}}
		}),
		{"action": {"description": "ourlab.mario.centrifuge.close: close the centrifuge",
			"task": {"ourlab.mario.centrifuge.close": {"agent": "?agent", "equipment": "?equipment"}},
			"preconditions": [],
			"deletions": [],
			"additions": [
				{"siteIsClosed": {"site": "ourlab.mario.site.CENTRIFUGE_1"}},
				{"siteIsClosed": {"site": "ourlab.mario.site.CENTRIFUGE_2"}},
				{"siteIsClosed": {"site": "ourlab.mario.site.CENTRIFUGE_3"}},
				{"siteIsClosed": {"site": "ourlab.mario.site.CENTRIFUGE_4"}}
			]
		}},
		_.map([1,2,3,4], function(n) {
			return {"method": {"description": "generic.openSite-CENTRIFUGE_"+n,
				"task": {"generic.openSite": {"site": "?site"}},
				"preconditions": [{"same": {"thing1": "?site", "thing2": "ourlab.mario.site.CENTRIFUGE_"+n}}],
				"subtasks": {"ordered": [_.fromPairs([["ourlab.mario.centrifuge.open"+n, {}]])]}
			}};
		}),
		_.map([1,2,3,4], function(n) {
			return {"action": {"description": "ourlab.mario.centrifuge.open: open an internal site on the centrifuge",
				"task": _.fromPairs([["ourlab.mario.centrifuge.open"+n, {}]]),
				"preconditions": [],
				"deletions": [
					{"siteIsClosed": {"site": "ourlab.mario.site.CENTRIFUGE_"+n}}
				],
				"additions": _.map(_.without([1,2,3,4], n), function(n2) {
					return {"siteIsClosed": {"site": "ourlab.mario.site.CENTRIFUGE_"+n2}};
				})
			}};
		}),
		Equipment.reader.getPredicates("ourlab.mario.evoware", "ourlab.mario.reader", "ourlab.mario.site.READER")
	]),

	schemas: _.merge(
		{
			"equipment.close|ourlab.mario.evoware|ourlab.mario.centrifuge": {
				properties: {
					agent: {description: "Agent identifier", type: "Agent"},
					equipment: {description: "Equipment identifier", type: "Equipment"},
				},
				required: ["agent", "equipment"]
			},
			"equipment.open|ourlab.mario.evoware|ourlab.mario.centrifuge": {
				properties: {
					agent: {description: "Agent identifier", type: "Agent"},
					equipment: {description: "Equipment identifier", type: "Equipment"},
				},
				required: ["agent", "equipment"]
			},
			"equipment.openSite|ourlab.mario.evoware|ourlab.mario.centrifuge": {
				properties: {
					agent: {description: "Agent identifier", type: "Agent"},
					equipment: {description: "Equipment identifier", type: "Equipment"},
					site: {description: "Site identifier", type: "Site"}
				},
				required: ["agent", "equipment", "site"]
			},
			"equipment.run|ourlab.mario.evoware|ourlab.mario.centrifuge": {
				properties: {
					agent: {description: "Agent identifier", type: "Agent"},
					equipment: {description: "Equipment identifier", type: "Equipment"},
					program: {
						description: "Program for centrifuging",
						type: "object",
						properties: {
							rpm: {type: "number", default: 3000},
							duration: {type: "Duration", default: "30 s"},
							spinUpTime: {type: "Duration", default: "9 s"},
							spinDownTime: {type: "Duration", default: "9 s"},
							temperature: {type: "Temperature", default: "25 degC"}
						}
					}
				},
				required: ["program"]
			},
			"shaker.run|ourlab.mario.evoware|ourlab.mario.shaker": {
				properties: {
					agent: {description: "Agent identifier", type: "Agent"},
					equipment: {description: "Equipment identifier", type: "Equipment"},
					//program: {description: "Program for shaking", type: "object"}
					program: {
						description: "Program for shaking",
						properties: {
							rpm: {description: "Rotations per minute (RPM)", type: "number"},
							duration: {description: "Duration of shaking", type: "Duration"}
						},
						required: ["duration"]
					}
				},
				required: ["agent", "equipment", "program"]
			},
			"pipetter.cleanTips|ourlab.mario.evoware|ourlab.mario.liha": {
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
		Equipment.reader.getSchemas("ourlab.mario.evoware", "ourlab.mario.reader"),
	),

	commandHandlers: _.merge(
		{
			"equipment.close|ourlab.mario.evoware|ourlab.mario.centrifuge": function(params, parsed, data) {
				return {expansion: [makeEvowareFacts(parsed, data, "Close")]};
			},
			"equipment.open|ourlab.mario.evoware|ourlab.mario.centrifuge": function(params, parsed, data) {
				return {expansion: [makeEvowareFacts(parsed, data, "Open")]};
			},
			"equipment.openSite|ourlab.mario.evoware|ourlab.mario.centrifuge": function(params, parsed, data) {
				var carrier = commandHelper.getParsedValue(parsed, data, "equipment", "evowareId");
				var sitesInternal = commandHelper.getParsedValue(parsed, data, "equipment", "sitesInternal");
				var siteIndex = sitesInternal.indexOf(parsed.objectName.site);
				expect.truthy({paramName: "site"}, siteIndex >= 0, "site must be one of the equipments internal sites: "+sitesInternal.join(", "));
				return {
					expansion: [
						{
							command: "evoware._facts",
							agent: parsed.objectName.agent,
							factsEquipment: carrier,
							factsVariable: carrier+"_MoveToPos",
							factsValue: (siteIndex+1).toString()
						},
						{
							command: "evoware._facts",
							agent: parsed.objectName.agent,
							factsEquipment: carrier,
							factsVariable: carrier+"_Open"
						},
					]
				};
			},
			"equipment.run|ourlab.mario.evoware|ourlab.mario.centrifuge": function(params, parsed, data) {
				//console.log("equipment.run|ourlab.mario.evoware|ourlab.mario.centrifuge:")
				//console.log({parsed, params})
				const parsedProgram = parsed.value.program;
				//console.log({parsedProgram});
				var list = [
					math.round(parsedProgram.rpm),
					math.round(parsedProgram.duration.toNumber('s')),
					math.round(parsedProgram.spinUpTime.toNumber('s')),
					math.round(parsedProgram.spinDownTime.toNumber('s')),
					math.round(parsedProgram.temperature.toNumber('degC'))
				];
				var value = list.join(",");
				return {expansion: [makeEvowareFacts(parsed, data, "Execute1", value)]};
			},
			// Shaker
			"shaker.run|ourlab.mario.evoware|ourlab.mario.shaker": function(params, parsed, data) {
				//console.log("equipment.run|ourlab.mario.evoware|ourlab.mario.shaker: "+JSON.stringify(parsed, null, '\t'))
				const equipmentId = commandHelper.getParsedValue(parsed, data, "equipment", "evowareId");
				const rpm = parsed.value.program.rpm || 750;

				// Construct the shaker program data
				const shakerNo = 1;
				// FIXME: Let the user specify mode1, steps1, mode2, steps2, power
				const mode1 = 2;
				const steps1 = 0;
				const mode2 = 1;
				const steps2 = 0;
				const freq = (60000000/rpm).toFixed(0);
				const cycles = Math.floor(rpm * parsed.value.program.duration.toNumber("minute")).toFixed(0);
				const powerPerc = 50;
				const power = Math.floor(255 * powerPerc / 100).toFixed(0);
				const s0 = `*27${shakerNo}|${freq}|${mode1}|${steps1}|${mode2}|${steps2}|${cycles}|${power}*27`;
				// Replace all occurences of '0' with "*30"
				const s1 = s0.replace(/0/g, "*30");
				// Finally, split string into 32 char parts, then rebind them, separated by commas
				const s2 = _(s1).chunk(32).map(s => s.join("")).join(",");

				return {
					expansion: [
						{
							command: "evoware._facts",
							agent: parsed.objectName.agent,
							factsEquipment: equipmentId,
							factsVariable: equipmentId+"_HP__Start",
							factsValue: s2
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
							factsVariable: equipmentId+"_HP__Stop",
							factsValue: "1"
						},
					]
				};
			},
			// Clean tips
			"pipetter.cleanTips|ourlab.mario.evoware|ourlab.mario.liha": function(params, parsed, data) {
				//console.log("pipetter.cleanTips|ourlab.mario.evoware|ourlab.mario.liha")
				//console.log(JSON.stringify(parsed, null, '  '))

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
								program: `ourlab.mario.washProgram.${intensity}_${volume}`,
								intensity: intensity,
								syringes: syringeNames
							});
						}
					}
				}
				sub(_.map([1, 2, 3, 4], n => `ourlab.mario.liha.syringe.${n}`), "1000");
				sub(_.map([5, 6, 7, 8], n => `ourlab.mario.liha.syringe.${n}`), "0050");
				return {expansion: expansionList};
			},
			"scale.weigh": function(params, parsed, data) {
				console.log({params, parsed})
				const expansion = [
					{
						command: "evoware._userPrompt",
						agent: parsed.objectName.agent,
						equipment: parsed.objectName.equipment,
						text: "Weigh "+parsed.objectName.object,
						beep: 2 // beep three times
					}
				];

				return {expansion};
			},
		},
		Equipment.evoware.getCommandHandlers(),
		Equipment.reader.getCommandHandlers("ourlab.mario.evoware", "ourlab.mario.reader"),
	),

	planHandlers: _.merge(
		{
			"ourlab.mario.centrifuge.close": function(params, parentParams, data) {
				return [{
					command: "equipment.close",
					agent: "ourlab.mario.evoware",
					equipment: "ourlab.mario.centrifuge"
				}];
			},
			"ourlab.mario.centrifuge.open1": function(params, parentParams, data) {
				return [{
					command: "equipment.openSite",
					agent: "ourlab.mario.evoware",
					equipment: "ourlab.mario.centrifuge",
					site: "ourlab.mario.site.CENTRIFUGE_1"
				}];
			},
			"ourlab.mario.centrifuge.open2": function(params, parentParams, data) {
				return [{
					command: "equipment.openSite",
					agent: "ourlab.mario.evoware",
					equipment: "ourlab.mario.centrifuge",
					site: "ourlab.mario.site.CENTRIFUGE_2"
				}];
			},
			"ourlab.mario.centrifuge.open3": function(params, parentParams, data) {
				return [{
					command: "equipment.openSite",
					agent: "ourlab.mario.evoware",
					equipment: "ourlab.mario.centrifuge",
					site: "ourlab.mario.site.CENTRIFUGE_3"
				}];
			},
			"ourlab.mario.centrifuge.open4": function(params, parentParams, data) {
				return [{
					command: "equipment.openSite",
					agent: "ourlab.mario.evoware",
					equipment: "ourlab.mario.centrifuge",
					site: "ourlab.mario.site.CENTRIFUGE_4"
				}];
			},
		},
	)
};
