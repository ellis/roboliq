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
		//console.log(JSON.stringify(item, null, '\t'));

		// Get initial contents of the syringe
		const syringeName = parsed.objectName[`items.${index}.syringe`];
		const syringeContentsName = `${syringeName}.contents`;

		const volume = item.volume;

		if (!_.isUndefined(item.source)) {
			const syringeContents0 = effects2[syringeContentsName] || item.syringe.contents || [];
			//console.log({syringeName, syringeContents0});
			// Get initial contents of the source well
			const [srcContents00, srcContentsName] = WellContents.getContentsAndName(item.source, data, effects2);
			const srcContents0 = (_.isEmpty(srcContents00))
				? ["Infinity l", item.source] : srcContents00;
			//console.log("srcContents0", srcContents0, srcContentsName);

			// Contents of source well and syringe after aspiration
			const [srcContents1, syringeContents1] = WellContents.transferContents(srcContents0, syringeContents0, item.volume);
			//console.log({srcContents1, syringeContents1});
			// Update content effect for source
			addEffect(srcContentsName, srcContents1);

			// Get list of syringe contaminants
			const contaminants0 = item.syringe.contaminants || [];
			const contaminants1 = _.keys(WellContents.flattenContents(syringeContents1));

			if (!_.isEqual(contaminants0, contaminants1))
				addEffect(`${syringeName}.contaminants`, contaminants1);
			// Update content effect
			addEffect(`${syringeName}.contents`, syringeContents1);
			// Remove cleaned property
			//console.log(`syringe ${syringeName}: `+JSON.stringify(item.syringe))
			if (!_.isUndefined(item.syringe.cleaned))
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

		if (!_.isUndefined(item.destination)) {
			const syringeContents0 = effects2[syringeContentsName] || item.syringe.contents || [];
			//console.log({syringeName, syringeContents0});
			// Get initial contents of the destination well
			const [dstContents0, dstContentsName] = WellContents.getContentsAndName(item.destination, data, effects2);
			//console.log("dst contents", dstContents0, dstContentsName);

			expect.truthy({paramName: `items[${index}].syringe`}, !WellContents.isEmpty(syringeContents0), "syringe contents should not be empty when dispensing");
			// Final contents of source well and syringe
			const [syringeContents1, dstContents1] = WellContents.transferContents(syringeContents0, dstContents0, item.volume);

			//console.log({srcContents1, syringeContents1});
			// Check for contact with the destination contents by looking for
			// the word "wet" in the program name.
			const isWetContact = /(_wet_|\bwet\b|_wet\b)/.test(parsed.value.program.toLowerCase());
			if (isWetContact) {
				// Contaminate the syringe with source contents
				// FIXME: Contaminate the syringe with destination contents if there is wet contact, i.e., use dstContents1 instead of srcContents0 for flattenContents()
				const contaminantsA = item.syringe.contaminants || [];
				const contaminantsB = _.keys(WellContents.flattenContents(dstContents0));
				const contaminants1 = _.uniq(contaminantsA.concat(contaminantsB));
				if (!_.isEqual(contaminantsA, contaminants1))
					addEffect(`${syringeName}.contaminants`, contaminants1);
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
			// REFACTOR: lots of duplication with the same code in the item.source condition
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
	});

	//console.log("effectsNew:\n"+JSON.stringify(effectsNew));

	return effectsNew;
}
/**
 * Get an object representing the effects of pipetting.
 * @param {object} params The parameters for the pipetter._pipette command.
 * @param {object} data The data object passed to command handlers.
 * @param {object} effects an optional effects object for effects which have taken place during the command handler and aren't in the data object
 * @return {object} The effects caused by the pipetting command.
 */
export function getEffects_pipette(parsed, data, effects) {
	const effects2 = (effects) ? _.cloneDeep(effects) : {};
	const effectsNew = {};

	function addEffect(name, obj) {
		effects2[name] = obj;
		effectsNew[name] = obj;
	}

	/*__WELLS__:
		plate1.contents.A01:
			isSource: true
			contentsInitial:
				water: 0ul
			volumeAdded: XXX
			volumeRemoved: 60ul
			contentsFinal:
				water: -60ul
				*/
	//console.log("getEffects_pipette:")
	//console.log(JSON.stringify(parsed, null, '\t'));
	parsed.value.items.forEach((item, index) => {
		//console.log("item "+index+": "+JSON.stringify(item));

		// Get initial contents of the source well
		let [srcContents0, srcContentsName] = WellContents.getContentsAndName(item.source, data, effects2);
		if (_.isEmpty(srcContents0))
			srcContents0 = ["Infinity l", item.source];
		//console.log("srcContents0", srcContents0, srcContentsName);

		// Get initial contents of the syringe
		const syringeContents0 = item.syringe.contents || [];
		// Get initial contents of the destination well
		const [dstContents0, dstContentsName] = WellContents.getContentsAndName(item.destination, data, effects2);
		//console.log("dst contents", dstContents0, dstContentsName);

		// Final contents of source and destination wells
		const [srcContents1, dstContents1] = WellContents.transferContents(srcContents0, dstContents0, item.volume);
		//console.log({srcContents1, dstContents1});

		// Contaminate the syringe with source contents
		// FIXME: Contaminate the syringe with destination contents if there is wet contact, i.e., use dstContents1 instead of srcContents0 for flattenContents()
		const contaminants0 = item.syringe.contaminants || [];
		const contaminants1 = _.keys(WellContents.flattenContents(srcContents0));
		const contaminants2 = _.uniq(contaminants0.concat(contaminants1));
		const syringeName = parsed.objectName[`items.${index}.syringe`];
		addEffect(`${syringeName}.contaminants`, contaminants2);
		// Remove contents property
		// FIXME: this isn't quite right -- should handle the case of multi-aspriating
		if (!_.isUndefined(item.syringe.contents))
			addEffect(`${syringeName}.contents`, null);
		// Remove cleaned property
		if (!_.isUndefined(item.syringe.cleaned))
			addEffect(`${syringeName}.cleaned`, null);

		// Update content effects
		addEffect(srcContentsName, srcContents1);
		addEffect(dstContentsName, dstContents1);
		//console.log()

		const volume = item.volume;

		// Update __WELLS__ effects for source
		if (true) {
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

		// Update __WELLS__ effects for destination
		if (true) {
			const volume1 = math.eval(dstContents1[0]);
			const nameWELL = "__WELLS__."+dstContentsName;
			//console.log("nameWELL:", nameWELL)
			const well0 = misc.findObjectsValue(nameWELL, data.objects, effects2) || {
				isSource: false,
				volumeMin: '0 l',
				volumeMax: '0 l'
			};
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
	});

	//console.log("effectsNew:\n"+JSON.stringify(effectsNew));

	return effectsNew;
}
