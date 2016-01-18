import _ from 'lodash';
import yaml from 'yamljs';
import * as EvowareCarrierFile from './EvowareCarrierFile.js';
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

			}
		}
	}
}

run();
