import _ from 'lodash';
import naturalSort from 'javascript-natural-sort';
import M from '../Medley.js';
import commandHelper from '../commandHelper.js';
import * as EvowareTableFile from './EvowareTableFile.js';
import evowareHelper from './commands/evowareHelper.js';
import * as evoware from './commands/evoware.js';
import * as pipetter from './commands/pipetter.js';
import * as system from './commands/system.js';
import * as timer from './commands/timer.js';
import * as transporter from './commands/transporter.js';

const commandHandlers = {
	"evoware._execute": evoware._execute,
	"evoware._facts": evoware._facts,
	"evoware._raw": evoware._raw,
	"pipetter._aspirate": pipetter._aspirate,
	"pipetter._dispense": pipetter._dispense,
	"pipetter._mix": pipetter._mix,
	"pipetter._pipette": pipetter._pipette,
	"pipetter._washTips": pipetter._washTips,
	"system.runtimeExitLoop": system.runtimeExitLoop,
	"timer._sleep": timer._sleep,
	"timer._start": timer._start,
	"timer._wait": timer._wait,
	"transporter._movePlate": transporter._movePlate
};

/**
 * Compile a protocol for a given evoware setup.
 *
 * @param  {EvowareCarrierData} carrierData
 * @param  {object} table - table object (see EvowareTableFile.load)
 * @param  {roboliq:Protocol} protocol
 * @param  {array} agents - string array of agent names that this script should generate script(s) for
 * @param  {object} options - an optional map of options; set timing=false to avoid outputting time-logging instructions
 * @return {array} an array of {table, lines} items; one item is generated per required table layout.  lines is an array of strings.
 */
export function compile(table, protocol, agents, options = {}) {
	options = _.defaults(options, _.get(protocol.config, "evowareCompiler", {}));
	table = _.cloneDeep(table);
	const objects = _.cloneDeep(protocol.objects);
	const results = compileStep(table, protocol, agents, [], objects, [], options);

	// Prepend token to call 'initRun'
	results.unshift({line: evowareHelper.createExecuteLine(options.variables.ROBOLIQ, ["initRun", options.variables.SCRIPTFILE], true)});
	// Append token to reset the last moved ROMA
	results.push(transporter.moveLastRomaHome({objects}));

	const lines = _(results).flattenDeep().map(x => x.line).compact().value();
	// Prepend token to create the TEMPDIR
	if (_.some(lines, line => line.indexOf(options.variables.TEMPDIR) >= 0)) {
		lines.unshift(evowareHelper.createExecuteLine("cmd", ["/c", "mkdir", options.variables.TEMPDIR], true));
	}

	return [{table, lines}];
}

export function compileStep(table, protocol, agents, path, objects, loopEndStack = [], options = {}) {
	try {
		const results = compileStepSub(table, protocol, agents, path, objects, loopEndStack, options);
		return results;
	} catch (e) {
		console.log("ERROR: "+path.join("."));
		console.log(JSON.stringify(_.get(protocol.steps, path)))
		console.log(e)
		console.log(e.stack)
	}
	return [];
}

function compileStepSub(table, protocol, agents, path, objects, loopEndStack, options) {
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

		const isLoop = _.includes(["system.repeat", "experiment.forEachGroup", "experiment.forEachRow"], step.command);
		const loopEndName = `_${path.join(".")}End`;
		const loopEndStack2 = (isLoop) ? [loopEndName].concat(loopEndStack) : loopEndStack;
		let needLoopLabel = false;

		// Try to expand the substeps
		for (const key of keys) {
			const result1 = compileStep(table, protocol, agents, path.concat(key), objects, loopEndStack2, options);
			// Possibly check whether we need a loop label
			if (isLoop && !needLoopLabel) {
				const result2 = _.flattenDeep(result1);
				needLoopLabel = _.some(result2, result => (result.line || "").indexOf(loopEndName) >= 0);
			}
			results.push(result1);
		}

		if (needLoopLabel) {
			results.push({line: `Comment("${loopEndName}");`});
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
			path,
			loopEndStack
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
			const exePath = "${ROBOLIQ}";//options.variables.ROBOLIQ;
			// console.log({agent})
			results.unshift({line: evowareHelper.createExecuteLine(exePath, ["begin", "--step", path.join("."), "--script", "${SCRIPTFILE}"], false)});
			results.push({line: evowareHelper.createExecuteLine(exePath, ["end", "--step", path.join("."), "--script", "${SCRIPTFILE}"], false)});
			generatedTimingLogs = true;
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

	substitutePathVariables(results, options);

	return results;
}

function substitutePathVariables(results, options) {
	// console.log({results, options})
	if (!options.variables)
		return;

	for (let i = 0; i < results.length; i++) {
		const result = results[i];
		if (_.isArray(result)) {
			substitutePathVariables(result, options);
		}
		else {
			let line = results[i].line;
			if (line) {
				line = line.replace("${ROBOLIQ}", options.variables.ROBOLIQ);
				line = line.replace("${SCRIPTFILE}", options.variables.SCRIPTFILE);
				line = line.replace("${TEMPDIR}", options.variables.TEMPDIR);
				results[i].line = line;
			}
		}
	}
}

/*
export function headerLines(protocol, options, linesOfSteps) {
	// console.log({options, linesOfSteps})
	// Check which variables were used in a line
	const checkList = ["RUN", "ROBOLIQ", "SCRIPTDIR", "RUNDIR"];
	const use = {};
	let useChanged = false;
	function check(line) {
		for (let i = 0; i < checkList.length; i++) {
			const name = checkList[i];
			// console.log({name, use: use[name], line, index: line.indexOf("~"+name+"~")})
			if (!use[name] && line.indexOf("~"+name+"~") >= 0) {
				use[name] = true;
				useChanged = true;
			}
		}
	}

	// Create variable line
	function getString(name, value, description) {
		return `Variable(${name},"${value}",0,"${description}",0,1.000000,10.000000,1,2,0,0);`;
	}

	// Check which variables were used in the script
	_.forEach(linesOfSteps, check);
	// console.log(use)

	// Create the necessary variable lines
	const descriptions = {
		ROBOLIQ: "Path to Roboliq executable program",
		SCRIPTDIR: "Directory of this script and related files",
		RUNDIR: "Directory where run-time files should be saved (e.g. logfiles and measurement data)",
		RUN: "Identifier for the current run of this script"
	};
	const variableLines = {};
	while (useChanged) {
		useChanged = false;
		_.forEach(checkList, name => {
			if (use[name] && !variableLines[name]) {
				const line = getString(name, options.variables[name], descriptions[name]);
				variableLines[name] = line;
				check(line);
			}
		});
	}

	const lines = [];
	_.forEach(checkList, name => {
		if (variableLines[name]) {
			lines.push(variableLines[name]);
		}
	});

	return lines;
}
*/
