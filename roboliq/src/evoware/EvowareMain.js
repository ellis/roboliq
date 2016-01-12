import _ from 'lodash';
import * as EvowareCarrierFile from './EvowareCarrierFile.js';

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
		const data = EvowareCarrierFile.loadEvowareCarrierData(opts.carrier);
		if (_.isEmpty(opts.table)) {
			data.printCarriersById();
		}
		else {
			const table = EvowareTableFile.load(opts.table);
			if (_.isEmpty(opts.protocol)) {
				table.print();
			}
			else {
				
			}
		}
	}
}

run();
