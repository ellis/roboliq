import _ from 'lodash';
import should from 'should';
import jsonfile from 'jsonfile';
import * as EvowareCarrierFile from '../src/evoware/EvowareCarrierFile.js';
import * as EvowareCompiler from '../src/evoware/EvowareCompiler.js';
import * as EvowareTableFile from '../src/evoware/EvowareTableFile.js';
import schemas from './schemas.js';

const protocol0 = {
	roboliq: "v1",
	objects: {
		robot1: {
			type: "Agent",
			"washProgram": {
				"type": "Namespace",
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
			},
		},
		timer1: {
			type: "Timer",
			evowareId: 1
		},
		timer2: {
			type: "Timer",
			evowareId: 2
		},
		transporter1: {
			type: "Transporter",
			evowareRoma: 0,
		},
		pipetter1: {
			type: "Pipetter",
			syringe: {
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
			}
		},
		plateModel1: {
			type: "PlateModel",
			rows: 8,
			columns: 12,
			evowareName: "96-Well Plate"
		},
		site1: {
			type: "Site",
			evowareCarrier: "Some Carrier",
			evowareGrid: 1,
			evowareSite: 1
		},
		site2: {
			type: "Site",
			evowareCarrier: "Some Carrier",
			evowareGrid: 1,
			evowareSite: 2
		},
		plate1: {
			type: "Plate",
			model: "plateModel1",
			location: "site1"
		}
	},
	predicates: [
		{"timer.canAgentEquipment": {agent: "robot1", equipment: "timer1"}},
		{"timer.canAgentEquipment": {agent: "robot1", equipment: "timer2"}},
	],
	schemas
};

describe('EvowareCompilerTest', function() {
	describe('compileStep', function () {
		it('should compile timer._wait', function () {
			const table = {};
			const protocol = _.merge({}, protocol0, {
				roboliq: "v1",
				steps: {
					1: {
						command: "timer._start",
						agent: "robot1",
						equipment: "timer1"
					},
					2: {
						command: "timer._wait",
						agent: "robot1",
						equipment: "timer1",
						till: "1 minute",
						stop: true
					}
				}
			});
			const agents = ["robot1"];
			const results = EvowareCompiler.compileStep(table, protocol, agents, [], undefined, {timing: false});
			should.deepEqual(results, [
				[{line: 'StartTimer("1");'}],
				[{line: 'WaitTimer("1","60");'}]
			]);
		});

		it("should compile evoware._facts", function() {
			const table = {};
			const protocol = _.merge({}, protocol0, {
				roboliq: "v1",
				steps: {
					1: {
						"command": "evoware._facts",
						"agent": "robot1",
						"factsEquipment": "RoboSeal",
						"factsVariable": "RoboSeal_Seal",
						"factsValue": "VALUE",
						"labware": "plate1"
					}
				}
			});
			const agents = ["robot1"];
			const results = EvowareCompiler.compileStep(table, protocol, agents, [], undefined, {timing: false});
			should.deepEqual(results, [
				[{
					line: "FACTS(\"RoboSeal\",\"RoboSeal_Seal\",\"VALUE\",\"0\",\"\");",
					tableEffects: [
						[ [ "Some Carrier", 1, 1 ], { "label": "site1", "labwareModelName": "96-Well Plate" } ]
					]
				}]
			]);
		});

		it("should compile transporter._movePlate #1", function() {
			const table = {};
			const protocol = _.merge({}, protocol0, {
				roboliq: "v1",
				steps: {
					1: {
						"command": "transporter._movePlate",
						"agent": "robot1",
						"equipment": "transporter1",
						"object": "plate1",
						"destination": "site2"
					}
				}
			});
			const agents = ["robot1"];
			const results = EvowareCompiler.compileStep(table, protocol, agents, [], undefined, {timing: false});
			//console.log(JSON.stringify(results, null, '\t'))
			should.deepEqual(results, [
				[
					{
						"line": "Transfer_Rack(\"1\",\"1\",0,0,0,0,0,\"\",\"96-Well Plate\",\"undefined\",\"\",\"\",\"Some Carrier\",\"\",\"Some Carrier\",\"1\",\"(Not defined)\",\"2\");",
						"effects": {
							"plate1.location": "site2",
							"EVOWARE.romaIndexPrev": 0
						},
						"tableEffects": [
							[ [ "Some Carrier", 1, 1 ], { "label": "site1", "labwareModelName": "96-Well Plate" } ],
							[ [ "Some Carrier", 1, 2 ], { "label": "site2", "labwareModelName": "96-Well Plate" } ]
						]
					}
				]
			]);
		});

		it("should compile transporter._movePlate #2", function() {
			const table = {};
			const protocol = _.merge({},
				require(__dirname+"/../src/config/roboliq.js"),
				require(__dirname+"/../src/config/ourlab.js"),
				protocol0,
				{
					roboliq: "v1",
					objects: {
						"stillPlate": {
							"type": "Plate",
							"model": "ourlab.model.plateModel_96_dwp",
							"location": "ourlab.mario.site.P4"
						}
					},
					steps: {
						1: {
							"command": "transporter._movePlate",
							"agent": "ourlab.mario.evoware",
							"equipment": "ourlab.mario.roma1",
							"program": "Narrow",
							"object": "stillPlate",
							"destination": "ourlab.mario.site.P3"
						}
					}
				}
			);
			const agents = ["ourlab.mario.evoware"];
			const results = EvowareCompiler.compileStep(table, protocol, agents, [], undefined, {timing: false});
			//console.log(JSON.stringify(results, null, '\t'))
			should.deepEqual(results, [
				[
					{
						"line": `Transfer_Rack("17","10",0,0,0,0,0,"","D-BSSE 96 Well DWP","Narrow","","","MP 3Pos Cooled 1 PCR","","MP 2Pos H+P Shake","2","(Not defined)","4");`,
						"effects": {
							"stillPlate.location": "ourlab.mario.site.P3",
							"EVOWARE.romaIndexPrev": 0
						},
						"tableEffects": [
							[ [ "MP 3Pos Cooled 1 PCR", 17, 2 ], { "label": "P4", "labwareModelName": "D-BSSE 96 Well DWP" } ],
							[ [ "MP 2Pos H+P Shake", 10, 4 ], { "label": "P3", "labwareModelName": "D-BSSE 96 Well DWP" } ]
						]
					}
				]
			]);
		});

		it("should compile pipetter._aspirate for a single aspiration", function() {
			const table = {};
			const protocol = _.merge({}, protocol0, {
				roboliq: "v1",
				objects: {
					plate1: {
						contents: {
							A01: ["10 ul", "water"]
						}
					}
				},
				steps: {
					"1": {
						command: "pipetter._aspirate",
						agent: "robot1",
						equipment: "pipetter1",
						program: "\"Water free dispense\"",
						items: [
							{
								syringe: "pipetter1.syringe.1",
								source: "plate1(A01)",
								volume: "10 ul"
							}
						]
					}
				}
			});
			const agents = ["robot1"];
			const results = EvowareCompiler.compileStep(table, protocol, agents, [], undefined, {timing: false});
			should.deepEqual(results, [[
				{line: "Aspirate(1,\"Water free dispense\",\"10\",0,0,0,0,0,0,0,0,0,0,0,1,0,1,\"0C0810000000000000\",0,0);"},
				{line: "MoveLiha(1,1,0,1,\"0C0810000000000000\",0,4,0,10,0,0);"},
				{"tableEffects": [
					[ [ "Some Carrier", 1, 1 ], { "label": "site1", "labwareModelName": "96-Well Plate" } ]
				]}
			]]);
		});

		it("should compile pipetter._dispense for a single dispense", function() {
			const table = {};
			const protocol = _.merge({}, protocol0, {
				roboliq: "v1",
				objects: {
					pipetter1: {
						syringe: {
							1: {
								contaminants: ["water"],
								contents: ["10 ul", "water"]
							}
						}
					}
				},
				steps: {
					"1": {
						command: "pipetter._dispense",
						agent: "robot1",
						equipment: "pipetter1",
						program: "\"Water free dispense\"",
						items: [
							{
								syringe: "pipetter1.syringe.1",
								destination: "plate1(A01)",
								volume: "10 ul"
							}
						]
					}
				}
			});
			const agents = ["robot1"];
			const results = EvowareCompiler.compileStep(table, protocol, agents, [], undefined, {timing: false});
			should.deepEqual(results, [[
				{line: "Dispense(1,\"Water free dispense\",\"10\",0,0,0,0,0,0,0,0,0,0,0,1,0,1,\"0C0810000000000000\",0,0);"},
				{line: "MoveLiha(1,1,0,1,\"0C0810000000000000\",0,4,0,10,0,0);"},
				{"tableEffects": [
					[ [ "Some Carrier", 1, 1 ], { "label": "site1", "labwareModelName": "96-Well Plate" } ]
				]}
			]]);
		});

		it("should compile pipetter._pipette for a single pipette", function() {
			const table = {};
			const protocol = _.merge({}, protocol0, {
				roboliq: "v1",
				objects: {
					plate1: {
						contents: {
							A01: ["10 ul", "water"]
						}
					}
				},
				steps: {
					"1": {
						command: "pipetter._pipette",
						agent: "robot1",
						equipment: "pipetter1",
						program: "\"Water free dispense\"",
						items: [
							{
								syringe: "pipetter1.syringe.1",
								source: "plate1(A01)",
								destination: "plate1(B01)",
								volume: "10 ul"
							}
						]
					}
				}
			});
			const agents = ["robot1"];
			const results = EvowareCompiler.compileStep(table, protocol, agents, [], undefined, {timing: false});
			should.deepEqual(results, [[
				{line: "Aspirate(1,\"Water free dispense\",\"10\",0,0,0,0,0,0,0,0,0,0,0,1,0,1,\"0C0810000000000000\",0,0);"},
				{line: "Dispense(1,\"Water free dispense\",\"10\",0,0,0,0,0,0,0,0,0,0,0,1,0,1,\"0C0820000000000000\",0,0);"},
				{line: "MoveLiha(1,1,0,1,\"0C0820000000000000\",0,4,0,10,0,0);"},
				{"tableEffects": [
					[ [ "Some Carrier", 1, 1 ], { "label": "site1", "labwareModelName": "96-Well Plate" } ]
				]}
			]]);
		});

		it("should compile pipetter._pipette for a two items, resulting in a single aspirate and multiple dispenses", function() {
			// console.log("schemas: "+JSON.stringify(schemas))
			const table = {};
			const protocol = _.merge({}, protocol0, {
				roboliq: "v1",
				objects: {
					plate1: {
						contents: {
							A01: ["10 ul", "water"]
						}
					}
				},
				steps: {
					"1": {
						command: "pipetter._pipette",
						agent: "robot1",
						equipment: "pipetter1",
						program: "\"Water free dispense\"",
						items: [
							{
								syringe: "pipetter1.syringe.1",
								source: "plate1(A01)",
								destination: "plate1(D01)",
								volume: "10 ul"
							},
							{
								syringe: "pipetter1.syringe.2",
								source: "plate1(B01)",
								destination: "plate1(F05)",
								volume: "10 ul"
							}
						]
					}
				}
			});
			const agents = ["robot1"];
			const results = EvowareCompiler.compileStep(table, protocol, agents, [], undefined, {timing: false});
			should.deepEqual(results, [[
				{line: "Aspirate(3,\"Water free dispense\",\"10\",\"10\",0,0,0,0,0,0,0,0,0,0,1,0,1,\"0C0830000000000000\",0,0);"},
				{line: "Dispense(1,\"Water free dispense\",\"10\",0,0,0,0,0,0,0,0,0,0,0,1,0,1,\"0C0880000000000000\",0,0);"},
				{line: "Dispense(2,\"Water free dispense\",0,\"10\",0,0,0,0,0,0,0,0,0,0,1,0,1,\"0C0800000400000000\",0,0);"},
				{line: "MoveLiha(2,1,0,1,\"0C0800000400000000\",0,4,0,10,0,0);"},
				{"tableEffects": [
					[ [ "Some Carrier", 1, 1 ], { "label": "site1", "labwareModelName": "96-Well Plate" } ]
				]}
			]]);
		});

		it("should compile pipetter._wash light", function() {
			const table = {};
			const protocol = _.merge({}, protocol0, {
				roboliq: "v1",
				objects: {
					plate1: {
						contents: {
							A01: ["10 ul", "water"]
						}
					}
				},
				steps: {
					"1": {
						"command": "pipetter._washTips",
						"agent": "robot1",
						"equipment": "pipetter1",
						"program": "robot1.washProgram.light_1000",
						"intensity": "light",
						"syringes": [
							"pipetter1.syringe.1",
							"pipetter1.syringe.2",
							"pipetter1.syringe.3",
							"pipetter1.syringe.4"
						]
					}
				}
			});
			const agents = ["robot1"];
			const results = EvowareCompiler.compileStep(table, protocol, agents, [], undefined, {timing: false});
			//console.log(JSON.stringify(results, null, '\t'))
			should.deepEqual(results, [[
				{line: "Wash(15,1,1,1,0,\"4\",500,\"2\",500,10,70,30,0,0,1000,0);"},
				{line: "MoveLiha(15,1,0,1,\"0108?0\",0,4,0,10,0,0);"}
			]]);
		});
	});
});
