import _ from 'lodash';
import naturalSort from 'javascript-natural-sort';
import * as EvowareTableFile from './EvowareTableFile.js';
import * as transporter from './commands/transporter.js';

const commandHandlers = {
	"evoware._facts": evoware._facts,
	/*"pipetter._aspirate" -> handlePipetterAspirate,
	"pipetter._dispense" -> handlePipetterDispense,
	"pipetter._pipette" -> handlePipetterPipette,
	"pipetter._cleanTips" -> handlePipetterCleanTips,
	"timer._start" -> handleTimerStart,
	"timer._wait" -> handleTimerWait,*/
	"transporter._movePlate": transporter._movePlate
}

/**
 * Compile a protocol for a given evoware setup
 * @param  {EvowareCarrierData} carrierData
 * @param  {object} table - table object (see EvowareTableFile.load)
 * @param  {roboliq:Protocol} protocol
 * @param  {array} agents - string array of agent names that this script should generate script(s) for
 * @return {array} an array of {table, lines} items; one item is generated per required table layout.  lines is an array of strings.
 */
export function compile(carrierData, table, protocol, agents) {
	table = _.cloneDeep(table);
	const objects = _.cloneDeep(protocol.objects);
	const results = compileStep(table, protocol, agents, [], objects);
	const flat = _.flattenDeep(results);
	const lines = _(flat).map(x => x.line).compact().value();
	return [{table, lines}];
}

export function compileStep(table, protocol, agents, path, objects) {
	//console.log(`compileStep: ${path.join(".")}`)
	//console.log({steps: protocol.steps})
	const step = (_.isEmpty(path)) ? protocol.steps : _.get(protocol.steps, path);
	if (_.isUndefined(step))
		return undefined;

	const results = [];

	//
	if (_.isUndefined(step.agent) || !_.includes(agents, step.agent)) {
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
			const result1 = compileStep(table, protocol, agents, path.concat(key), objects);
			results.push(result1);
		}
	}
	else {
		if (_.isUndefined(step.command)) {
			// ???
		}
		else {
			const commandHandler = commandHandlers[step.command];
			if (_.isUndefined(commandHandler)) {
				// Pass the command to the user
			}
			else {
				const result0 = commandHandler(step, objects, protocol, path);
				_.forEach(result0, result1 => {
					//console.log("result1: "+JSON.stringify(result1));
					results.push(result1);
					if (result1.effects) {
						_.forEach(result1.effects, (effect, path2) => {
							_.set(objects, path2, effect);
						});
					}
					//console.log({tableEffects: result1.tableEffects})
					if (!_.isEmpty(result1.tableEffects)) {
						_.forEach(result1.tableEffects, ([path2, value]) => {
							//console.log({path2, value})
							// TODO: Need to check whether a table change is required because a different labware model is now used at a given site
							_.set(table, path2, value);
						});
					}
				});
			}
		}
	}

	// Process the effects
	const effects = protocol.effects[path.join(".")];
	if (!_.isUndefined(effects)) {
		_.forEach(effects, (effect, path2) => {
			_.set(objects, path2, effect);
		});
	}

	return results;
}
