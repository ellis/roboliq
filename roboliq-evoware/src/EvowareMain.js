/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017, ETH Zurich, Ellis Whitehead
 * @license GPL-3.0
 */

const _ = require('lodash');
const fs = require('fs');
const iconv = require('iconv-lite');
const jsonfile = require('jsonfile');
const path = require('path');
const yaml = require('yamljs');
const EvowareCarrierFile = require('./EvowareCarrierFile.js');
const EvowareCompiler = require('./EvowareCompiler.js');
const EvowareTableFile = require('./EvowareTableFile.js');

const version = "v1";

const commander = require('commander')
	.version("1.0")
	.option("-d, --debug", "enable debugging output")
	.option("-o, --output", "full path (directory and filename) to save the script to")
	.option("-O, --outputDir", "directory to save the script to (defaults to the same directory as the input protocol)")
	.option("-b, --outputBasename", "filename for the script (without directory) (defaults to basename of the input protocol)")
	.option("--SCRIPTDIR [dir]", "value of SCRIPTDIR variable (default to directory where script is saved)")
	.option("--progress", "display progress while compiling the script")
	.arguments("[carrier] [table] [protocol] [agents]")
	.description(
		"Arguments:\n"+
		"    carrier   path to Carrier.cfg\n"+
		"    table     path to table file (.ewt or .esc)\n"+
		"    protocol  path to compiled protocol (.out.json)\n"+
		"    agents    list of agents to compile for (comma-separated)\n"
	);

function runWithArgv(argv) {
	var opts = commander.parse(argv);
	if (opts.args.length === 0 || opts.rawArgs.indexOf("--help") >= 0 || opts.rawArgs.indexOf("-h") >= 0) {
		opts.outputHelp();
		return;
	}
	run(opts);
}

function run(opts) {
	if (opts.debug) {
		console.log(opts);
	}

	opts.carrier = _.get(opts.args, 0);
	opts.table = _.get(opts.args, 1);
	opts.protocol = _.get(opts.args, 2);
	opts.agents = _.get(opts.args, 3);

	if (_.isEmpty(opts.carrier)) {
		commander.outputHelp();
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
				const scriptDir = path.win32.normalize(opts.SCRIPTDIR || path.resolve(path.dirname(opts.protocol)));
				const scriptFile = path.win32.join(scriptDir, path.basename(opts.protocol));
				const agentConfig = _.get(protocol.objects, agents[0].split(".").concat(["config"]), {});
				const options = {
					variables: {
						BROWSER: agentConfig.BROWSER,
						ROBOLIQ: agentConfig.ROBOLIQ,
						SCRIPTFILE: scriptFile,
						SCRIPTDIR: scriptDir,
						TEMPDIR: agentConfig.TEMPDIR,
					},
				};

				const results = EvowareCompiler.compile(table, protocol, agents, options);
				//console.log()
				//console.log(JSON.stringify(results, null, '\t'))
				//console.log()
				_.forEach(results, result => {
					const tableLines = EvowareTableFile.toStrings(carrierData, result.table);
					const output = tableLines.concat(result.lines).join("\r\n") + "\r\n";
					const inpath = opts.protocol;
					const dir = path.dirname(inpath);
					const outpath = path.join(dir, path.basename(inpath, ".out.json")+".esc");
					const encoded = iconv.encode(output, "ISO-8859-1");
					fs.writeFileSync(outpath, encoded);
					console.log("output written to "+outpath);

					// Save any additional files generated
					_.flattenDeep(result.tokenTree).forEach(token => {
						if (token.file) {
							const filepath = path.join(dir, token.file);
							fs.writeFileSync(filepath, token.data);
							console.log("output written to "+filepath);
						}
					});
				});
				//console.log(JSON.stringify(results, null, '\t'))
			}
		}
	}
}

// If called from the command line:
if (require.main === module) {
	run(process.argv);
}

module.exports = {
	runWithArgv,
	run
};
