import _ from 'lodash';
import fs from 'fs';
import iconv from 'iconv-lite';
import jsonfile from 'jsonfile';
import path from 'path';
import yaml from 'yamljs';
import * as EvowareCarrierFile from './EvowareCarrierFile.js';
import * as EvowareCompiler from './EvowareCompiler.js';
import * as EvowareTableFile from './EvowareTableFile.js';

const version = "v1";

const nomnom = require('nomnom').options({
	carrier: {
		position: 0,
		help: 'path to Carrier.cfg',
	},
	table: {
		position: 1,
		help: "path to table file (.ewt or .esc)"
	},
	protocol: {
		position: 2,
		help: "path to protocol (.json)"
	},
	agents: {
		position: 3,
		help: "list of agents to compile for (comma-separated)"
	},
	version: {
		flag: true,
		help: 'print version and exit',
		callback: function() {
			return "version "+version;
		}
	},
});

export function run(argv) {
	var opts = nomnom.parse(argv);
	if (opts.debug) {
		console.log(opts);
	}

	if (_.isEmpty(opts.carrier)) {
		console.log(nomnom.getUsage());
		process.exit(0);
	}
	else {
		const carrierData = EvowareCarrierFile.load(opts.carrier);
		if (_.isEmpty(opts.table)) {
			carrierData.printCarriersById();
		}
		else {
			const table = EvowareTableFile.load(carrierData, opts.table);
			if (_.isEmpty(opts.protocol)) {
				console.log(yaml.dump(table));
			}
			else {
				const protocol = jsonfile.readFileSync(opts.protocol);
				const agents = opts.agents.split(",");
				const results = EvowareCompiler.compile(carrierData, table, protocol, agents);
				//console.log()
				//console.log(JSON.stringify(results, null, '\t'))
				//console.log()
				_.forEach(results, result => {
					const tableLines = EvowareTableFile.toStrings(carrierData, result.table);
					const output = tableLines.concat(result.lines).join("\r\n") + "\r\n";
					const inpath = opts.protocol;
					const dir = path.dirname(inpath);
					const outpath = path.join(dir, path.basename(inpath, path.extname(inpath))+".esc");
					const encoded = iconv.encode(output, "ISO-8859-1");
					fs.writeFileSync(outpath, encoded);
					console.log("output written to "+outpath);
				});
				//console.log(JSON.stringify(results, null, '\t'))
			}
		}
	}
}

run();
