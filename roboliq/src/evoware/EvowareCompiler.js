import _ from 'lodash';
import naturalSort from 'javascript-natural-sort';
import * as EvowareTableFile from './EvowareTableFile.js';
import * as transporter from './commands/transporter.js';

const commandHandlers = {
	"transporter._movePlate": transporter._movePlate
}

export function compile(carrierData, table, protocol, agents) {
	table = _.cloneDeep(table);
	const objects = _.cloneDeep(protocol.objects);
	const results = compileStep(table, protocol, agents, [], objects);
	const flat = _.flattenDeep(results);
	const lines = _(flat).map(x => x.line).compact().value();
	return EvowareTableFile.toString(carrierData, table) + lines.join("\n");
}

export function compileStep(table, protocol, agents, path, objects) {
	console.log(`compileStep: ${path.join(".")}`)
	console.log({steps: protocol.steps})
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
		console.log({keys})
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
				const result1 = commandHandler(step, objects, protocol, path);
				console.log({result1});
				results.push(result1);
				if (results.effects) {
					_.forEach(result.effects, (effect, path2) => {
						_.set(objects, path2, effect);
					});
				}
				if (results.tableEffects) {
					_.forEach(results.tableEffects, ([path2, value]) => {
						// TODO: Need to check whether a table change is required because a different labware model is now used at a given site
						_.set(table, path2, value);
					});
				}
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
