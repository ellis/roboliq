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
			const results = EvowareCompiler.compileStep(table, protocol, agents, [], undefined, [], {timing: false});
			should.deepEqual(results, [
				[{line: 'StartTimer("1");'}],
				[{line: 'WaitTimer("1","60");'}]
			]);
		});

		it("should compile evoware._execute", function() {
			const table = {};
			const protocol = _.merge({}, protocol0, {
				roboliq: "v1",
				steps: {
					1: {
						"command": "evoware._execute",
						"agent": "robot1",
						"path": "wscript",
						"args": ["some.vbs"],
						"wait": false
					}
				}
			});
			const agents = ["robot1"];
			const results = EvowareCompiler.compileStep(table, protocol, agents, [], undefined, [], {timing: false});
			should.deepEqual(results, [
				[{ line: "Execute(\"wscript some.vbs\",0,\"\",2);" }]
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
			const results = EvowareCompiler.compileStep(table, protocol, agents, [], undefined, [], {timing: false});
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
			const results = EvowareCompiler.compileStep(table, protocol, agents, [], undefined, [], {timing: false});
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
			const results = EvowareCompiler.compileStep(table, protocol, agents, [], undefined, [], {timing: false});
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
			const results = EvowareCompiler.compileStep(table, protocol, agents, [], undefined, [], {timing: false});
			should.deepEqual(results, [[
				{line: "Aspirate(1,\"Water free dispense\",\"10\",0,0,0,0,0,0,0,0,0,0,0,1,0,1,\"0C0810000000000000\",0,0);"},
				{line: "MoveLiha(1,1,0,1,\"0C0810000000000000\",4,4,0,400,0,0);"},
				{"tableEffects": [
					[ [ "Some Carrier", 1, 1 ], { "label": "site1", "labwareModelName": "96-Well Plate" } ]
				]}
			]]);
		});

		it("should compile pipetter._aspirate using syringe number instead of name", function() {
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
								syringe: 2,
								source: "plate1(A01)",
								volume: "10 ul"
							}
						]
					}
				}
			});
			const agents = ["robot1"];
			const results = EvowareCompiler.compileStep(table, protocol, agents, [], undefined, [], {timing: false});
			should.deepEqual(results, [[
				{line: "Aspirate(2,\"Water free dispense\",0,\"10\",0,0,0,0,0,0,0,0,0,0,1,0,1,\"0C0810000000000000\",0,0);"},
				{line: "MoveLiha(2,1,0,1,\"0C0820000000000000\",4,4,0,400,0,0);"},
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
			const results = EvowareCompiler.compileStep(table, protocol, agents, [], undefined, [], {timing: false});
			should.deepEqual(results, [[
				{line: "Dispense(1,\"Water free dispense\",\"10\",0,0,0,0,0,0,0,0,0,0,0,1,0,1,\"0C0810000000000000\",0,0);"},
				{line: "MoveLiha(1,1,0,1,\"0C0810000000000000\",4,4,0,400,0,0);"},
				{"tableEffects": [
					[ [ "Some Carrier", 1, 1 ], { "label": "site1", "labwareModelName": "96-Well Plate" } ]
				]}
			]]);
		});

		it("should compile pipetter._mix", function() {
			const table = {};
			const protocol = _.merge({}, protocol0, {
				roboliq: "v1",
				objects: {
					plate1: {
						contents: {
							A01: ["100 ul", "water"],
							B01: ["100 ul", "water"],
							C01: ["100 ul", "water"],
							D01: ["100 ul", "water"],
							// A02: ["100 ul", "water"],
							// B02: ["100 ul", "water"],
							// C02: ["100 ul", "water"],
							// D02: ["100 ul", "water"],
						}
					}
				},
				steps: {
					"1": {
						command: "pipetter._mix",
						agent: "robot1",
						equipment: "pipetter1",
						program: "\"Water free dispense\"",
						itemDefaults: {
							volume: "50 ul",
							count: 2
						},
						items: [
							{ syringe: "pipetter1.syringe.1", well: "plate1(A01)" },
							{ syringe: "pipetter1.syringe.2", well: "plate1(B01)" },
							{ syringe: "pipetter1.syringe.3", well: "plate1(C01)" },
							{ syringe: "pipetter1.syringe.4", well: "plate1(D01)" },
						]
					}
				}
			});
			const agents = ["robot1"];
			const results = EvowareCompiler.compileStep(table, protocol, agents, [], undefined, [], {timing: false});
			should.deepEqual(results, [[
				{line: "Mix(15,\"Water free dispense\",\"50\",\"50\",\"50\",\"50\",0,0,0,0,0,0,0,0,1,0,1,\"0C08?0000000000000\",2,0,0);"},
				{line: "MoveLiha(15,1,0,1,\"0C08?0000000000000\",4,4,0,400,0,0);"},
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
			const results = EvowareCompiler.compileStep(table, protocol, agents, [], undefined, [], {timing: false});
			should.deepEqual(results, [[
				{line: "Aspirate(1,\"Water free dispense\",\"10\",0,0,0,0,0,0,0,0,0,0,0,1,0,1,\"0C0810000000000000\",0,0);"},
				{line: "Dispense(1,\"Water free dispense\",\"10\",0,0,0,0,0,0,0,0,0,0,0,1,0,1,\"0C0820000000000000\",0,0);"},
				{line: "MoveLiha(1,1,0,1,\"0C0810000000000000\",4,4,0,400,0,0);"},
				{"tableEffects": [
					[ [ "Some Carrier", 1, 1 ], { "label": "site1", "labwareModelName": "96-Well Plate" } ]
				]}
			]]);
		});

		it("should compile pipetter._pipette for two items, resulting in a single aspirate and multiple dispenses", function() {
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
			const results = EvowareCompiler.compileStep(table, protocol, agents, [], undefined, [], {timing: false});
			should.deepEqual(results, [[
				{line: "Aspirate(3,\"Water free dispense\",\"10\",\"10\",0,0,0,0,0,0,0,0,0,0,1,0,1,\"0C0830000000000000\",0,0);"},
				{line: "Dispense(1,\"Water free dispense\",\"10\",0,0,0,0,0,0,0,0,0,0,0,1,0,1,\"0C0880000000000000\",0,0);"},
				{line: "Dispense(2,\"Water free dispense\",0,\"10\",0,0,0,0,0,0,0,0,0,0,1,0,1,\"0C0800000400000000\",0,0);"},
				{line: "MoveLiha(3,1,0,1,\"0C0830000000000000\",4,4,0,400,0,0);"},
				{"tableEffects": [
					[ [ "Some Carrier", 1, 1 ], { "label": "site1", "labwareModelName": "96-Well Plate" } ]
				]}
			]]);
		});

		it("should compile pipetter._pipette for two items with mixing", function() {
			// console.log("schemas: "+JSON.stringify(schemas))
			const table = {};
			const protocol1 = _.merge({}, protocol0, {
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
								volume: "10 ul",
								sourceMixing: {count: 3, volume: "7 ul"},
								destinationMixing: {count: 3, volume: "7 ul"}
							},
							{
								syringe: "pipetter1.syringe.2",
								source: "plate1(B01)",
								destination: "plate1(F05)",
								volume: "10 ul",
								sourceMixing: {count: 3, volume: "7 ul"},
								destinationMixing: {count: 3, volume: "7 ul"}
							}
						]
					}
				}
			});
			const expected = [[
				{line: "Mix(3,\"Water free dispense\",\"7\",\"7\",0,0,0,0,0,0,0,0,0,0,1,0,1,\"0C0830000000000000\",3,0,0);"},
				{line: "Aspirate(3,\"Water free dispense\",\"10\",\"10\",0,0,0,0,0,0,0,0,0,0,1,0,1,\"0C0830000000000000\",0,0);"},
				{line: "Dispense(1,\"Water free dispense\",\"10\",0,0,0,0,0,0,0,0,0,0,0,1,0,1,\"0C0880000000000000\",0,0);"},
				{line: "Mix(1,\"Water free dispense\",\"7\",0,0,0,0,0,0,0,0,0,0,0,1,0,1,\"0C0880000000000000\",3,0,0);"},
				{line: "Dispense(2,\"Water free dispense\",0,\"10\",0,0,0,0,0,0,0,0,0,0,1,0,1,\"0C0800000400000000\",0,0);"},
				{line: "Mix(2,\"Water free dispense\",0,\"7\",0,0,0,0,0,0,0,0,0,0,1,0,1,\"0C0800000400000000\",3,0,0);"},
				{line: "MoveLiha(3,1,0,1,\"0C0830000000000000\",4,4,0,400,0,0);"},
				{"tableEffects": [
					[ [ "Some Carrier", 1, 1 ], { "label": "site1", "labwareModelName": "96-Well Plate" } ]
				]}
			]];
			const agents = ["robot1"];
			const results1 = EvowareCompiler.compileStep(table, protocol1, agents, [], undefined, [], {timing: false});
			should.deepEqual(results1, expected);

			const protocol2 = _.merge({}, protocol0, {
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
						sourceMixing: {count: 3, volume: "7 ul"},
						destinationMixing: {count: 3, volume: "7 ul"},
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
			const results2 = EvowareCompiler.compileStep(table, protocol2, agents, [], undefined, [], {timing: false});
			should.deepEqual(results2, expected);

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
			const results = EvowareCompiler.compileStep(table, protocol, agents, [], undefined, [], {timing: false});
			//console.log(JSON.stringify(results, null, '\t'))
			should.deepEqual(results, [[
				{line: "Wash(15,1,1,1,0,\"4\",500,\"2\",500,10,70,30,0,0,1000,0);"},
				{line: "MoveLiha(15,1,0,1,\"0108?0\",4,4,0,400,0,0);"}
			]]);
		});

		it("should handle timing", function() {
			const table = {};
			const protocol = _.merge({}, protocol0, {
				roboliq: "v1",
				steps: {
					1: {
						"command": "evoware._execute",
						"agent": "robot1",
						"path": "wscript",
						"args": ["some.vbs"],
						"wait": false
					}
				}
			});
			const agents = ["robot1"];
			const options = {timing: true, variables: {ROBOLIQ: "AAA", SCRIPTFILE: "C:\\Here\\myscript.out.json"}};
			const results = EvowareCompiler.compileStep(table, protocol, agents, [], undefined, [], options);
			// console.log(JSON.stringify(results, null, '\t'));
			should.deepEqual(results, [[
				{ "line": "Group(\"Step 1\");" },
				{ "line": "Execute(\"AAA begin --step 1 --script C:\\Here\\myscript.out.json\",0,\"\",2);" },
				{ "line": "Execute(\"wscript some.vbs\",0,\"\",2);" },
				{ "line": "Execute(\"AAA end --step 1 --script C:\\Here\\myscript.out.json\",0,\"\",2);" },
				{ "line": "GroupEnd();" }
			]]);
		});

		it("should handle system.repeat and system.runtimeExitLoop", () => {
			const table = {};
			const protocol = _.merge({}, protocol0, {
				roboliq: "v1",
				steps: {
					1: {
						"command": "system.repeat",
						1: {
							command: "system.runtimeExitLoop",
							testType: "R",
							test: "cat('true')"
						}
					}
				}
			});
			const agents = ["robot1"];
			const options = {timing: false, variables: {ROBOLIQ: "AAA", SCRIPTFILE: "C:\\Here\\myscript.out.json"}};
			const results = EvowareCompiler.compileStep(table, protocol, agents, [], undefined, [], options);
			// console.log(JSON.stringify(results, null, '\t'));
			should.deepEqual(results, [[
				[
					{ "line": "Execute(\"AAA execTest C:\\Here\\myscript.out.json 1.1\",6,\"EXITLOOP\",2);" },
					{ "line": "If(\"EXITLOOP\",0,\"0\",\"_1End\");" }
				],
				{ "line": "Comment(\"_1End\");" }
			]]);
		});

		it("should handle system.runtimeLoadVariables and ", () => {
			const table = {};
			const protocol = _.merge({}, protocol0, {
				roboliq: "v1",
				steps: {
					1: {
						"command": "system.runtimeLoadVariables",
						"varset": "varset1"
					}
				}
			});
			const agents = ["robot1"];
			const options = {timing: false, variables: {ROBOLIQ: "AAA", SCRIPTFILE: "C:\\Here\\myscript.out.json"}};
			const results = EvowareCompiler.compileStep(table, protocol, agents, [], undefined, [], options);
			// console.log(JSON.stringify(results, null, '\t'));
			should.deepEqual(results, [[
				[
					{ "line": "Execute(\"AAA execTest C:\\Here\\myscript.out.json 1.1\",6,\"EXITLOOP\",2);" },
					{ "line": "If(\"EXITLOOP\",0,\"0\",\"_1End\");" }
				],
				{ "line": "Comment(\"_1End\");" }
			]]);
		});

	});

	describe("compile", () => {
		it("should call `initRun` script", () => {
			const table = {};
			const protocol = _.merge({}, protocol0, {
				roboliq: "v1",
				steps: {
					1: {
						command: "timer._start",
						agent: "robot1",
						equipment: "timer1"
					}
				}
			});
			const agents = ["robot1"];
			const options = {timing: true, variables: {ROBOLIQ: "AAA", SCRIPTFILE: "C:\\Here\\myscript.out.json", TEMPDIR: "C:\\Temp"}};
			const [{lines}] = EvowareCompiler.compile(table, protocol, agents, options);
			// console.log({lines});
			should(lines[0]).equal('Execute("AAA initRun C:\\Here\\myscript.out.json",2,"",2);');
		});

		it("should automatically insert token to create TEMPDIR, if used", () => {
			const table = {};
			const protocol = _.merge({}, protocol0, {
				roboliq: "v1",
				steps: {
					1: {
						"command": "evoware._execute",
						"agent": "robot1",
						"path": "wscript",
						"args": ["${TEMPDIR}"],
						"wait": false
					}
				}
			});
			const agents = ["robot1"];
			const options = {timing: true, variables: {ROBOLIQ: "AAA", SCRIPTFILE: "C:\\Here\\myscript.out.json", TEMPDIR: "C:\\Temp"}};
			const [{lines}] = EvowareCompiler.compile(table, protocol, agents, options);
			// console.log({lines});
			should(lines).containEql('Execute("cmd /c mkdir C:\\Temp",2,"",2);');
		});

	});
});
