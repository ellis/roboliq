import _ from 'lodash';
import should from 'should';
import jsonfile from 'jsonfile';
import * as EvowareCarrierFile from '../src/evoware/EvowareCarrierFile.js';
import * as EvowareCompiler from '../src/evoware/EvowareCompiler.js';
import * as EvowareTableFile from '../src/evoware/EvowareTableFile.js';

const protocol0 = {
	roboliq: "v1",
	objects: {
		robot1: {
			type: "Agent"
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
			evowareRoma: 1,
		},
		pipetter1: {
			type: "Pipetter",
			syringe: {
				1: {
					type: "Syringe",
					row: 1
				}
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
	schemas: require(__dirname+'/schemas.json')
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
			const results = EvowareCompiler.compileStep(table, protocol, agents, [], protocol.objects);
			should.deepEqual(results, [
				[{line: 'StartTimer("1");'}],
				[{line: 'WaitTimer("1","60");'}, {line: 'StopTimer("1");'}]
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
						"factsValue": "VALUE"
					}
				}
			});
			const agents = ["robot1"];
			const results = EvowareCompiler.compileStep(table, protocol, agents, [], protocol.objects);
			should.deepEqual(results, [
				[{line: "FACTS(\"RoboSeal\",\"RoboSeal_Seal\",\"VALUE\",\"0\",\"\");"}]
			]);
		});

		it("should compile transporter._movePlate", function() {
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
			const results = EvowareCompiler.compileStep(table, protocol, agents, [], protocol.objects);
			//console.log(JSON.stringify(results, null, '\t'))
			should.deepEqual(results, [
				[
					{
						"line": "Transfer_Rack(\"1\",\"1\",0,0,0,1,0,\"\",\"96-Well Plate\",\"undefined\",\"\",\"\",\"Some Carrier\",\"\",\"Some Carrier\",\"0\",(Not defined),\"1\");",
						"effects": {
							"plate1.location": "site2"
						},
						"tableEffects": [
							[ [ "Some Carrier", 1, 1 ], { "label": "site1", "labwareModelName": "96-Well Plate" } ],
							[ [ "Some Carrier", 1, 2 ], { "label": "site2", "labwareModelName": "96-Well Plate" } ]
						]
					}
				]
			]);
		});

		it("should compile pipetter._aspirate", function() {
			const table = {};
			const protocol = _.merge({}, protocol0, {
				roboliq: "v1",
				objects: {
					liquid1: {
						type: "Variable",
						value: "ourlab.mario.systemLiquid"
					},
					ourlab: {
						mario: {
							liha: {
								syringe: {
									1: {
										contaminants: undefined,
										contents: undefined,
										cleaned: "thorough"
									}
								}
							}
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
			const results = EvowareCompiler.compileStep(table, protocol, agents, [], protocol.objects);
			should.deepEqual(results, [[
				{line: "Aspirate(1,\"Water free dispense\",0,\"10\",0,0,0,0,0,0,0,0,0,0,1,0,1,\"0C0804000000000000\",0,0);"},
				{"tableEffects": [
					[ [ "Some Carrier", 1, 1 ], { "label": "site1", "labwareModelName": "96-Well Plate" } ]
				]}
			]]);
		});

		it("should compile pipetter._dispense", function() {
			const table = {};
			const protocol = _.merge({}, protocol0, {
				roboliq: "v1",
				objects: {
					liquid1: {
						type: "Variable",
						value: "ourlab.mario.systemLiquid"
					},
					ourlab: {
						mario: {
							liha: {
								syringe: {
									1: {
										contaminants: ["water"],
										contents: ["10 ul", "water"]
									}
								}
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
								source: "plate1(A01)",
								volume: "10 ul"
							}
						]
					}
				}
			});
			const agents = ["robot1"];
			const results = EvowareCompiler.compileStep(table, protocol, agents, [], protocol.objects);
			should.deepEqual(results, [[
				{line: "Dispense(1,\"Water free dispense\",0,\"10\",0,0,0,0,0,0,0,0,0,0,1,0,1,\"0C0804000000000000\",0,0);"},
				{"tableEffects": [
					[ [ "Some Carrier", 1, 1 ], { "label": "site1", "labwareModelName": "96-Well Plate" } ]
				]}
			]]);
		});
	});
});
