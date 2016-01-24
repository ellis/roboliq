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
		}
	},
	predicates: [
		{"timer.canAgentEquipment": {agent: "robot1", equipment: "timer1"}},
		{"timer.canAgentEquipment": {agent: "robot1", equipment: "timer2"}},
	]
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
						till: 1
					}
				}
			});
			const agents = ["robot1"];
			const results = EvowareCompiler.compileStep(table, protocol, agents, [], protocol.objects);
			should.deepEqual(results, [
				[{line: 'StartTimer("1");'}],
				[{line: 'WaitTimer("1","1");'}]
			]);
		});
	});
});
