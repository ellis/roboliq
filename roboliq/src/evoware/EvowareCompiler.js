import _ from 'lodash';
import naturalSort from 'javascript-natural-sort';
import M from '../Medley.js';
import commandHelper from '../commandHelper.js';
import * as EvowareTableFile from './EvowareTableFile.js';
import * as evoware from './commands/evoware.js';
import * as pipetter from './commands/pipetter.js';
import * as timer from './commands/timer.js';
import * as transporter from './commands/transporter.js';

const commandHandlers = {
	"evoware._facts": evoware._facts,
	"evoware._raw": evoware._raw,
	"pipetter._aspirate": pipetter._aspirate,
	"pipetter._dispense": pipetter._dispense,
	"pipetter._mix": pipetter._mix,
	"pipetter._pipette": pipetter._pipette,
	"pipetter._washTips": pipetter._washTips,
	"timer._sleep": timer._sleep,
	"timer._start": timer._start,
	"timer._wait": timer._wait,
	"transporter._movePlate": transporter._movePlate
};

/**
 * Compile a protocol for a given evoware setup
 * @param  {EvowareCarrierData} carrierData
 * @param  {object} table - table object (see EvowareTableFile.load)
 * @param  {roboliq:Protocol} protocol
 * @param  {array} agents - string array of agent names that this script should generate script(s) for
 * @param  {object} options - an optional map of options; set timing=false to avoid outputting time-logging instructions
 * @return {array} an array of {table, lines} items; one item is generated per required table layout.  lines is an array of strings.
 */
export function compile(carrierData, table, protocol, agents, options = {}) {
	options = _.defaults(options, _.get(protocol.config, "evowareCompiler", {}));
	table = _.cloneDeep(table);
	const objects = _.cloneDeep(protocol.objects);
	const results = compileStep(table, protocol, agents, [], objects, options);
	results.push(transporter.moveLastRomaHome({objects}));
	const flat = _.flattenDeep(results);
	//flat.forEach(x => console.log(x));
	const lines = _(flat).map(x => x.line).compact().value();
	return [{table, lines}];
}

export function compileStep(table, protocol, agents, path, objects, options = {}) {
	if (_.isUndefined(objects)) {
		objects = _.cloneDeep(protocol.objects);
	}

	// console.log(`compileStep: ${path.join(".")}`)
	// console.log({steps: protocol.steps})
	const step = (_.isEmpty(path)) ? protocol.steps : _.get(protocol.steps, path);
	if (_.isUndefined(step))
		return undefined;

	const results = [];

	const commandHandler
		= (_.isUndefined(step.command) || _.isUndefined(step.agent) || !_.includes(agents, step.agent))
		? undefined
		: commandHandlers[step.command];
	let generatedCommandLines = false;
	let generatedTimingLogs = false;
	// If there is no command handler for this step, then handle sub-steps
	if (_.isUndefined(commandHandler)) {
		// Find all sub-steps (properties that start with a digit)
		var keys = _.filter(_.keys(step), function(key) {
			var c = key[0];
			return (c >= '0' && c <= '9');
		});
		// Sort them in "natural" order
		keys.sort(naturalSort);
		//console.log({keys})
		// Try to expand the substeps
		for (const key of keys) {
			const result1 = compileStep(table, protocol, agents, path.concat(key), objects, options);
			results.push(result1);
		}
	}
	// Else, handle the step's command:
	else {
		const data = {
			objects,
			//predicates,
			//planHandlers: protocol.planHandlers,
			schemas: protocol.schemas,
			accesses: [],
			//files: filecache,
			protocol,
			path
		};
		// Parse command options
		const schema = protocol.schemas[step.command];
		const parsed = (schema)
			? commandHelper.parseParams(step, data, schema)
			: {orig: step};
		// Handle the command
		const result0 = commandHandler(step, parsed, data);
		// For all returned results:
		_.forEach(_.compact(result0), result1 => {
			// console.log("result1: "+JSON.stringify(result1));
			results.push(result1);
			if (result1.effects) {
				_.forEach(result1.effects, (effect, path2) => {
					M.setMut(objects, path2, effect);
				});
			}
			if (!_.isEmpty(result1.tableEffects)) {
				//console.log("tableEffects: "+JSON.stringify(result1.tableEffects))
				_.forEach(result1.tableEffects, ([path2, value]) => {
					//console.log({path2, value})
					// TODO: Need to check whether a table change is required because a different labware model is now used at a given site
					M.setMut(table, path2, value);
				});
			}
		});

		// Check whether command produced any output lines
		generatedCommandLines = _.find(_.flattenDeep(results), x => _.has(x, "line"));

		// Possibly wrap the instructions in calls to pathToRoboliqRuntimeCli in order to check timing
		// console.log({options, timing: _.get(options, "timing", true)})
		if (generatedCommandLines && _.get(options, "timing", true) === true) {
			const agent = _.get(objects, step.agent);
			// console.log({agent})
			if (_.has(agent, ["config", "pathToRoboliqRuntimeCli"])) {
				const pathToRoboliqRuntimeCli = agent.config.pathToRoboliqRuntimeCli;
				// TODO: set 2 => 0 after the command line in order not to wait till execution is complete
				// This will wait: `Execute("wscript ${pathToRoboliqRuntimeCli} begin ${path.join(".")}",2,"",2);`
				// This wont wait: `Execute("wscript ${pathToRoboliqRuntimeCli} begin ${path.join(".")}",0,"",2);`
				results.unshift({line: `Execute("wscript ${pathToRoboliqRuntimeCli} --begin ${path.join(".")}",0,"",2);`})
				results.push({line: `Execute("wscript ${pathToRoboliqRuntimeCli} --end ${path.join(".")}",0,"",2);`})
				generatedTimingLogs = true;
			}
		}

	}

	// Process the protocol's effects
	const effects = _.get(protocol, ["effects", path.join(".")]);
	if (!_.isUndefined(effects)) {
		_.forEach(effects, (effect, path2) => {
			M.setMut(objects, path2, effect);
		});
	}

	const generatedComment = !_.isEmpty(step.description);
	if (generatedComment) {
		const hasInstruction = _.find(_.flattenDeep(results), x => _.has(x, "line"));
		const text = `${path.join(".")}) ${step.description}`;
		results.unshift({line: `Comment("${text}");`});
	}

	// Possibly wrap the instructions in a group
	const generatedGroup =
		(generatedTimingLogs) ||
		(!generatedCommandLines && generatedComment && results.length > 1);
	if (generatedGroup) {
		const text = `Step ${path.join(".")}`;
		results.unshift({line: `Group("${text}");`});
		results.push({line: `GroupEnd();`});
	}

	return results;
}

function headerLines(table, protocol, agents, path, objects, options = {}) {
	return [
		'Variable(RUN,"1",0,"An identifier for this run of the protocol",0,1.000000,10.000000,1,2,0,0);',
		'Variable(BASEDIR,"C:\ProgramData\Tecan\EVOware\database\scripts\Ellis",0,"Directory containing files required for this script",0,1.000000,10.000000,1,2,0,0);',
		'Variable(RUNDIR,"~BASEDIR~/run-~RUN~",0,"Directory where log and measurement files should be stored for this run",0,1.000000,10.000000,1,2,0,0);'
	];
}
