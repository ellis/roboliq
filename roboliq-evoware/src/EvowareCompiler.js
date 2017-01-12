/**
 * Module for compiling an instruction list (created by roboliq) to
 * an Evoware script.
 * @module
 */
import _ from 'lodash';
import naturalSort from 'javascript-natural-sort';
import path from 'path';
import M from './Medley.js';
import commandHelper from 'roboliq-processor/dist/commandHelper.js';
import * as EvowareTableFile from './EvowareTableFile.js';
import * as evowareHelper from './commands/evowareHelper.js';
import * as evoware from './commands/evoware.js';
import * as pipetter from './commands/pipetter.js';
import * as system from './commands/system.js';
import * as timer from './commands/timer.js';
import * as transporter from './commands/transporter.js';

const commandHandlers = {
	"evoware._execute": evoware._execute,
	"evoware._facts": evoware._facts,
	"evoware._raw": evoware._raw,
	"evoware._userPrompt": evoware._userPrompt,
	"evoware._variable": evoware._variable,
	"pipetter._aspirate": pipetter._aspirate,
	"pipetter._dispense": pipetter._dispense,
	"pipetter._measureVolume": pipetter._measureVolume,
	"pipetter._mix": pipetter._mix,
	"pipetter._pipette": pipetter._pipette,
	"pipetter._washTips": pipetter._washTips,
	"system.runtimeExitLoop": system.runtimeExitLoop,
	"system.runtimeLoadVariables": system.runtimeLoadVariables,
	"system.runtimeSteps": system.runtimeSteps,
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
 * @param  {object} options - an optional map of options; e.g. set timing=false to avoid outputting time-logging instructions
 * @return {array} an array of {table, lines} items; one item is generated per required table layout.  lines is an array of strings.
 */
export function compile(table, protocol, agents, options = {}) {
	// console.log(`compile:`)
	// console.log({options})
	options = _.defaults(options, _.get(protocol.config, "evowareCompiler", {}));
	table = _.cloneDeep(table);
	const objects = _.cloneDeep(protocol.objects);
	const evowareVariables = {}
	const results = compileStep(table, protocol, agents, [], objects, evowareVariables, [], options);

	// Prepend variables
	// console.log({evowareVariables})
	const variableList = _.reverse(_.sortBy(_.toPairs(evowareVariables), x => x[0]));
	// console.log({variableList})
	_.forEach(variableList, ([name, value]) => {
		// console.log({name, value, line: evowareHelper.createVariableLine(name, value)})
		results.unshift({line: evowareHelper.createVariableLine(name, value)});
	});
	// Prepend token to call 'initRun'
	results.unshift({line: evowareHelper.createExecuteLine(options.variables.ROBOLIQ, ["initRun", options.variables.SCRIPTFILE], true)});
	// Append token to reset the last moved ROMA
	results.push(transporter.moveLastRomaHome({objects}));

	const lines = _(results).flattenDeep().map(x => x.line).compact().value();
	// Prepend token to create the TEMPDIR
	if (_.some(lines, line => line.indexOf(options.variables.TEMPDIR) >= 0)) {
		lines.unshift(evowareHelper.createExecuteLine("cmd", ["/c", "mkdir", options.variables.TEMPDIR], true));
	}

	// Prepend token to open HTML
	if (_.get(options, "checkBench", true)) {
		lines.unshift(evowareHelper.createUserPromptLine("Please check the bench setup and then confirm this dialog when you're done"));
		lines.unshift(evowareHelper.createExecuteLine(options.variables.BROWSER, [path.dirname(options.variables.SCRIPTFILE)+"\\index.html"], false));
	}

	return [{table, lines, tokenTree: results}];
}

export function compileStep(table, protocol, agents, path, objects, evowareVariables, loopEndStack = [], options = {}) {
	// console.log(`compileStep: ${path.join(".")}`)
	// console.log({options})
	try {
		const results = compileStepSub(table, protocol, agents, path, objects, evowareVariables, loopEndStack, options);
		return results;
	} catch (e) {
		console.log("ERROR: "+path.join("."));
		console.log(JSON.stringify(_.get(protocol.steps, path)))
		console.log(e)
		console.log(e.stack)
	}
	return [];
}

function compileStepSub(table, protocol, agents, path, objects, evowareVariables, loopEndStack, options) {
	// console.log(`compileStepSub: ${path.join(".")}`)
	// console.log({options})
	if (_.isUndefined(objects)) {
		objects = _.cloneDeep(protocol.objects);
	}

	const stepId = path.join(".");
	//console.log({steps: protocol.steps})
	const step = (_.isEmpty(path)) ? protocol.steps : _.get(protocol.steps, path);
	// console.log({step})
	if (_.isUndefined(step))
		return undefined;

	if (stepId !== "" && protocol.COMPILER) {
		// Handle suspending
		if (protocol.COMPILER.suspendStepId) {
			const cmp = naturalSort(stepId, protocol.COMPILER.suspendStepId);
			// console.log({stepId, suspendStepId: protocol.COMPILER.suspendStepId, cmp})
			// If we've passed the suspend step, quit compiling
			if (cmp > 0) {
				return undefined;
			}
			// If we're before the suspend step, skip unless the current step is a parent step
			else if (cmp < 0 && !_.startsWith(protocol.COMPILER.suspendStepId, stepId+".")) {
				return undefined;
			}
		}
		// Handle resuming
		if (protocol.COMPILER.resumeStepId) {
			const cmp = naturalSort(stepId, protocol.COMPILER.resumeStepId);
			// console.log({stepId, resumeStepId: protocol.COMPILER.resumeStepId, cmp})
			// Skip until we've passed the resume step
			if (cmp <= 0) {
				return undefined;
			}
		}
	}

	const results = [];

	const commandHandler = commandHandlers[step.command];
	let generatedCommandLines = false;
	let generatedTimingLogs = false;
	const agentMatch = _.isUndefined(step.agent) || _.includes(agents, step.agent);
	// If there is no command handler for this step, then handle sub-steps
	if (_.isUndefined(commandHandler) || !agentMatch) {
		// Find all sub-steps (properties that start with a digit)
		var keys = _.filter(_.keys(step), function(key) {
			var c = key[0];
			return (c >= '0' && c <= '9');
		});
		// Sort them in "natural" order
		keys.sort(naturalSort);
		// console.log({keys})

		const isLoop = _.includes(["system.repeat", "experiment.forEachGroup", "experiment.forEachRow"], step.command);
		const loopEndName = `_${stepId}End`;
		const loopEndStack2 = (isLoop) ? [loopEndName].concat(loopEndStack) : loopEndStack;
		let needLoopLabel = false;

		// Try to expand the substeps
		for (const key of keys) {
			const result1 = compileStep(table, protocol, agents, path.concat(key), objects, evowareVariables, loopEndStack2, options);
			// Possibly check whether we need a loop label
			if (isLoop && !needLoopLabel) {
				const result2 = _.flattenDeep(result1);
				needLoopLabel = _.some(result2, result => (result.line || "").indexOf(loopEndName) >= 0);
			}
			if (!_.isUndefined(result1)) {
				results.push(result1);
			}
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
			loopEndStack,
			evowareVariables
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
		if (generatedCommandLines && _.get(options, "timing", false) === true) {
			const agent = _.get(objects, step.agent);
			const exePath = "%{ROBOLIQ}";//options.variables.ROBOLIQ;
			// console.log({agent})
			results.unshift({line: evowareHelper.createExecuteLine(exePath, ["begin", "--step", stepId, "--script", "%{SCRIPTFILE}"], false)});
			results.push({line: evowareHelper.createExecuteLine(exePath, ["end", "--step", stepId, "--script", "%{SCRIPTFILE}"], false)});
			generatedTimingLogs = true;
		}

	}

	// Process the protocol's effects
	const effects = _.get(protocol, ["effects", stepId]);
	if (!_.isUndefined(effects)) {
		_.forEach(effects, (effect, path2) => {
			M.setMut(objects, path2, effect);
		});
	}

	const generatedComment = !_.isEmpty(step.description);
	if (generatedComment) {
		const hasInstruction = _.find(_.flattenDeep(results), x => _.has(x, "line"));
		const text = `${stepId}) ${step.description}`;
		results.unshift({line: `Comment("${text}");`});
	}

	// Possibly wrap the instructions in a group
	const generatedGroup =
		(generatedTimingLogs) ||
		(!generatedCommandLines && generatedComment && results.length > 1);
	if (generatedGroup) {
		const text = `Step ${stepId}`;
		results.unshift({line: `Group("${text}");`});
		results.push({line: `GroupEnd();`});
	}

	// console.log({stepId, results})
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
				line = line.replace("%{ROBOLIQ}", options.variables.ROBOLIQ);
				line = line.replace("%{SCRIPTFILE}", options.variables.SCRIPTFILE);
				line = line.replace("%{SCRIPTDIR}", options.variables.SCRIPTDIR);
				line = line.replace("%{TEMPDIR}", options.variables.TEMPDIR);
				results[i].line = line;
			}
		}
	}
}
