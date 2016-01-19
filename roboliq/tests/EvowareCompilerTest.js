import _ from 'lodash';
import should from 'should';
import jsonfile from 'jsonfile';
import * as EvowareCarrierFile from '../src/evoware/EvowareCarrierFile.js';
import * as EvowareCompiler from '../src/evoware/EvowareCompiler.js';
import * as EvowareTableFile from '../src/evoware/EvowareTableFile.js';

describe('EvowareTableFile', function() {
	describe('load', function () {
		it('should load table from NewLayout_Feb2015.ewt without raising an exception', function () {
			const carrierData = EvowareCarrierFile.load("../testdata/bsse-mario/Carrier.cfg");
			//carrierData.printCarriersById();
			const table = EvowareTableFile.load(carrierData, "../testdata/bsse-mario/NewLayout_Feb2015.ewt");
			//console.log(JSON.stringify(table, null, '\t'));
			const protocol = jsonfile.readFileSync("protocols/output/protocol3.cmp.json");

			const result = EvowareCompiler.compile(carrierData, table, protocol, ["ourlab.mario.evoware"]);
			//console.log({result})
			should.equal(result.length, 1);
			should.deepEqual(result[0].lines, [
				'Transfer_Rack("10","35",0,0,0,1,0,"","Ellis Nunc F96 MicroWell","Narrow","","","MP 2Pos H+P Shake","","RoboSeal","1",(Not defined),"0");',
				'Transfer_Rack("35","10",0,0,0,1,0,"","Ellis Nunc F96 MicroWell","Narrow","","","RoboSeal","","MP 2Pos H+P Shake","0",(Not defined),"1");'
			]);
			//console.log(EvowareTableFile.toStrings(carrierData, result[0].table).join("\n"))
			//console.log(JSON.stringify(result[0].table, null, '\t'))
			const table0 = result[0].table;
			should.deepEqual(_.get(result[0].table, ["MP 2Pos H+P Shake", 10, 2]), {"label": "P2", "labwareModelName": "Ellis Nunc F96 MicroWell"});
			should.deepEqual(_.get(result[0].table, ["RoboSeal", 35, 1]), {"label": "ROBOSEAL", "labwareModelName": "Ellis Nunc F96 MicroWell"});
		});
	});
});
