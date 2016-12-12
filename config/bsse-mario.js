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

module.exports = {
	roboliq: "v1",
	objects: {
		"ourlab": {
			"type": "Namespace",
			"mario": {
				"type": "Namespace",
				"centrifuge": {
					"type": "Centrifuge",
					"sitesInternal": ["ourlab.mario.site.CENTRIFUGE_1", "ourlab.mario.site.CENTRIFUGE_2", "ourlab.mario.site.CENTRIFUGE_3", "ourlab.mario.site.CENTRIFUGE_4"],
					"evowareId": "Centrifuge"
				},
				"evoware": {
					"type": "EvowareRobot",
					config: {
						TEMPDIR: "C:\\Users\\localadmin\\Desktop\\Ellis\\temp",
						ROBOLIQ: "wscript C:\\Users\\localadmin\\Desktop\\Ellis\\roboliq\\roboliq-runtime-cli\\roboliq-runtime-cli.vbs",
						BROWSER: "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe"
					}
				},
				"reader": {
					"type": "Reader",
					"sitesInternal": ["ourlab.mario.site.READER"],
					"evowareId": "ReaderNETwork",
					"modelToPlateFile": {
						"ourlab.model.plateModel_96_round_transparent_nunc": "NUN96ft",
						"ourlab.model.plateModel_384_square": "Which One?"
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
							"tipModel": "ourlab.mario.liha.tipModel.tipModel1000",
							"tipModelPermanent": "ourlab.mario.liha.tipModel.tipModel1000",
							"row": 1
						},
						"2": {
							"type": "Syringe",
							"tipModel": "ourlab.mario.liha.tipModel.tipModel1000",
							"tipModelPermanent": "ourlab.mario.liha.tipModel.tipModel1000",
							"row": 2
						},
						"3": {
							"type": "Syringe",
							"tipModel": "ourlab.mario.liha.tipModel.tipModel1000",
							"tipModelPermanent": "ourlab.mario.liha.tipModel.tipModel1000",
							"row": 3
						},
						"4": {
							"type": "Syringe",
							"tipModel": "ourlab.mario.liha.tipModel.tipModel1000",
							"tipModelPermanent": "ourlab.mario.liha.tipModel.tipModel1000",
							"row": 4
						},
						"5": {
							"type": "Syringe",
							"tipModel": "ourlab.mario.liha.tipModel.tipModel0050",
							"tipModelPermanent": "ourlab.mario.liha.tipModel.tipModel0050",
							"row": 5
						},
						"6": {
							"type": "Syringe",
							"tipModel": "ourlab.mario.liha.tipModel.tipModel0050",
							"tipModelPermanent": "ourlab.mario.liha.tipModel.tipModel0050",
							"row": 6
						},
						"7": {
							"type": "Syringe",
							"tipModel": "ourlab.mario.liha.tipModel.tipModel0050",
							"tipModelPermanent": "ourlab.mario.liha.tipModel.tipModel0050",
							"row": 7
						},
						"8": {
							"type": "Syringe",
							"tipModel": "ourlab.mario.liha.tipModel.tipModel0050",
							"tipModelPermanent": "ourlab.mario.liha.tipModel.tipModel0050",
							"row": 8
						}
					},
					"tipModel": {
						"tipModel1000": {"type": "TipModel", "programCode": "1000", "min": "3ul", "max": "950ul", "canHandleSeal": false, "canHandleCells": true},
						"tipModel0050": {"type": "TipModel", "programCode": "0050", "min": "0.25ul", "max": "45ul", "canHandleSeal": true, "canHandleCells": false}
					},
					"tipModelToSyringes": {
						"ourlab.mario.liha.tipModel.tipModel1000": ["ourlab.mario.liha.syringe.1", "ourlab.mario.liha.syringe.2", "ourlab.mario.liha.syringe.3", "ourlab.mario.liha.syringe.4"],
						"ourlab.mario.liha.tipModel.tipModel0050": ["ourlab.mario.liha.syringe.5", "ourlab.mario.liha.syringe.6", "ourlab.mario.liha.syringe.7", "ourlab.mario.liha.syringe.8"]
					}
				},
				/*"peeler": {
					"type": "Peeler",
					"evowareId": "RoboPeel"
				},*/
				"sealer": {
					"type": "Sealer",
					"evowareId": "RoboSeal"
				},
				"shaker": {
					"type": "Shaker",
					"evowareId": "HPShaker"
				},
				"site": {
					"type": "Namespace",
					"CENTRIFUGE_1": { "type": "Site", "evowareCarrier": "Centrifuge", "evowareGrid": 54, "evowareSite": 1, "closed": true },
					"CENTRIFUGE_2": { "type": "Site", "evowareCarrier": "Centrifuge", "evowareGrid": 54, "evowareSite": 2, "closed": true },
					"CENTRIFUGE_3": { "type": "Site", "evowareCarrier": "Centrifuge", "evowareGrid": 54, "evowareSite": 1, "closed": true },
					"CENTRIFUGE_4": { "type": "Site", "evowareCarrier": "Centrifuge", "evowareGrid": 54, "evowareSite": 2, "closed": true },
					"HOTEL4_1": { "type": "Site", "evowareCarrier": "Hotel 4Pos Transfer Grid 69", "evowareGrid": 69, "evowareSite": 1 },
					"HOTEL4_2": { "type": "Site", "evowareCarrier": "Hotel 4Pos Transfer Grid 69", "evowareGrid": 69, "evowareSite": 2 },
					"HOTEL4_3": { "type": "Site", "evowareCarrier": "Hotel 4Pos Transfer Grid 69", "evowareGrid": 69, "evowareSite": 3 },
					"HOTEL4_4": { "type": "Site", "evowareCarrier": "Hotel 4Pos Transfer Grid 69", "evowareGrid": 69, "evowareSite": 4 },
					"HOTEL32_A1": { "type": "Site", "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 4 },
					"HOTEL32_B1": { "type": "Site", "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 3 },
					"HOTEL32_C1": { "type": "Site", "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 2 },
					"HOTEL32_D1": { "type": "Site", "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 1 },
					"HOTEL32_A2": { "type": "Site", "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 8 },
					"HOTEL32_B2": { "type": "Site", "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 7 },
					"HOTEL32_C2": { "type": "Site", "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 6 },
					"HOTEL32_D2": { "type": "Site", "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 5 },
					"HOTEL32_A3": { "type": "Site", "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 12 },
					"HOTEL32_B3": { "type": "Site", "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 11 },
					"HOTEL32_C3": { "type": "Site", "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 10 },
					"HOTEL32_D3": { "type": "Site", "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 9 },
					"HOTEL32_A4": { "type": "Site", "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 16 },
					"HOTEL32_B4": { "type": "Site", "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 15 },
					"HOTEL32_C4": { "type": "Site", "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 14 },
					"HOTEL32_D4": { "type": "Site", "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 13 },
					"HOTEL32_A5": { "type": "Site", "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 20 },
					"HOTEL32_B5": { "type": "Site", "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 19 },
					"HOTEL32_C5": { "type": "Site", "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 18 },
					"HOTEL32_D5": { "type": "Site", "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 17 },
					"HOTEL32_A6": { "type": "Site", "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 24 },
					"HOTEL32_B6": { "type": "Site", "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 23 },
					"HOTEL32_C6": { "type": "Site", "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 22 },
					"HOTEL32_D6": { "type": "Site", "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 21 },
					"HOTEL32_A7": { "type": "Site", "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 28 },
					"HOTEL32_B7": { "type": "Site", "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 27 },
					"HOTEL32_C7": { "type": "Site", "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 26 },
					"HOTEL32_D7": { "type": "Site", "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 25 },
					"HOTEL32_A8": { "type": "Site", "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 32 },
					"HOTEL32_B8": { "type": "Site", "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 31 },
					"HOTEL32_C8": { "type": "Site", "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 30 },
					"HOTEL32_D8": { "type": "Site", "evowareCarrier": "Shelf 32Pos Microplate", "evowareGrid": 11, "evowareSite": 29 },
					"P2": {
						"type": "Site",
						"evowareCarrier": "MP 2Pos H+P Shake",
						"evowareGrid": 10,
						"evowareSite": 2
					},
					"P3": {
						"type": "Site",
						"evowareCarrier": "MP 2Pos H+P Shake",
						"evowareGrid": 10,
						"evowareSite": 4
					},
					P4: { type: "Site", evowareCarrier: "MP 3Pos Cooled 1 PCR", evowareGrid: 17, evowareSite: 2 },
					P5: { type: "Site", evowareCarrier: "MP 3Pos Cooled 1 PCR", evowareGrid: 17, evowareSite: 4 },
					P6: { type: "Site", evowareCarrier: "MP 3Pos Cooled 2 PCR", evowareGrid: 24, evowareSite: 2 },
					P7: { type: "Site", evowareCarrier: "MP 3Pos Cooled 2 PCR", evowareGrid: 24, evowareSite: 4 },
					P8: { type: "Site", evowareCarrier: "MP 3Pos Cooled 2 PCR", evowareGrid: 24, evowareSite: 6 },
					R1: { type: "Site", evowareCarrier: "LI - Trough 3Pos 100ml", evowareGrid: 3, evowareSite: 1 },
					R2: { type: "Site", evowareCarrier: "LI - Trough 3Pos 100ml", evowareGrid: 3, evowareSite: 2 },
					R3: { type: "Site", evowareCarrier: "LI - Trough 3Pos 100ml", evowareGrid: 3, evowareSite: 3 },
					R4: { type: "Site", evowareCarrier: "LI - Trough 3Pos 100mlOffset", evowareGrid: 8, evowareSite: 1 },
					R5: { type: "Site", evowareCarrier: "LI - Trough 3Pos 100mlOffset", evowareGrid: 8, evowareSite: 2 },
					R6: { type: "Site", evowareCarrier: "LI - Trough 3Pos 100mlOffset", evowareGrid: 8, evowareSite: 3 },
					READER: { type: "Site", evowareCarrier: "Infinite M200", evowareGrid: 61, evowareSite: 1, closed: true },
					"REGRIP": {
						"type": "Site",
						"evowareCarrier": "ReGrip Station",
						"evowareGrid": 62,
						"evowareSite": 1
					},
					"ROBOPEEL": {
						"type": "Site",
						"evowareCarrier": "RoboPeel",
						"evowareGrid": 12,
						"evowareSite": 1
					},
					"ROBOSEAL": {
						"type": "Site",
						"evowareCarrier": "RoboSeal",
						"evowareGrid": 35,
						"evowareSite": 1
					},
					"SYSTEM": {
						"type": "Site",
						"evowareCarrier": "System",
						"evowareGrid": -1,
						"evowareSite": 0
					},
					T1: { type: "Site", evowareCarrier: "Cooled 8Pos*15ml 8Pos*50ml", evowareGrid: 4, evowareSite: 1 },
					T2: { type: "Site", evowareCarrier: "Cooled 8Pos*15ml 8Pos*50ml", evowareGrid: 4, evowareSite: 2 },
					T3: { type: "Site", evowareCarrier: "Block 20Pos", evowareGrid: 16, evowareSite: 1 },
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
						"ourlab.mario.systemLiquidLabware(A01)",
						"ourlab.mario.systemLiquidLabware(B01)",
						"ourlab.mario.systemLiquidLabware(C01)",
						"ourlab.mario.systemLiquidLabware(D01)"
					]
				},
				"systemLiquidLabware": {
					"type": "Plate",
					"description": "dummy labware representing the system liquid source",
					"model": "ourlab.mario.systemLiquidLabwareModel",
					"location": "ourlab.mario.site.SYSTEM",
					"contents": ["Infinity l", "systemLiquid"]
				},
				"timer1": {
					"type": "Timer",
					"evowareId": 1
				},
				"timer2": {
					"type": "Timer",
					"evowareId": 2
				},
				"timer3": {
					"type": "Timer",
					"evowareId": 2
				},
				"timer4": {
					"type": "Timer",
					"evowareId": 2
				},
				"timer5": {
					"type": "Timer",
					"evowareId": 2
				},
				"washProgram": {
					"type": "Namespace",
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
			"model": {
				"type": "Namespace",
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
			}
		}
	},

	objectToPredicateConverters: _.merge({},
		Equipment.evoware.objectToPredicateConverters
	),

	predicates: _.flatten([
		// Centrifuge sites
		makeSiteModelPredicates({
			siteModel: "ourlab.mario.siteModel_CENTRIFUGE",
			sites: ["CENTRIFUGE_1", "CENTRIFUGE_2", "CENTRIFUGE_3", "CENTRIFUGE_4"].map(s => "ourlab.mario.site."+s),
			labwareModels: ["ourlab.model.plateModel_96_round_transparent_nunc", "ourlab.model.plateModel_96_square_transparent_nunc", "ourlab.model.plateModel_384_square"]
		}),
		// short-format plate sites (non-tall labware, not for deep well plates)
		makeSiteModelPredicates({
			siteModel: "ourlab.mario.siteModel_short",
			sites: ["HOTEL4_1", "HOTEL4_2", "HOTEL4_3", "HOTEL4_4", "HOTEL32_A1", "HOTEL32_B1", "HOTEL32_C1", "HOTEL32_D1", "HOTEL32_A2", "HOTEL32_B2", "HOTEL32_C2", "HOTEL32_D2", "HOTEL32_A3", "HOTEL32_B3", "HOTEL32_C3", "HOTEL32_D3", "HOTEL32_A4", "HOTEL32_B4", "HOTEL32_C4", "HOTEL32_D4", "HOTEL32_A5", "HOTEL32_B5", "HOTEL32_C5", "HOTEL32_D5", "HOTEL32_A6", "HOTEL32_B6", "HOTEL32_C6", "HOTEL32_D6", "HOTEL32_A7", "HOTEL32_B7", "HOTEL32_C7", "HOTEL32_D7", "HOTEL32_A8", "HOTEL32_B8", "HOTEL32_C8", "HOTEL32_D8", "READER", "ROBOPEEL", "ROBOSEAL"].map(s => "ourlab.mario.site."+s),
			labwareModels: ["ourlab.model.plateModel_96_round_transparent_nunc", "ourlab.model.plateModel_96_square_transparent_nunc", "ourlab.model.plateModel_384_square"]
		}),
		// Bench sites that don't have any obstructions, so deep well plates can fit on them too
		makeSiteModelPredicates({
			siteModel: "ourlab.mario.siteModel_open",
			sites: ["P2", "P3", "P4", "P5", "P6", "P7", "P8", "REGRIP"].map(s => "ourlab.mario.site."+s),
			labwareModels: ["ourlab.model.plateModel_96_round_transparent_nunc", "ourlab.model.plateModel_96_square_transparent_nunc", "ourlab.model.plateModel_384_square", "plateModel_96_dwp"]
		}),
		makeTransporterPredicates("ourlab.mario", "ourlab.mario.evoware", {
			"ourlab.mario.roma1": {
				Narrow: [
					["P2", "P3", "P4", "P5", "P6", "P7", "P8", "REGRIP"].map(s => "ourlab.mario.site."+s),
					["CENTRIFUGE_1", "REGRIP"].map(s => "ourlab.mario.site."+s),
					["CENTRIFUGE_2", "REGRIP"].map(s => "ourlab.mario.site."+s),
					["CENTRIFUGE_3", "REGRIP"].map(s => "ourlab.mario.site."+s),
					["CENTRIFUGE_4", "REGRIP"].map(s => "ourlab.mario.site."+s),
				]
			},
			"ourlab.mario.roma2": {
				Narrow: [
					[
						"P1", "P2", "P3", "P6", "P7", "P8", "ROBOPEEL", "ROBOSEAL", "REGRIP",
						"HOTEL4_1", "HOTEL4_2", "HOTEL4_3", "HOTEL4_4",
						"HOTEL32_A1", "HOTEL32_B1", "HOTEL32_C1", "HOTEL32_D1",
						"HOTEL32_A2", "HOTEL32_B2", "HOTEL32_C2", "HOTEL32_D2",
						"HOTEL32_A3", "HOTEL32_B3", "HOTEL32_C3", "HOTEL32_D3",
						"HOTEL32_A4", "HOTEL32_B4", "HOTEL32_C4", "HOTEL32_D4",
						"HOTEL32_A5", "HOTEL32_B5", "HOTEL32_C5", "HOTEL32_D5",
						"HOTEL32_A6", "HOTEL32_B6", "HOTEL32_C6", "HOTEL32_D6",
						"HOTEL32_A7", "HOTEL32_B7", "HOTEL32_C7", "HOTEL32_D7",
						"HOTEL32_A8", "HOTEL32_B8", "HOTEL32_C8", "HOTEL32_D8",
					].map(s => "ourlab.mario.site."+s)
				],
				Wide: [
					["READER", "REGRIP"].map(s => "ourlab.mario.site."+s)
				]
			}
		}),
		_.map(["ourlab.model.plateModel_384_square", "ourlab.model.plateModel_96_round_transparent_nunc"], function(model) {
			return {"centrifuge.canAgentEquipmentModelSite1Site2": {
				"agent": "ourlab.mario.evoware",
				"equipment": "ourlab.mario.centrifuge",
				"model": model,
				"site1": "ourlab.mario.site.CENTRIFUGE_2",
				"site2": "ourlab.mario.site.CENTRIFUGE_4"
			}};
		}),
		{"#for": {
			factors: {model: ["plateModel_384_square", "plateModel_96_round_transparent_nunc"]},
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
			factors: {model: ["plateModel_384_square", "plateModel_96_round_transparent_nunc"]},
			output: {
				"fluorescenceReader.canAgentEquipmentModelSite": {
					"agent": "ourlab.mario.evoware",
					"equipment": "ourlab.mario.reader",
					"model": "ourlab.model.{{model}}",
					"site": "ourlab.mario.site.READER"
				}
			}
		}},
		{
			"sealer.canAgentEquipmentProgramModelSite": {
				"agent": "ourlab.mario.evoware",
				"equipment": "ourlab.mario.sealer",
				"program": "C:\\HJBioanalytikGmbH\\RoboSeal3\\RoboSeal_PlateParameters\\PerkinElmer_weiss.bcf",
				"model": "ourlab.model.plateModel_96_round_transparent_nunc",
				"site": "ourlab.mario.site.ROBOSEAL"
			}
		},
		{
			"sealer.canAgentEquipmentProgramModelSite": {
				"agent": "ourlab.mario.evoware",
				"equipment": "ourlab.mario.sealer",
				"program": "C:\\HJBioanalytikGmbH\\RoboSeal3\\RoboSeal_PlateParameters\\PerkinElmer_weiss.bcf",
				"model": "ourlab.model.plateModel_96_square_transparent_nunc",
				"site": "ourlab.mario.site.ROBOSEAL"
			}
		},
		{
			"sealer.canAgentEquipmentProgramModelSite": {
				"agent": "ourlab.mario.evoware",
				"equipment": "ourlab.mario.sealer",
				"program": "C:\\HJBioanalytikGmbH\\RoboSeal3\\RoboSeal_PlateParameters\\Greiner_384_schwarz.bcf",
				"model": "ourlab.model.plateModel_384_square",
				"site": "ourlab.mario.site.ROBOSEAL"
			}
		},
		{
			"shaker.canAgentEquipmentSite": {
				"agent": "ourlab.mario.evoware",
				"equipment": "ourlab.mario.shaker",
				"site": "ourlab.mario.site.P3"
			}
		},
		{
			"shaker.canAgentEquipmentSite": {
				"agent": "ourlab.mario.evoware",
				"equipment": "ourlab.mario.reader",
				"site": "ourlab.mario.site.READER"
			}
		},
		{
			"pipetter.canAgentEquipment": {
				"agent": "ourlab.mario.evoware",
				"equipment": "ourlab.mario.liha"
			}
		},
		{"#for": {
			factors: {site: ["P2", "P3", "P4", "P5", "P6", "P7", "P8", "R1", "R2", "R3", "R4", "R5", "R6", "SYSTEM", "T1", "T2", "T3"]},
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
		{"timer.canAgentEquipment": {
			"agent": "ourlab.mario.evoware",
			"equipment": "ourlab.mario.timer1",
		}},
		{"timer.canAgentEquipment": {
			"agent": "ourlab.mario.evoware",
			"equipment": "ourlab.mario.timer2",
		}},
		{"timer.canAgentEquipment": {
			"agent": "ourlab.mario.evoware",
			"equipment": "ourlab.mario.timer3",
		}},
		{"timer.canAgentEquipment": {
			"agent": "ourlab.mario.evoware",
			"equipment": "ourlab.mario.timer4",
		}},
		{"timer.canAgentEquipment": {
			"agent": "ourlab.mario.evoware",
			"equipment": "ourlab.mario.timer5",
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
		Equipment.sealer.getSchemas("ourlab.mario.evoware", "ourlab.mario.sealer")
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
		Equipment.sealer.getCommandHandlers("ourlab.mario.evoware", "ourlab.mario.sealer")
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
		Equipment.reader.getPlanHandlers("ourlab.mario.evoware", "ourlab.mario.reader", "ourlab.mario.site.READER")
	)
}
