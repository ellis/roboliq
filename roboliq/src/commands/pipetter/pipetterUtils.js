/**
 * A module of helper functions for the pipetter commands.
 * @module commands/pipetter/pipetterUtils
 */

var _ = require('lodash');
var assert = require('assert');
var math = require('mathjs');
import expect from '../../expect.js';
var misc = require('../../misc.js');
var wellsParser = require('../../parsers/wellsParser.js');
import * as WellContents from '../../WellContents.js';

/**
 * Get fully qualified syringe object name
 * @param {string|integer} syringeName - name or number of syringe
 * @param {object} data - The data object passed to command handlers.
 * @return {string} fully qualified syringe object name, if found
 */
export function getSyringeName(syringeName, equipmentName, data) {
	const syringeName2 = `${equipmentName}.syringe.${syringeName}`;
	if (_.isInteger(syringeName)) {
		return syringeName2;
	}
	else if (_.has(data.objects, syringeName)) {
		return syringeName;
	}
	else if (_.has(data.objects, syringeName2)) {
		return syringeName2;
	}
	return syringeName;
}

/**
 * Get an object representing the effects of pipetting, aspirating, or dispensing.
 * @param {object} params The parameters for the pipetter._aspirate command.
 * @param {object} data The data object passed to command handlers.
 * @param {object} effects an optional effects object for effects which have taken place during the command handler and aren't in the data object
 * @return {object} The effects caused by the `_aspirate`, `_dispense` or `_pipette` command.
 */
export function getEffects_pipette(parsed, data, effects) {
	const effects2 = (effects) ? _.cloneDeep(effects) : {};
	const effectsNew = {};

	function addEffect(name, obj) {
		effects2[name] = obj;
		effectsNew[name] = obj;
	}

	//console.log("getEffects_aspirate:\n"+JSON.stringify(parsed, null, '\t'));
	parsed.value.items.forEach((item, index) => {
		// console.log("item: "+JSON.stringify(item, null, '\t'));

		// Get initial contents of the syringe
		const syringe = _.isString(item.syringe) ? _.get(data.objects, item.syringe) : item.syringe;
		const syringeName = _.isString(item.syringe) ? item.syringe : parsed.objectName[`items.${index}.syringe`];
		const syringeContentsName = `${syringeName}.contents`;
		const syringeContents00 = effects2[syringeContentsName] || syringe.contents || [];
		const syringeContaminantsName = `${syringeName}.contaminants`;

		const volume = item.volume;

		const source = item.source;
		if (!_.isUndefined(source)) {
			const syringeContents0 = syringeContents00;
			const syringeContaminants0 = effects2[syringeContaminantsName] || syringe.contaminants || [];
			//console.log({syringeName, syringeContents0});
			// Get initial contents of the source well
			const [srcContents00, srcContentsName] = WellContents.getContentsAndName(source, data, effects2);
			const srcContents0 = (_.isEmpty(srcContents00))
				? ["Infinity l", source] : srcContents00;
			//console.log("srcContents0", srcContents0, srcContentsName);

			// Contents of source well and syringe after aspiration
			const [srcContents1, syringeContents1] = WellContents.transferContents(srcContents0, syringeContents0, item.volume);
			//console.log({srcContents1, syringeContents1});
			// Update content effect for source
			addEffect(srcContentsName, srcContents1);

			// Get list of syringe contaminants
			const contaminants1 = _.keys(WellContents.flattenContents(syringeContents1));
			//console.log({syringeContaminantsName, syringeContaminants0, contaminants1});
			// Update contaminant effects
			if (!_.isEqual(syringeContaminants0, contaminants1))
				addEffect(syringeContaminantsName, contaminants1);
			// Update content effect
			addEffect(syringeContentsName, syringeContents1);
			// Remove cleaned property
			//console.log(`syringe ${syringeName}: `+JSON.stringify(item.syringe))
			if (!_.isUndefined(syringe.cleaned))
				addEffect(`${syringeName}.cleaned`, null);

			// Update __WELLS__ effects for source
			const volume1 = math.eval(srcContents1[0]);
			const nameWELL = "__WELLS__."+srcContentsName;
			//console.log("nameWELL:", nameWELL)
			const well0 = misc.findObjectsValue(nameWELL, data.objects, effects2) || {
				isSource: true,
				volumeMin: srcContents0[0],
				volumeMax: srcContents0[0]
			};
			const well1 = _.merge({}, well0, {
				volumeMax: math.max(math.eval(well0.volumeMax), volume1).format({precision: 14}),
				volumeMin: math.min(math.eval(well0.volumeMin), volume1).format({precision: 14}),
				volumeRemoved: (well0.volumeRemoved)
					? math.chain(math.eval(well0.volumeRemoved)).add(volume).done().format({precision: 14})
					: volume.format({precision: 14})
			});
			//console.log({well0, well1});
			//console.log("x:\n"+JSON.stringify(x, null, '  '));
			addEffect(nameWELL, well1);
		}

		const destination = item.destination;
		if (!_.isUndefined(destination)) {
			const syringeContents0 = effects2[syringeContentsName] || syringe.contents || [];
			const syringeContaminants0 = effects2[syringeContaminantsName] || syringe.contaminants || [];
			//console.log({syringeName, syringeContents0});
			// Get initial contents of the destination well
			const [dstContents0, dstContentsName] = WellContents.getContentsAndName(destination, data, effects2);
			//console.log("dst contents", dstContents0, dstContentsName);

			expect.truthy({paramName: `items[${index}].syringe`}, !WellContents.isEmpty(syringeContents0), "syringe contents should not be empty when dispensing");
			// Final contents of source well and syringe
			const [syringeContents1, dstContents1] = WellContents.transferContents(syringeContents0, dstContents0, item.volume);
			//console.log({syringeContents1, dstContents1})

			//console.log({srcContents1, syringeContents1});
			// Check for contact with the destination contents by looking for
			// the word "wet" in the program name.
			const isWetContact = !_.isEmpty(parsed.value.program) && /(_wet_|\bwet\b|_wet\b)/.test(parsed.value.program.toLowerCase());
			if (isWetContact) {
				// Contaminate the syringe with source contents
				// FIXME: Contaminate the syringe with destination contents if there is wet contact, i.e., use dstContents1 instead of srcContents0 for flattenContents()
				const contaminantsB = _.keys(WellContents.flattenContents(dstContents0));
				const contaminants1 = _.uniq(syringeContaminants0.concat(contaminantsB));
				if (!_.isEqual(syringeContaminants0, contaminants1))
					addEffect(syringeContaminantsName, contaminants1);
			}

			// Update content effect
			// If content volume = zero, set to null
			const syringeContents2 = (WellContents.isEmpty(syringeContents1))
				? null : syringeContents1;
			if (!_.isEqual(syringeContents0, syringeContents2))
				addEffect(syringeContentsName, syringeContents2);

			// Update content effect for destination
			addEffect(dstContentsName, dstContents1);

			// Update __WELLS__ effects for destination
			// REFACTOR: lots of duplication with the same code in the source condition
			const volume0 = WellContents.getVolume(dstContents0);
			const volume1 = WellContents.getVolume(dstContents1);
			const nameWELL = "__WELLS__."+dstContentsName;
			//console.log({nameWELL, volume0, volume1})
			//console.log("nameWELL:", nameWELL)
			const well0 = misc.findObjectsValue(nameWELL, data.objects, effects2) || {
				isSource: false,
				volumeMin: volume0.format({precision: 14}),
				volumeMax: volume0.format({precision: 14})
			};
			//console.log({well0})
			const well1 = _.merge(well0, {
				volumeMax: math.max(math.eval(well0.volumeMax), volume1).format({precision: 14}),
				volumeMin: math.min(math.eval(well0.volumeMin), volume1).format({precision: 14}),
				volumeAdded: (well0.volumeAdded)
					? math.chain(math.eval(well0.volumeAdded)).add(volume).done().format({precision: 14})
					: volume.format({precision: 14})
			});
			//console.log("x:\n"+JSON.stringify(x, null, '  '));
			addEffect(nameWELL, well1);
		}

		if (!_.isUndefined(item.well)) {
			console.log({item})
			const source = item.well;
			const syringeContents0 = syringeContents00;
			const syringeContaminants0 = effects2[syringeContaminantsName] || syringe.contaminants || [];
			//console.log({syringeName, syringeContents0});
			// Get initial contents of the source well
			const [srcContents00, srcContentsName] = WellContents.getContentsAndName(source, data, effects2);
			const srcContents0 = (_.isEmpty(srcContents00))
				? ["Infinity l", source] : srcContents00;
			//console.log("srcContents0", srcContents0, srcContentsName);

			// Contents of source well and syringe after aspiration
			const [srcContents1, syringeContents1] = WellContents.transferContents(srcContents0, syringeContents0, item.volume);
			//console.log({srcContents1, syringeContents1});

			// Get list of syringe contaminants
			const contaminants1 = _.keys(WellContents.flattenContents(syringeContents1));
			//console.log({syringeContaminantsName, syringeContaminants0, contaminants1});
			// Update contaminant effects
			if (!_.isEqual(syringeContaminants0, contaminants1))
				addEffect(syringeContaminantsName, contaminants1);
			// Remove cleaned property
			//console.log(`syringe ${syringeName}: `+JSON.stringify(item.syringe))
			if (!_.isUndefined(syringe.cleaned))
				addEffect(`${syringeName}.cleaned`, null);
		}

		// Prevent superfluous 'nulling' of syringe contents
		//console.log({syringeContentsName, syringeContents00, effects2: effects2[syringeContentsName], effectsNew: effectsNew[syringeContentsName]})
		if (effects2[syringeContentsName] === null && _.isEmpty(syringeContents00))
			delete effectsNew[syringeContentsName];
	});

	//console.log("effectsNew:\n"+JSON.stringify(effectsNew));

	return effectsNew;
}
